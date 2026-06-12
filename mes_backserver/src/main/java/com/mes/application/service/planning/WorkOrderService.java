package com.mes.application.service.planning;

import com.mes.domain.planning.workorder.dto.MaterialTransferRequest;
import com.mes.domain.planning.workorder.dto.MaterialTransferResponse;
import com.mes.domain.planning.workorder.dto.MaterialTransferStatusResponse;
import com.mes.domain.planning.workorder.dto.WorkOrderCreateRequest;
import com.mes.domain.planning.workorder.dto.WorkOrderDto;
import com.mes.domain.planning.workorder.dto.WorkOrderSearchDto;
import com.mes.global.common.dto.PageResponse;
import com.mes.global.exception.BusinessException;
import com.mes.global.exception.ErrorCode;
import com.mes.infra.persistence.mybatis.mapper.planning.WorkOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class WorkOrderService {

    private static final String SYSTEM_USER = "SYSTEM";
    private static final DateTimeFormatter WO_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final WorkOrderMapper workOrderMapper;
    private final McsTransferClient mcsTransferClient;

    public WorkOrderService(WorkOrderMapper workOrderMapper, McsTransferClient mcsTransferClient) {
        this.workOrderMapper = workOrderMapper;
        this.mcsTransferClient = mcsTransferClient;
    }

    public List<WorkOrderDto> getWorkOrders(String woNo, String plantCd, String itemCd, String woStatus,
                                            LocalDate woFromDt, LocalDate woToDt) {
        return workOrderMapper.selectWorkOrders(woNo, plantCd, itemCd, woStatus, woFromDt, woToDt);
    }

    public PageResponse<WorkOrderDto> getWorkOrderList(WorkOrderSearchDto searchDto) {
        List<WorkOrderDto> workOrders = workOrderMapper.selectWorkOrderList(searchDto);
        int totalCount = workOrderMapper.countWorkOrders(searchDto);
        return PageResponse.createPagedResponse(workOrders, totalCount, searchDto);
    }

    public WorkOrderDto getWorkOrder(Long woId) {
        WorkOrderDto workOrder = workOrderMapper.selectWorkOrderById(woId);
        if (workOrder == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "작업지시를 찾을 수 없습니다. woId=" + woId);
        }
        return workOrder;
    }

    @Transactional
    public void createWorkOrder(WorkOrderCreateRequest request) {
        int dailySeq = workOrderMapper.selectWorkOrderCountByDate(request.getWoDt()) + 1;
        String woNo = generateWoNo(request.getWoDt(), dailySeq);
        WorkOrderCreateRequest requestWithLot = withGeneratedLotNo(request, dailySeq);
        int inserted = workOrderMapper.insertWorkOrder(requestWithLot, woNo, SYSTEM_USER);
        if (inserted != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "작업지시 등록에 실패했습니다.");
        }
    }

    @Transactional
    public void updateWorkOrderStatus(Long woId, String newStatus) {
        WorkOrderDto workOrder = getWorkOrder(woId);
        validateStatusTransition(workOrder.getWoStatus(), newStatus);

        if ("취소".equals(newStatus)) {
            cancelActiveMaterialTransfers(workOrder);
            int cancelled = workOrderMapper.cancelWorkOrder(woId, SYSTEM_USER);
            if (cancelled != 1) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "작업오더 취소에 실패했습니다.");
            }
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime actualStartDtm = null;
        LocalDateTime actualEndDtm = null;

        if ("진행".equals(newStatus)) {
            validateMaterialTransfersCompleted(workOrder);
            actualStartDtm = now;
        } else if ("완료".equals(newStatus)) {
            actualEndDtm = now;
        }

        int updated = workOrderMapper.updateWorkOrderStatus(woId, newStatus, actualStartDtm, actualEndDtm, SYSTEM_USER);
        if (updated != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "작업지시 상태 변경에 실패했습니다.");
        }
    }

    @Transactional
    public MaterialTransferResponse requestMaterialTransfer(Long woId, MaterialTransferRequest request) {
        WorkOrderDto workOrder = getWorkOrder(woId);
        String itemCd = request.getItemCd() == null || request.getItemCd().isBlank() ? workOrder.getItemCd() : request.getItemCd();
        BigDecimal transferQty = request.getTransferQty() == null ? workOrder.getWoQty() : request.getTransferQty();
        validateNoActiveMaterialTransfer(workOrder);

        McsTransferClient.McsMaterialRequestResult result = mcsTransferClient.createMaterialRequest(new McsTransferClient.McsMaterialRequestPayload(
                "MES",
                workOrder.getWoId(),
                workOrder.getWoNo(),
                workOrder.getPlantCd(),
                itemCd,
                transferQty.doubleValue(),
                workOrder.getWorkcenterCd(),
                normalizeOptimizeRule(request.getOptimizeRule()),
                request.getRequestReason()
        ));

        return new MaterialTransferResponse(
                workOrder.getWoId(),
                workOrder.getWoNo(),
                result.getTransferId(),
                result.getTransferNo(),
                result.getFromLocationId(),
                result.getFromLocationCd(),
                result.getToLocationId(),
                result.getToLocationCd(),
                result.getItemCd(),
                result.getLotNo(),
                result.getTransferQty(),
                result.getOptimizeRule()
        );
    }

    public MaterialTransferStatusResponse getMaterialTransferStatus(Long woId) {
        WorkOrderDto workOrder = getWorkOrder(woId);
        return buildMaterialTransferStatus(workOrder);
    }

    private void validateNoActiveMaterialTransfer(WorkOrderDto workOrder) {
        mcsTransferClient.getTransfersByWorkOrder(workOrder.getWoId()).stream()
                .filter(transfer -> !"CANCELLED".equals(transfer.getTransferStatus()))
                .findFirst()
                .ifPresent(transfer -> {
                    throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                            "이미 생성된 MCS 자재 이동 요청이 있습니다. 이동번호="
                                    + transfer.getTransferNo()
                                    + ", 상태=" + transferStatusLabel(transfer.getTransferStatus(), transfer.getTransferStatusNm()));
                });
    }

    private void validateMaterialTransfersCompleted(WorkOrderDto workOrder) {
        List<McsTransferClient.McsTransferSummary> transfers = mcsTransferClient.getTransfersByWorkOrder(workOrder.getWoId());
        List<McsTransferClient.McsTransferSummary> activeTransfers = transfers.stream()
                .filter(transfer -> !"CANCELLED".equals(transfer.getTransferStatus()))
                .toList();
        if (activeTransfers.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "MCS 자재 이동 요청이 완료되지 않아 작업을 시작할 수 없습니다. 먼저 자재 요청을 생성하세요.");
        }

        activeTransfers.stream()
                .filter(transfer -> !"COMPLETED".equals(transfer.getTransferStatus()))
                .findFirst()
                .ifPresent(transfer -> {
                    throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                            "MCS 자재 이동이 아직 완료되지 않았습니다. 이동번호="
                                    + transfer.getTransferNo()
                                    + ", 상태=" + transferStatusLabel(transfer.getTransferStatus(), transfer.getTransferStatusNm()));
                });
    }

    private void cancelActiveMaterialTransfers(WorkOrderDto workOrder) {
        mcsTransferClient.cancelMaterialRequestsByWorkOrder(workOrder.getWoId());
    }

    private MaterialTransferStatusResponse buildMaterialTransferStatus(WorkOrderDto workOrder) {
        List<McsTransferClient.McsTransferSummary> transfers = mcsTransferClient.getTransfersByWorkOrder(workOrder.getWoId());
        McsTransferClient.McsTransferSummary activeTransfer = transfers.stream()
                .filter(transfer -> !"CANCELLED".equals(transfer.getTransferStatus()))
                .findFirst()
                .orElse(null);

        if (activeTransfer == null) {
            return new MaterialTransferStatusResponse(
                    workOrder.getWoId(),
                    false,
                    false,
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "MCS 자재 이동 요청 전"
            );
        }

        boolean completed = "COMPLETED".equals(activeTransfer.getTransferStatus());
        boolean failed = "FAILED".equals(activeTransfer.getTransferStatus());
        return new MaterialTransferStatusResponse(
                workOrder.getWoId(),
                true,
                completed,
                completed,
                activeTransfer.getTransferId(),
                activeTransfer.getTransferNo(),
                activeTransfer.getTransferStatus(),
                transferStatusLabel(activeTransfer.getTransferStatus(), activeTransfer.getTransferStatusNm()),
                activeTransfer.getFromLocationCd(),
                activeTransfer.getToLocationCd(),
                completed ? "MCS 자재 이동 완료" : (failed ? "MCS 자재 이동 실패 - MCS에서 취소 후 재요청하세요." : "MCS 자재 이동 대기 중")
        );
    }

    private String generateWoNo(LocalDate woDt, int dailySeq) {
        return "WO" + woDt.format(WO_DATE_FMT) + String.format("%04d", dailySeq);
    }

    private WorkOrderCreateRequest withGeneratedLotNo(WorkOrderCreateRequest request, int dailySeq) {
        if (request.getLotNo() != null && !request.getLotNo().isBlank()) {
            return request;
        }

        return new WorkOrderCreateRequest(
                request.getPlantCd(),
                request.getPlanId(),
                request.getWoDt(),
                request.getItemCd(),
                request.getWoQty(),
                request.getWorkcenterCd(),
                request.getEquipmentCd(),
                request.getWorkerId(),
                request.getPlanStartDtm(),
                request.getPlanEndDtm(),
                request.getPriority(),
                generateProductionLotNo(request, dailySeq),
                request.getOrderNo(),
                request.getDeliveryDt(),
                request.getWoRmk()
        );
    }

    private String generateProductionLotNo(WorkOrderCreateRequest request, int dailySeq) {
        String itemPart = request.getItemCd().replaceAll("[^A-Za-z0-9]", "");
        if (itemPart.length() > 16) {
            itemPart = itemPart.substring(0, 16);
        }
        return "LOT-" + itemPart + "-" + request.getWoDt().format(WO_DATE_FMT) + "-" + String.format("%04d", dailySeq);
    }

    private String normalizeOptimizeRule(String optimizeRule) {
        return optimizeRule == null || optimizeRule.isBlank() ? "AVOID_CONGESTION" : optimizeRule;
    }

    private String transferStatusLabel(String status, String statusName) {
        if (statusName != null && !statusName.isBlank()) {
            return statusName + "(" + status + ")";
        }
        return switch (status) {
            case "REQUESTED" -> "요청";
            case "IN_PROGRESS" -> "이동중";
            case "COMPLETED" -> "완료";
            case "CANCELLED" -> "취소";
            case "FAILED" -> "실패";
            default -> status;
        };
    }

    private void validateStatusTransition(String currentStatus, String newStatus) {
        Set<String> allowed = switch (currentStatus) {
            case "대기" -> Set.of("진행", "취소");
            case "진행" -> Set.of("완료", "취소");
            default -> Set.of();
        };

        if (!allowed.contains(newStatus)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "'" + currentStatus + "' 상태에서 '" + newStatus + "' 상태로 변경할 수 없습니다.");
        }
    }
}
