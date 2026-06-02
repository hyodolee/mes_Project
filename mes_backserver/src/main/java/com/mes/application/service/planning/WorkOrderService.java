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

    public List<WorkOrderDto> getWorkOrders(String plantCd, String itemCd, String woStatus,
                                            LocalDate woFromDt, LocalDate woToDt) {
        return workOrderMapper.selectWorkOrders(plantCd, itemCd, woStatus, woFromDt, woToDt);
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
        int dailySeq = workOrderMapper.selectWorkOrderCountByDate(request.woDt()) + 1;
        String woNo = generateWoNo(request.woDt(), dailySeq);
        WorkOrderCreateRequest requestWithLot = withGeneratedLotNo(request, dailySeq);
        int inserted = workOrderMapper.insertWorkOrder(requestWithLot, woNo, SYSTEM_USER);
        if (inserted != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "작업지시 등록에 실패했습니다.");
        }
    }

    @Transactional
    public void updateWorkOrderStatus(Long woId, String newStatus) {
        WorkOrderDto workOrder = getWorkOrder(woId);
        validateStatusTransition(workOrder.woStatus(), newStatus);

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
        String itemCd = request.itemCd() == null || request.itemCd().isBlank() ? workOrder.itemCd() : request.itemCd();
        BigDecimal transferQty = request.transferQty() == null ? workOrder.woQty() : request.transferQty();
        validateNoActiveMaterialTransfer(workOrder);

        McsTransferClient.McsMaterialRequestResult result = mcsTransferClient.createMaterialRequest(new McsTransferClient.McsMaterialRequestPayload(
                "MES",
                workOrder.woId(),
                workOrder.woNo(),
                workOrder.plantCd(),
                itemCd,
                transferQty.doubleValue(),
                workOrder.workcenterCd(),
                normalizeOptimizeRule(request.optimizeRule()),
                request.requestReason()
        ));

        return new MaterialTransferResponse(
                workOrder.woId(),
                workOrder.woNo(),
                result.transferId(),
                result.transferNo(),
                result.fromLocationId(),
                result.fromLocationCd(),
                result.toLocationId(),
                result.toLocationCd(),
                result.itemCd(),
                result.lotNo(),
                result.transferQty(),
                result.optimizeRule()
        );
    }

    public MaterialTransferStatusResponse getMaterialTransferStatus(Long woId) {
        WorkOrderDto workOrder = getWorkOrder(woId);
        return buildMaterialTransferStatus(workOrder);
    }

    private void validateNoActiveMaterialTransfer(WorkOrderDto workOrder) {
        mcsTransferClient.getTransfersByWorkOrder(workOrder.woId()).stream()
                .filter(transfer -> !"CANCELLED".equals(transfer.transferStatus()))
                .findFirst()
                .ifPresent(transfer -> {
                    throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                            "이미 생성된 MCS 자재 이동 요청이 있습니다. 이동번호="
                                    + transfer.transferNo()
                                    + ", 상태=" + transferStatusLabel(transfer.transferStatus(), transfer.transferStatusNm()));
                });
    }

    private void validateMaterialTransfersCompleted(WorkOrderDto workOrder) {
        List<McsTransferClient.McsTransferSummary> transfers = mcsTransferClient.getTransfersByWorkOrder(workOrder.woId());
        List<McsTransferClient.McsTransferSummary> activeTransfers = transfers.stream()
                .filter(transfer -> !"CANCELLED".equals(transfer.transferStatus()))
                .toList();
        if (activeTransfers.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "MCS 자재 이동 요청이 완료되지 않아 작업을 시작할 수 없습니다. 먼저 자재 요청을 생성하세요.");
        }

        activeTransfers.stream()
                .filter(transfer -> !"COMPLETED".equals(transfer.transferStatus()))
                .findFirst()
                .ifPresent(transfer -> {
                    throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                            "MCS 자재 이동이 아직 완료되지 않았습니다. 이동번호="
                                    + transfer.transferNo()
                                    + ", 상태=" + transferStatusLabel(transfer.transferStatus(), transfer.transferStatusNm()));
                });
    }

    private void cancelActiveMaterialTransfers(WorkOrderDto workOrder) {
        mcsTransferClient.cancelMaterialRequestsByWorkOrder(workOrder.woId());
    }

    private MaterialTransferStatusResponse buildMaterialTransferStatus(WorkOrderDto workOrder) {
        List<McsTransferClient.McsTransferSummary> transfers = mcsTransferClient.getTransfersByWorkOrder(workOrder.woId());
        McsTransferClient.McsTransferSummary activeTransfer = transfers.stream()
                .filter(transfer -> !"CANCELLED".equals(transfer.transferStatus()))
                .findFirst()
                .orElse(null);

        if (activeTransfer == null) {
            return new MaterialTransferStatusResponse(
                    workOrder.woId(),
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

        boolean completed = "COMPLETED".equals(activeTransfer.transferStatus());
        boolean failed = "FAILED".equals(activeTransfer.transferStatus());
        return new MaterialTransferStatusResponse(
                workOrder.woId(),
                true,
                completed,
                completed,
                activeTransfer.transferId(),
                activeTransfer.transferNo(),
                activeTransfer.transferStatus(),
                transferStatusLabel(activeTransfer.transferStatus(), activeTransfer.transferStatusNm()),
                activeTransfer.fromLocationCd(),
                activeTransfer.toLocationCd(),
                completed ? "MCS 자재 이동 완료" : (failed ? "MCS 자재 이동 실패 - MCS에서 취소 후 재요청하세요." : "MCS 자재 이동 대기 중")
        );
    }

    private String generateWoNo(LocalDate woDt, int dailySeq) {
        return "WO" + woDt.format(WO_DATE_FMT) + String.format("%04d", dailySeq);
    }

    private WorkOrderCreateRequest withGeneratedLotNo(WorkOrderCreateRequest request, int dailySeq) {
        if (request.lotNo() != null && !request.lotNo().isBlank()) {
            return request;
        }

        return new WorkOrderCreateRequest(
                request.plantCd(),
                request.planId(),
                request.woDt(),
                request.itemCd(),
                request.woQty(),
                request.workcenterCd(),
                request.equipmentCd(),
                request.workerId(),
                request.planStartDtm(),
                request.planEndDtm(),
                request.priority(),
                generateProductionLotNo(request, dailySeq),
                request.orderNo(),
                request.deliveryDt(),
                request.woRmk()
        );
    }

    private String generateProductionLotNo(WorkOrderCreateRequest request, int dailySeq) {
        String itemPart = request.itemCd().replaceAll("[^A-Za-z0-9]", "");
        if (itemPart.length() > 16) {
            itemPart = itemPart.substring(0, 16);
        }
        return "LOT-" + itemPart + "-" + request.woDt().format(WO_DATE_FMT) + "-" + String.format("%04d", dailySeq);
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
