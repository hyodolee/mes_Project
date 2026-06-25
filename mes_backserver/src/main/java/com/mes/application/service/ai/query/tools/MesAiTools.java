package com.mes.application.service.ai.query.tools;

import com.mes.application.service.master.ItemService;
import com.mes.application.service.master.PlantService;
import com.mes.application.service.planning.ProdPlanService;
import com.mes.application.service.planning.WorkOrderService;
import org.springframework.ai.tool.annotation.Tool;

import java.util.List;

import static com.mes.application.service.ai.query.tools.OperationToolSupport.num;
import static com.mes.application.service.ai.query.tools.OperationToolSupport.safe;
import static com.mes.application.service.ai.query.tools.OperationToolSupport.str;

public class MesAiTools {

    private static final int MAX_ROWS = 30;

    private final WorkOrderService workOrderService;
    private final ProdPlanService prodPlanService;
    private final PlantService plantService;
    private final ItemService itemService;
    private final List<String> dataPoints;

    public MesAiTools(
            WorkOrderService workOrderService,
            ProdPlanService prodPlanService,
            PlantService plantService,
            ItemService itemService,
            List<String> dataPoints
    ) {
        this.workOrderService = workOrderService;
        this.prodPlanService = prodPlanService;
        this.plantService = plantService;
        this.itemService = itemService;
        this.dataPoints = dataPoints;
    }

    @Tool(description = "작업지시(생산 오더) 목록을 조회한다. 각 작업의 상태(대기/진행/완료), 품목명, 지시 수량, 담당 설비를 포함한다.")
    public List<OperationToolViews.WorkOrderView> getWorkOrders() {
        var orders = workOrderService.getWorkOrders(null, null, null, null, null, null);
        long pending = orders.stream().filter(o -> "대기".equals(o.getWoStatus())).count();
        long inProgress = orders.stream().filter(o -> "진행".equals(o.getWoStatus())).count();
        dataPoints.add("작업지시 " + orders.size() + "건 조회 (대기 " + pending + ", 진행 " + inProgress + ")");
        return orders.stream().limit(MAX_ROWS)
                .map(o -> new OperationToolViews.WorkOrderView(safe(o.getWoNo()), safe(o.getItemCd()), safe(o.getItemNm()),
                        num(o.getWoQty()), num(o.getGoodQty()), num(o.getDefectQty()), safe(o.getWoStatus()),
                        o.getPriority(), safe(o.getWorkcenterCd()), safe(o.getEquipmentCd()), safe(o.getLotNo()),
                        str(o.getPlanStartDtm()), str(o.getPlanEndDtm())))
                .toList();
    }

    @Tool(description = "생산 계획 목록을 조회한다. 각 계획의 상태, 품목명, 계획 수량, 실적 수량, 계획 시작/종료일을 포함한다.")
    public List<OperationToolViews.ProdPlanView> getProdPlans() {
        var plans = prodPlanService.getProdPlans(null, null, null, null, null);
        dataPoints.add("생산 계획 " + plans.size() + "건 조회");
        return plans.stream().limit(MAX_ROWS)
                .map(p -> new OperationToolViews.ProdPlanView(safe(p.getPlanNo()), safe(p.getItemNm()), num(p.getPlanQty()),
                        num(p.getResultQty()), safe(p.getPlanStatus()), p.getPriority(),
                        str(p.getPlanStartDt()), str(p.getPlanEndDt()), str(p.getDeliveryDt())))
                .toList();
    }

    @Tool(description = "등록된 공장(사업장) 목록을 조회한다. 공장 코드, 공장명, 소속 회사명, 사용 여부를 포함한다. "
            + "'공장이 몇 개 등록되어 있어?' 같은 기준정보 질문에 사용한다.")
    public List<OperationToolViews.PlantView> getPlants() {
        var plants = plantService.getPlants(null, null, null);
        dataPoints.add("공장 " + plants.size() + "개 조회");
        return plants.stream()
                .map(p -> new OperationToolViews.PlantView(safe(p.getPlantCd()), safe(p.getPlantNm()), safe(p.getCompanyNm()), safe(p.getUseYn())))
                .toList();
    }

    @Tool(description = "등록된 품목(자재/제품) 목록을 조회한다. 품목 코드, 품목명, 품목 유형, 단위를 포함한다.")
    public List<OperationToolViews.ItemView> getItems() {
        var items = itemService.getItems(null, null);
        dataPoints.add("품목 " + items.size() + "개 조회");
        return items.stream().limit(MAX_ROWS)
                .map(i -> new OperationToolViews.ItemView(safe(i.getItemCd()), safe(i.getItemNm()), safe(i.getItemSpec()),
                        safe(i.getItemType()), safe(i.getItemGrp()), safe(i.getUnit()), safe(i.getUseYn())))
                .toList();
    }
}
