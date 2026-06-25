package com.mes.application.service.ai.query.tools;

import com.mes.application.service.planning.McsTransferClient;
import com.mes.mcs.domain.inventory.dto.LocStockSearchDto;
import com.mes.mcs.domain.location.dto.LocationSearchDto;
import com.mes.mcs.domain.plc.dto.PlcEventSearchDto;
import com.mes.mcs.domain.route.dto.RouteSearchDto;
import com.mes.mcs.infra.persistence.mybatis.mapper.inventory.InventoryMapper;
import com.mes.mcs.infra.persistence.mybatis.mapper.location.LocationMapper;
import com.mes.mcs.infra.persistence.mybatis.mapper.plc.PlcEventMapper;
import com.mes.mcs.infra.persistence.mybatis.mapper.route.RouteMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;

import static com.mes.application.service.ai.query.tools.OperationToolSupport.hasAny;
import static com.mes.application.service.ai.query.tools.OperationToolSupport.safe;
import static com.mes.application.service.ai.query.tools.OperationToolSupport.str;
import static com.mes.application.service.ai.query.tools.OperationToolSupport.toPlcView;

public class McsAiTools {

    private final McsTransferClient mcsTransferClient;
    private final RouteMapper routeMapper;
    private final LocationMapper locationMapper;
    private final InventoryMapper inventoryMapper;
    private final PlcEventMapper plcEventMapper;
    private final List<String> dataPoints;

    public McsAiTools(
            McsTransferClient mcsTransferClient,
            RouteMapper routeMapper,
            LocationMapper locationMapper,
            InventoryMapper inventoryMapper,
            PlcEventMapper plcEventMapper,
            List<String> dataPoints
    ) {
        this.mcsTransferClient = mcsTransferClient;
        this.routeMapper = routeMapper;
        this.locationMapper = locationMapper;
        this.inventoryMapper = inventoryMapper;
        this.plcEventMapper = plcEventMapper;
        this.dataPoints = dataPoints;
    }

    @Tool(description = "자재 이동(반송) 목록을 조회한다. 각 이동의 상태(이동중/완료/실패), 출발 위치, 도착 위치를 포함한다. "
            + "특정 이동의 실패 원인을 알려면 transferId를 getTransferEvents 도구에 넘겨 PLC 이벤트를 추가 조회해야 한다.")
    public List<OperationToolViews.TransferView> getTransfers() {
        var transfers = mcsTransferClient.getAllTransfers(50);
        long failed = transfers.stream().filter(t -> "FAILED".equals(t.getTransferStatus())).count();
        dataPoints.add("자재 이동 " + transfers.size() + "건 조회 (실패 " + failed + "건)");
        return transfers.stream()
                .map(t -> new OperationToolViews.TransferView(t.getTransferId(), safe(t.getTransferNo()), safe(t.getTransferStatus()),
                        safe(t.getTransferStatusNm()), safe(t.getFromLocationCd()), safe(t.getToLocationCd())))
                .toList();
    }

    @Tool(description = "특정 자재 이동의 PLC 설비 이벤트를 조회한다. 이동이 실패하거나 멈춘 원인(오류 코드, 오류 메시지, 설비명)을 알 수 있다. "
            + "transferId는 getTransfers 결과에서 얻는다.")
    public List<OperationToolViews.PlcEventView> getTransferEvents(
            @ToolParam(description = "조회할 자재 이동의 ID (getTransfers 결과의 transferId)") Long transferId
    ) {
        var events = mcsTransferClient.getPlcEventsByTransfer(transferId, 10);
        dataPoints.add("이동 ID " + transferId + "의 PLC 이벤트 " + events.size() + "건 조회");
        return events.stream().map(OperationToolSupport::toPlcView).toList();
    }

    @Tool(description = "최근 PLC 설비 이벤트 전체를 조회한다. 설비 오류, 인터락(안전 정지), 데이터 누락(검증 실패) 등 "
            + "특정 이동과 무관한 설비 신호 문제를 파악할 때 사용한다.")
    public List<OperationToolViews.PlcEventView> getRecentPlcEvents() {
        var events = mcsTransferClient.getRecentPlcEvents(20);
        long errors = events.stream().filter(e -> "ERROR".equals(e.getEventStatus())).count();
        dataPoints.add("최근 PLC 이벤트 " + events.size() + "건 조회 (오류 " + errors + "건)");
        return events.stream().map(OperationToolSupport::toPlcView).toList();
    }

    @Tool(description = "MCS 경로 구간을 조회한다. 막힌 경로, 혼잡 경로, 인터록 경로, 출발/도착 노드 확인 질문에 사용한다.")
    public List<OperationToolViews.RouteEdgeView> getRouteEdges() {
        RouteSearchDto searchDto = new RouteSearchDto();
        var edges = routeMapper.selectRouteEdges(searchDto);
        dataPoints.add("MCS 경로 구간 " + edges.size() + "건 조회");
        return edges.stream()
                .map(e -> new OperationToolViews.RouteEdgeView(e.getRouteEdgeId(), safe(e.getEdgeCd()), safe(e.getEdgeNm()),
                        safe(e.getFromNodeCd()), safe(e.getToNodeCd()), safe(e.getEdgeStatus()), e.getTravelTimeSec()))
                .toList();
    }

    @Tool(description = "현재 사용할 수 없거나 주의가 필요한 MCS 경로 구간을 조회한다. BLOCKED, CONGESTED, INTERLOCKED, MAINTENANCE 상태를 중심으로 본다.")
    public List<OperationToolViews.RouteEdgeView> getBlockedRoutes() {
        var edges = getRouteEdges().stream()
                .filter(e -> hasAny(e.getEdgeStatus(), "BLOCKED", "CONGESTED", "INTERLOCKED", "MAINTENANCE"))
                .toList();
        dataPoints.add("주의 경로 구간 " + edges.size() + "건 필터링");
        return edges;
    }

    @Tool(description = "특정 자재 이동의 MCS 이동 경로 요약을 조회한다. transferId는 getTransfers 결과에서 얻는다.")
    public OperationToolViews.TransferRouteView getTransferRoute(
            @ToolParam(description = "조회할 자재 이동 ID") Long transferId
    ) {
        var route = routeMapper.selectTransferRoute(transferId).orElse(null);
        if (route == null) {
            dataPoints.add("이동 ID " + transferId + "의 경로 없음");
            return null;
        }
        dataPoints.add("이동 ID " + transferId + "의 경로 1건 조회");
        return new OperationToolViews.TransferRouteView(route.getTransferRouteId(), route.getTransferId(), safe(route.getRouteStatus()),
                route.getTotalDistanceM(), route.getTotalTimeSec(), safe(route.getOptimizeRule()), route.getReplanCount());
    }

    @Tool(description = "특정 자재 이동의 MCS 경로 단계 목록을 조회한다. 어느 구간에서 막히는지 확인할 때 사용한다.")
    public List<OperationToolViews.RouteStepView> getTransferRouteSteps(
            @ToolParam(description = "조회할 자재 이동 ID") Long transferId
    ) {
        var route = routeMapper.selectTransferRoute(transferId).orElse(null);
        if (route == null) {
            dataPoints.add("이동 ID " + transferId + "의 경로 단계 없음");
            return List.of();
        }
        var steps = routeMapper.selectTransferRouteSteps(route.getTransferRouteId());
        dataPoints.add("이동 ID " + transferId + "의 경로 단계 " + steps.size() + "건 조회");
        return steps.stream()
                .map(s -> new OperationToolViews.RouteStepView(s.getStepSeq(), safe(s.getEdgeCd()), safe(s.getFromNodeCd()),
                        safe(s.getToNodeCd()), safe(s.getStepStatus()), safe(s.getEdgeStatus()), s.getExpectedTimeSec()))
                .toList();
    }

    @Tool(description = "MCS 로케이션 목록을 조회한다. 특정 위치, 사용 가능 위치, 로케이션 상태 질문에 사용한다.")
    public List<OperationToolViews.LocationView> getLocations() {
        LocationSearchDto searchDto = new LocationSearchDto();
        searchDto.setPage(1);
        searchDto.setSize(100);
        var locations = locationMapper.selectLocationList(searchDto);
        dataPoints.add("MCS 로케이션 " + locations.size() + "건 조회");
        return locations.stream()
                .map(l -> new OperationToolViews.LocationView(l.getLocationId(), safe(l.getLocationCd()), safe(l.getLocationNm()),
                        safe(l.getWarehouseNm()), safe(l.getZoneNm()), safe(l.getLocationStatus()),
                        l.getCurrentUsage(), l.getMaxCapacity()))
                .toList();
    }

    @Tool(description = "MCS 로케이션 재고를 조회한다. 로케이션별 품목/LOT/가용수량 질문에 사용한다.")
    public List<OperationToolViews.LocationStockView> getLocationStocks() {
        LocStockSearchDto searchDto = new LocStockSearchDto();
        searchDto.setPage(1);
        searchDto.setSize(100);
        searchDto.setExcludeZeroStock(true);
        var stocks = inventoryMapper.selectLocStockList(searchDto);
        dataPoints.add("MCS 로케이션 재고 " + stocks.size() + "건 조회");
        return stocks.stream()
                .map(s -> new OperationToolViews.LocationStockView(s.getLocStockId(), safe(s.getLocationCd()), safe(s.getItemCd()),
                        safe(s.getItemNm()), safe(s.getLotNo()), s.getStockQty(), s.getAvailableQty()))
                .toList();
    }

    @Tool(description = "PLC 이벤트 중 검증 실패 또는 처리 실패 이벤트를 조회한다. payload 필드 누락, toLocationCd 누락, 인터록 문제 확인에 사용한다.")
    public List<OperationToolViews.PlcEventView> getPlcValidationFailures() {
        PlcEventSearchDto searchDto = new PlcEventSearchDto();
        searchDto.setPage(1);
        searchDto.setSize(50);
        var events = plcEventMapper.selectPlcEventList(searchDto).stream()
                .filter(e -> hasAny(e.getProcessResult(), "VALIDATION_FAILED", "FAILED")
                        || hasAny(e.getEventStatus(), "ERROR", "FAILED"))
                .map(e -> new OperationToolViews.PlcEventView(safe(e.getEquipmentCd()), safe(e.getEventType()), safe(e.getEventStatus()),
                        safe(e.getLocationCd()), safe(e.getErrorCode()), safe(e.getEventMessage()),
                        safe(e.getProcessResult()), safe(e.getProcessMessage()), str(e.getEventDtm())))
                .toList();
        dataPoints.add("PLC 검증/처리 실패 " + events.size() + "건 조회");
        return events;
    }

    @Tool(description = "자재 이동 실패 원인을 종합 조회한다. 이동 목록, PLC 실패 이벤트, 막힌 경로를 함께 보고 짧은 원인 후보를 만든다.")
    public OperationToolViews.TransferBlockerSummary analyzeTransferBlockers() {
        var transfers = getTransfers();
        var failedTransfers = transfers.stream().filter(t -> "FAILED".equals(t.getStatus())).toList();
        var plcFailures = getPlcValidationFailures();
        var blockedRoutes = getBlockedRoutes();
        dataPoints.add("자재 이동 장애 요약 생성");
        return new OperationToolViews.TransferBlockerSummary(failedTransfers.size(), plcFailures.size(), blockedRoutes.size());
    }
}
