package com.mes.application.service.ai.query;

import com.mes.application.service.equipment.EquipmentService;
import com.mes.application.service.inventory.InventoryService;
import com.mes.application.service.master.ItemService;
import com.mes.application.service.master.PlantService;
import com.mes.application.service.planning.McsTransferClient;
import com.mes.application.service.planning.ProdPlanService;
import com.mes.application.service.planning.WorkOrderService;
import com.mes.application.service.production.DefectHistoryService;
import com.mes.application.service.quality.InspectResultService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 운영 질의에서 사용할 읽기 전용 조회 도구 묶음을 생성한다.
 *
 * <p>{@link OperationQueryService}가 MES/MCS 개별 서비스를 모두 직접 들고 있지 않도록
 * 조회 도구 생성 책임을 분리했다.</p>
 */
@Component
public class OperationToolsFactory {

    private final McsTransferClient mcsTransferClient;
    private final WorkOrderService workOrderService;
    private final PlantService plantService;
    private final ItemService itemService;
    private final InventoryService inventoryService;
    private final EquipmentService equipmentService;
    private final ProdPlanService prodPlanService;
    private final DefectHistoryService defectHistoryService;
    private final InspectResultService inspectResultService;
    private final OperationDocumentSearchService documentSearchService;

    public OperationToolsFactory(
            McsTransferClient mcsTransferClient,
            WorkOrderService workOrderService,
            PlantService plantService,
            ItemService itemService,
            InventoryService inventoryService,
            EquipmentService equipmentService,
            ProdPlanService prodPlanService,
            DefectHistoryService defectHistoryService,
            InspectResultService inspectResultService,
            OperationDocumentSearchService documentSearchService
    ) {
        this.mcsTransferClient = mcsTransferClient;
        this.workOrderService = workOrderService;
        this.plantService = plantService;
        this.itemService = itemService;
        this.inventoryService = inventoryService;
        this.equipmentService = equipmentService;
        this.prodPlanService = prodPlanService;
        this.defectHistoryService = defectHistoryService;
        this.inspectResultService = inspectResultService;
        this.documentSearchService = documentSearchService;
    }

    public OperationTools create(List<String> dataPoints) {
        // dataPoints는 이번 질문에서 실제 조회한 내역을 담으므로 요청마다 새 도구를 만든다.
        return new OperationTools(
                mcsTransferClient,
                workOrderService,
                plantService,
                itemService,
                inventoryService,
                equipmentService,
                prodPlanService,
                defectHistoryService,
                inspectResultService,
                documentSearchService,
                dataPoints
        );
    }
}
