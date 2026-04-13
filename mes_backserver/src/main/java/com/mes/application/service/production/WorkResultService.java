package com.mes.application.service.production;

import com.mes.domain.production.workresult.dto.WorkResultCreateRequest;
import com.mes.domain.production.workresult.dto.WorkResultDto;
import com.mes.domain.production.workresult.dto.WorkResultSearchDto;
import com.mes.global.common.dto.PageResponse;
import com.mes.global.exception.BusinessException;
import com.mes.global.exception.ErrorCode;
import com.mes.infra.persistence.mybatis.mapper.production.WorkResultMapper;
import com.mes.infra.persistence.mybatis.mapper.planning.WorkOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class WorkResultService {

    private static final String SYSTEM_USER = "SYSTEM";
    private static final DateTimeFormatter RESULT_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final WorkResultMapper workResultMapper;
    private final WorkOrderMapper workOrderMapper;
    private final com.mes.application.service.inventory.InventoryService inventoryService;
    private final com.mes.application.service.equipment.EquipmentService equipmentService;

    public WorkResultService(WorkResultMapper workResultMapper, 
                             WorkOrderMapper workOrderMapper,
                             com.mes.application.service.inventory.InventoryService inventoryService,
                             com.mes.application.service.equipment.EquipmentService equipmentService) {
        this.workResultMapper = workResultMapper;
        this.workOrderMapper = workOrderMapper;
        this.inventoryService = inventoryService;
        this.equipmentService = equipmentService;
    }

    public List<WorkResultDto> getWorkResults(String plantCd, String itemCd, LocalDate fromDt, LocalDate toDt) {
        return workResultMapper.selectWorkResults(plantCd, itemCd, fromDt, toDt);
    }

    public PageResponse<WorkResultDto> getWorkResultPage(WorkResultSearchDto searchDto) {
        int total = workResultMapper.countWorkResults(searchDto);
        List<WorkResultDto> content = workResultMapper.selectWorkResultList(searchDto);
        return PageResponse.createPagedResponse(content, total, searchDto);
    }

    @Transactional
    public void createWorkResult(WorkResultCreateRequest request) {
        // 1. Generate Result No
        String resultNo = generateResultNo(request.getResultDt());

        // 2. Insert Work Result
        int inserted = workResultMapper.insertWorkResult(request, resultNo, SYSTEM_USER);
        if (inserted != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "작업실적 등록에 실패했습니다.");
        }

        // 3. Update Work Order Qty
        int updated = workOrderMapper.updateWorkOrderQty(request.getWoId(), request.getGoodQty(), 
            request.getDefectQty() == null ? java.math.BigDecimal.ZERO : request.getDefectQty(), SYSTEM_USER);
        if (updated != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "작업지시 수량 업데이트에 실패했습니다.");
        }

        // 4. Inventory Receipt (Production Receipt)
        if (request.getGoodQty().compareTo(java.math.BigDecimal.ZERO) > 0) {
            com.mes.domain.inventory.trans.dto.TransRequest transRequest = com.mes.domain.inventory.trans.dto.TransRequest.builder()
                .plantCd(request.getPlantCd())
                .transType("입고")
                .transReason("생산입고")
                .itemCd(request.getItemCd())
                .lotNo(request.getLotNo())
                .transQty(request.getGoodQty())
                .unit("EA")
                .toWarehouseCd("WH_PROD")
                .refType("작업실적")
                .refNo(resultNo)
                .transUserId(request.getWorkerId())
                .regUserId(SYSTEM_USER)
                .build();
            
            inventoryService.processTransaction(transRequest);
        }

        // 5. Equipment Operation Status Record
        if (request.getEquipmentCd() != null && !request.getEquipmentCd().isEmpty()) {
            com.mes.domain.equipment.oper.dto.OperStatusRequest operRequest = com.mes.domain.equipment.oper.dto.OperStatusRequest.builder()
                .plantCd(request.getPlantCd())
                .equipmentCd(request.getEquipmentCd())
                .operDt(request.getResultDt())
                .shift(request.getShift())
                .operStatus("가동")
                .startDtm(request.getStartDtm())
                .endDtm(request.getEndDtm())
                .operTime(request.getWorkTime())
                .woId(request.getWoId())
                .itemCd(request.getItemCd())
                .prodQty(request.getProdQty())
                .workerId(request.getWorkerId())
                .regUserId(SYSTEM_USER)
                .build();
            
            equipmentService.recordOperStatus(operRequest);
        }
    }

    private String generateResultNo(LocalDate resultDt) {
        int count = workResultMapper.selectWorkResultCountByDate(resultDt);
        return "WR" + resultDt.format(RESULT_DATE_FMT) + String.format("%04d", count + 1);
    }
}
