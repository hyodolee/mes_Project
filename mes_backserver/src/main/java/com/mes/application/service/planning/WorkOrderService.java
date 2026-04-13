package com.mes.application.service.planning;

import com.mes.domain.planning.workorder.dto.WorkOrderCreateRequest;
import com.mes.domain.planning.workorder.dto.WorkOrderDto;
import com.mes.domain.planning.workorder.dto.WorkOrderSearchDto;
import com.mes.global.common.dto.PageResponse;
import com.mes.global.exception.BusinessException;
import com.mes.global.exception.ErrorCode;
import com.mes.infra.persistence.mybatis.mapper.planning.WorkOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public WorkOrderService(WorkOrderMapper workOrderMapper) {
        this.workOrderMapper = workOrderMapper;
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
        String woNo = generateWoNo(request.woDt());
        int inserted = workOrderMapper.insertWorkOrder(request, woNo, SYSTEM_USER);
        if (inserted != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "작업지시 등록에 실패했습니다.");
        }
    }

    @Transactional
    public void updateWorkOrderStatus(Long woId, String newStatus) {
        WorkOrderDto workOrder = getWorkOrder(woId);
        validateStatusTransition(workOrder.woStatus(), newStatus);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime actualStartDtm = null;
        LocalDateTime actualEndDtm = null;

        if ("진행".equals(newStatus)) {
            actualStartDtm = now;
        } else if ("완료".equals(newStatus) || "취소".equals(newStatus)) {
            actualEndDtm = now;
        }

        int updated = workOrderMapper.updateWorkOrderStatus(woId, newStatus, actualStartDtm, actualEndDtm, SYSTEM_USER);
        if (updated != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "작업지시 상태 변경에 실패했습니다.");
        }
    }

    private String generateWoNo(LocalDate woDt) {
        int count = workOrderMapper.selectWorkOrderCountByDate(woDt);
        return "WO" + woDt.format(WO_DATE_FMT) + String.format("%04d", count + 1);
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
