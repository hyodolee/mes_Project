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
import com.mes.domain.inventory.stock.dto.StockSearchDto;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.math.BigDecimal;
import java.util.List;

/**
 * 운영 챗봇 답변에 필요한 조회 기능 모음.
 *
 * <p>질문마다 필요한 데이터가 다르므로, 모델이 사용할 수 있는 조회 메서드를 여기에 모아 둔다.
 * 백엔드에서 질문 유형을 직접 분류하지 않는다.</p>
 *
 * <p><b>설계 원칙: 조회(READ) 전용 도구만 등록한다.</b>
 * 수정/삭제 기능을 등록하면 모델 응답 과정에서 데이터가 변경될 수 있으므로 금지한다.</p>
 *
 * <p>요청마다 새로 생성되며, 호출한 도구 내역을 {@code dataPoints}에 기록해
 * 프론트엔드의 "근거 데이터" 표시에 활용한다.</p>
 */
public class OperationTools {

    private static final int MAX_ROWS = 50;

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
    private final List<String> dataPoints;

    public OperationTools(
            McsTransferClient mcsTransferClient,
            WorkOrderService workOrderService,
            PlantService plantService,
            ItemService itemService,
            InventoryService inventoryService,
            EquipmentService equipmentService,
            ProdPlanService prodPlanService,
            DefectHistoryService defectHistoryService,
            InspectResultService inspectResultService,
            OperationDocumentSearchService documentSearchService,
            List<String> dataPoints
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
        this.dataPoints = dataPoints;
    }

    // === MCS: 자재 이동 / PLC 이벤트 =========================================

    @Tool(description = "자재 이동(반송) 목록을 조회한다. 각 이동의 상태(이동중/완료/실패), 출발 위치, 도착 위치를 포함한다. "
            + "특정 이동의 실패 원인을 알려면 transferId를 getTransferEvents 도구에 넘겨 PLC 이벤트를 추가 조회해야 한다.")
    public List<TransferView> getTransfers() {
        var transfers = mcsTransferClient.getAllTransfers(50);
        long failed = transfers.stream().filter(t -> "FAILED".equals(t.getTransferStatus())).count();
        dataPoints.add("자재 이동 " + transfers.size() + "건 조회 (실패 " + failed + "건)");
        return transfers.stream()
                .map(t -> new TransferView(t.getTransferId(), safe(t.getTransferNo()), safe(t.getTransferStatus()),
                        safe(t.getTransferStatusNm()), safe(t.getFromLocationCd()), safe(t.getToLocationCd())))
                .toList();
    }

    @Tool(description = "특정 자재 이동의 PLC 설비 이벤트를 조회한다. 이동이 실패하거나 멈춘 원인(오류 코드, 오류 메시지, 설비명)을 알 수 있다. "
            + "transferId는 getTransfers 결과에서 얻는다.")
    public List<PlcEventView> getTransferEvents(
            @ToolParam(description = "조회할 자재 이동의 ID (getTransfers 결과의 transferId)") Long transferId
    ) {
        var events = mcsTransferClient.getPlcEventsByTransfer(transferId, 10);
        dataPoints.add("이동 ID " + transferId + "의 PLC 이벤트 " + events.size() + "건 조회");
        return events.stream().map(this::toPlcView).toList();
    }

    @Tool(description = "최근 PLC 설비 이벤트 전체를 조회한다. 설비 오류, 인터락(안전 정지), 데이터 누락(검증 실패) 등 "
            + "특정 이동과 무관한 설비 신호 문제를 파악할 때 사용한다.")
    public List<PlcEventView> getRecentPlcEvents() {
        var events = mcsTransferClient.getRecentPlcEvents(20);
        long errors = events.stream().filter(e -> "ERROR".equals(e.getEventStatus())).count();
        dataPoints.add("최근 PLC 이벤트 " + events.size() + "건 조회 (오류 " + errors + "건)");
        return events.stream().map(this::toPlcView).toList();
    }

    // === MES: 작업지시 / 생산계획 ============================================

    @Tool(description = "작업지시(생산 오더) 목록을 조회한다. 각 작업의 상태(대기/진행/완료), 품목명, 지시 수량, 담당 설비를 포함한다.")
    public List<WorkOrderView> getWorkOrders() {
        var orders = workOrderService.getWorkOrders(null, null, null, null, null, null);
        long pending = orders.stream().filter(o -> "대기".equals(o.getWoStatus())).count();
        long inProgress = orders.stream().filter(o -> "진행".equals(o.getWoStatus())).count();
        dataPoints.add("작업지시 " + orders.size() + "건 조회 (대기 " + pending + ", 진행 " + inProgress + ")");
        return orders.stream().limit(MAX_ROWS)
                .map(o -> new WorkOrderView(safe(o.getWoNo()), safe(o.getItemNm()), num(o.getWoQty()),
                        safe(o.getWoStatus()), safe(o.getEquipmentCd())))
                .toList();
    }

    @Tool(description = "생산 계획 목록을 조회한다. 각 계획의 상태, 품목명, 계획 수량, 실적 수량, 계획 시작/종료일을 포함한다.")
    public List<ProdPlanView> getProdPlans() {
        var plans = prodPlanService.getProdPlans(null, null, null, null, null);
        dataPoints.add("생산 계획 " + plans.size() + "건 조회");
        return plans.stream().limit(MAX_ROWS)
                .map(p -> new ProdPlanView(safe(p.getPlanNo()), safe(p.getItemNm()), num(p.getPlanQty()),
                        num(p.getResultQty()), safe(p.getPlanStatus()), str(p.getPlanStartDt()), str(p.getPlanEndDt())))
                .toList();
    }

    // === 기준정보: 공장 / 품목 ===============================================

    @Tool(description = "등록된 공장(사업장) 목록을 조회한다. 공장 코드, 공장명, 소속 회사명, 사용 여부를 포함한다. "
            + "'공장이 몇 개 등록되어 있어?' 같은 기준정보 질문에 사용한다.")
    public List<PlantView> getPlants() {
        var plants = plantService.getPlants(null, null, null);
        dataPoints.add("공장 " + plants.size() + "개 조회");
        return plants.stream()
                .map(p -> new PlantView(safe(p.getPlantCd()), safe(p.getPlantNm()), safe(p.getCompanyNm()), safe(p.getUseYn())))
                .toList();
    }

    @Tool(description = "등록된 품목(자재/제품) 목록을 조회한다. 품목 코드, 품목명, 품목 유형, 단위를 포함한다.")
    public List<ItemView> getItems() {
        var items = itemService.getItems(null, null);
        dataPoints.add("품목 " + items.size() + "개 조회");
        return items.stream().limit(MAX_ROWS)
                .map(i -> new ItemView(safe(i.getItemCd()), safe(i.getItemNm()), safe(i.getItemType()), safe(i.getUnit())))
                .toList();
    }

    // === 재고 ===============================================================

    @Tool(description = "재고 현황을 조회한다. 품목명, 위치, 로트번호, 재고 수량, 가용 수량, 재고 상태를 포함한다. "
            + "'OO 품목 재고 얼마야?', '재고 부족한 거 있어?' 같은 질문에 사용한다.")
    public List<StockView> getStocks() {
        StockSearchDto search = new StockSearchDto();
        search.setSize(100);
        var stocks = inventoryService.getStocks(search);
        dataPoints.add("재고 " + stocks.size() + "건 조회");
        return stocks.stream().limit(MAX_ROWS)
                .map(s -> new StockView(safe(s.getItemNm()), safe(s.getLocationCd()), safe(s.getLotNo()),
                        num(s.getStockQty()), num(s.getAvailableQty()), safe(s.getUnit()), safe(s.getStockStatus())))
                .toList();
    }

    // === 설비 ===============================================================

    @Tool(description = "설비 가동 현황을 조회한다. 설비별 가동 상태, 가동일, 가동 시간, 생산 수량을 포함한다. "
            + "'설비 가동률', '어떤 설비가 돌고 있어?' 같은 질문에 사용한다.")
    public List<EquipStatusView> getEquipmentStatus() {
        var list = equipmentService.getOperStatuses(null, null, null, null);
        dataPoints.add("설비 가동 현황 " + list.size() + "건 조회");
        return list.stream().limit(MAX_ROWS)
                .map(o -> new EquipStatusView(safe(o.getEquipmentCd()), str(o.getOperDt()), safe(o.getOperStatus()),
                        num(o.getOperTime()), num(o.getProdQty())))
                .toList();
    }

    @Tool(description = "설비 비가동(고장/정지) 이력을 조회한다. 설비별 비가동 유형, 사유, 비가동 시간을 포함한다. "
            + "'고장난 설비 있어?', '설비가 왜 멈췄어?' 같은 질문에 사용한다.")
    public List<DowntimeView> getEquipmentDowntimes() {
        var list = equipmentService.getDowntimes(null, null, null, null);
        dataPoints.add("설비 비가동 이력 " + list.size() + "건 조회");
        return list.stream().limit(MAX_ROWS)
                .map(d -> new DowntimeView(safe(d.getEquipmentCd()), str(d.getDowntimeDt()), safe(d.getDowntimeType()),
                        safe(d.getDowntimeReason()), num(d.getDowntimeMin())))
                .toList();
    }

    // === 품질 / 불량 ========================================================

    @Tool(description = "품질 검사 결과를 조회한다. 검사 유형, 품목, 검사 수량, 합격/불합격 수량, 판정 결과를 포함한다. "
            + "'품질 검사 결과', '불합격된 거 있어?' 같은 질문에 사용한다.")
    public List<InspectView> getInspectResults() {
        var list = inspectResultService.getInspectResults(null, null, null, null);
        dataPoints.add("품질 검사 " + list.size() + "건 조회");
        return list.stream().limit(MAX_ROWS)
                .map(r -> new InspectView(safe(r.getInspectType()), safe(r.getItemCd()), num(r.getInspectQty()),
                        num(r.getPassQty()), num(r.getFailQty()), safe(r.getJudgeResult()), str(r.getInspectDt())))
                .toList();
    }

    @Tool(description = "불량 이력을 조회한다. 불량 유형, 불량명, 불량 수량, 불량 원인, 발생 설비/품목을 포함한다. "
            + "'불량 많은 품목', '불량 원인이 뭐야?' 같은 질문에 사용한다.")
    public List<DefectView> getDefects() {
        var list = defectHistoryService.getDefectHistories(null, null, null, null);
        dataPoints.add("불량 이력 " + list.size() + "건 조회");
        return list.stream().limit(MAX_ROWS)
                .map(d -> new DefectView(str(d.getDefectDt()), safe(d.getItemCd()), safe(d.getDefectNm()),
                        num(d.getDefectQty()), safe(d.getDefectCause()), safe(d.getEquipmentCd())))
                .toList();
    }

    // === 운영 문서 ==========================================================

    @Tool(description = "PLC-MCS 통신 정의서, 필수 필드, PLC 태그 매핑표, 오류 조치 SOP 문서를 검색한다. "
            + "PLC 이벤트 필드 누락, 태그 매핑, 통신 규격, 오류 원인 설명에 문서 근거가 필요할 때 사용한다.")
    public List<OperationDocumentSearchService.DocumentSnippet> searchOperationDocuments(
            @ToolParam(description = "문서에서 찾을 키워드나 질문. 예: TRANSFER_STARTED toLocationCd 누락, LOT 태그, 인터락 오류")
            String query
    ) {
        var results = documentSearchService.search(query);
        dataPoints.add("운영 문서 근거 " + results.size() + "건 조회");
        return results;
    }

    // === 헬퍼 ===============================================================

    private PlcEventView toPlcView(McsTransferClient.McsPlcEventSummary e) {
        return new PlcEventView(safe(e.getEquipmentCd()), safe(e.getEventType()), safe(e.getEventStatus()),
                safe(e.getLocationCd()), safe(e.getErrorCode()), safe(e.getEventMessage()),
                safe(e.getProcessResult()), safe(e.getProcessMessage()), safe(e.getEventDtm()));
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private Double num(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private String str(Object value) {
        return value == null ? "-" : value.toString();
    }

    // === AI에게 넘기는 경량 뷰 (대용량/불필요 필드 제외) ======================

    @lombok.Getter @lombok.AllArgsConstructor
    public static class TransferView {
        private Long transferId;
        private String transferNo;
        private String status;
        private String statusNm;
        private String fromLocation;
        private String toLocation;
    }

    @lombok.Getter @lombok.AllArgsConstructor
    public static class PlcEventView {
        private String equipmentCd;
        private String eventType;
        private String eventStatus;
        private String locationCd;
        private String errorCode;
        private String eventMessage;
        private String processResult;
        private String processMessage;
        private String eventDtm;
    }

    @lombok.Getter @lombok.AllArgsConstructor
    public static class WorkOrderView {
        private String woNo;
        private String itemNm;
        private Double woQty;
        private String status;
        private String equipmentCd;
    }

    @lombok.Getter @lombok.AllArgsConstructor
    public static class ProdPlanView {
        private String planNo;
        private String itemNm;
        private Double planQty;
        private Double resultQty;
        private String status;
        private String startDt;
        private String endDt;
    }

    @lombok.Getter @lombok.AllArgsConstructor
    public static class PlantView {
        private String plantCd;
        private String plantNm;
        private String companyNm;
        private String useYn;
    }

    @lombok.Getter @lombok.AllArgsConstructor
    public static class ItemView {
        private String itemCd;
        private String itemNm;
        private String itemType;
        private String unit;
    }

    @lombok.Getter @lombok.AllArgsConstructor
    public static class StockView {
        private String itemNm;
        private String locationCd;
        private String lotNo;
        private Double stockQty;
        private Double availableQty;
        private String unit;
        private String stockStatus;
    }

    @lombok.Getter @lombok.AllArgsConstructor
    public static class EquipStatusView {
        private String equipmentCd;
        private String operDt;
        private String operStatus;
        private Double operTime;
        private Double prodQty;
    }

    @lombok.Getter @lombok.AllArgsConstructor
    public static class DowntimeView {
        private String equipmentCd;
        private String downtimeDt;
        private String downtimeType;
        private String downtimeReason;
        private Double downtimeMin;
    }

    @lombok.Getter @lombok.AllArgsConstructor
    public static class InspectView {
        private String inspectType;
        private String itemCd;
        private Double inspectQty;
        private Double passQty;
        private Double failQty;
        private String judgeResult;
        private String inspectDt;
    }

    @lombok.Getter @lombok.AllArgsConstructor
    public static class DefectView {
        private String defectDt;
        private String itemCd;
        private String defectNm;
        private Double defectQty;
        private String defectCause;
        private String equipmentCd;
    }
}
