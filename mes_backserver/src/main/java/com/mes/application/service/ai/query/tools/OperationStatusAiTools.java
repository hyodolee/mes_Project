package com.mes.application.service.ai.query.tools;

import com.mes.application.service.equipment.EquipmentService;
import com.mes.application.service.inventory.InventoryService;
import com.mes.application.service.production.DefectHistoryService;
import com.mes.application.service.quality.InspectResultService;
import com.mes.domain.inventory.stock.dto.StockSearchDto;
import org.springframework.ai.tool.annotation.Tool;

import java.util.List;

import static com.mes.application.service.ai.query.tools.OperationToolSupport.num;
import static com.mes.application.service.ai.query.tools.OperationToolSupport.safe;
import static com.mes.application.service.ai.query.tools.OperationToolSupport.str;

public class OperationStatusAiTools {

    private static final int MAX_ROWS = 30;

    private final InventoryService inventoryService;
    private final EquipmentService equipmentService;
    private final InspectResultService inspectResultService;
    private final DefectHistoryService defectHistoryService;
    private final List<String> dataPoints;

    public OperationStatusAiTools(
            InventoryService inventoryService,
            EquipmentService equipmentService,
            InspectResultService inspectResultService,
            DefectHistoryService defectHistoryService,
            List<String> dataPoints
    ) {
        this.inventoryService = inventoryService;
        this.equipmentService = equipmentService;
        this.inspectResultService = inspectResultService;
        this.defectHistoryService = defectHistoryService;
        this.dataPoints = dataPoints;
    }

    @Tool(description = "재고 현황을 조회한다. 품목, 위치, 로트, 재고/가용 수량, 안전재고(기준치)와 기준치 이하 여부(belowSafety), 재고 상태를 포함한다. "
            + "'OO 품목 재고 얼마야?', '재고 부족한 거 있어?', '기준치(안전재고) 이하 품목 있어?' 같은 질문에 사용한다. "
            + "'기준치 이하'는 belowSafety=true 또는 가용수량 < 안전재고로 판단한다.")
    public List<OperationToolViews.StockView> getStocks() {
        StockSearchDto search = new StockSearchDto();
        search.setSize(100);
        var stocks = inventoryService.getStocks(search);
        dataPoints.add("재고 " + stocks.size() + "건 조회");
        return stocks.stream().limit(MAX_ROWS)
                .map(s -> {
                    Double available = num(s.getAvailableQty());
                    Double safety = num(s.getSafetyStockQty());
                    Boolean below = (available != null && safety != null) ? available < safety : null;
                    return new OperationToolViews.StockView(safe(s.getItemCd()), safe(s.getItemNm()), safe(s.getLocationCd()), safe(s.getLotNo()),
                            num(s.getStockQty()), available, num(s.getReservedQty()), safety, below,
                            safe(s.getUnit()), safe(s.getStockStatus()), str(s.getExpireDt()));
                })
                .toList();
    }

    @Tool(description = "설비 가동 현황을 조회한다. 설비별 가동 상태, 가동일, 가동 시간, 생산 수량을 포함한다. "
            + "'설비 가동률', '어떤 설비가 돌고 있어?' 같은 질문에 사용한다.")
    public List<OperationToolViews.EquipStatusView> getEquipmentStatus() {
        var list = equipmentService.getOperStatuses(null, null, null, null);
        dataPoints.add("설비 가동 현황 " + list.size() + "건 조회");
        return list.stream().limit(MAX_ROWS)
                .map(o -> new OperationToolViews.EquipStatusView(safe(o.getEquipmentCd()), str(o.getOperDt()), safe(o.getShift()),
                        safe(o.getOperStatus()), num(o.getOperTime()), num(o.getProdQty()),
                        safe(o.getItemCd()), safe(o.getWorkerId())))
                .toList();
    }

    @Tool(description = "설비 비가동(고장/정지) 이력을 조회한다. 설비별 비가동 유형, 사유, 비가동 시간을 포함한다. "
            + "'고장난 설비 있어?', '설비가 왜 멈췄어?' 같은 질문에 사용한다.")
    public List<OperationToolViews.DowntimeView> getEquipmentDowntimes() {
        var list = equipmentService.getDowntimes(null, null, null, null);
        dataPoints.add("설비 비가동 이력 " + list.size() + "건 조회");
        return list.stream().limit(MAX_ROWS)
                .map(d -> new OperationToolViews.DowntimeView(safe(d.getEquipmentCd()), str(d.getDowntimeDt()), safe(d.getDowntimeType()),
                        safe(d.getDowntimeCd()), safe(d.getDowntimeReason()), num(d.getDowntimeMin()), safe(d.getActionContent())))
                .toList();
    }

    @Tool(description = "품질 검사 결과를 조회한다. 검사 유형, 품목, 검사 수량, 합격/불합격 수량, 판정 결과를 포함한다. "
            + "'품질 검사 결과', '불합격된 거 있어?' 같은 질문에 사용한다.")
    public List<OperationToolViews.InspectView> getInspectResults() {
        var list = inspectResultService.getInspectResults(null, null, null, null, null);
        dataPoints.add("품질 검사 " + list.size() + "건 조회");
        return list.stream().limit(MAX_ROWS)
                .map(r -> new OperationToolViews.InspectView(safe(r.getInspectType()), safe(r.getItemCd()), safe(r.getLotNo()),
                        num(r.getInspectQty()), num(r.getSampleQty()), num(r.getPassQty()), num(r.getFailQty()),
                        safe(r.getJudgeResult()), str(r.getInspectDt())))
                .toList();
    }

    @Tool(description = "불량 이력을 조회한다. 불량 유형, 불량명, 불량 수량, 불량 원인, 발생 설비/품목을 포함한다. "
            + "'불량 많은 품목', '불량 원인이 뭐야?' 같은 질문에 사용한다.")
    public List<OperationToolViews.DefectView> getDefects() {
        var list = defectHistoryService.getDefectHistories(null, null, null, null);
        dataPoints.add("불량 이력 " + list.size() + "건 조회");
        return list.stream().limit(MAX_ROWS)
                .map(d -> new OperationToolViews.DefectView(str(d.getDefectDt()), safe(d.getItemCd()), safe(d.getDefectType()), safe(d.getDefectNm()),
                        num(d.getDefectQty()), safe(d.getDefectCause()), safe(d.getDefectAction()),
                        safe(d.getDisposition()), safe(d.getEquipmentCd())))
                .toList();
    }
}
