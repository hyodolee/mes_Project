package com.mes.application.service.ai.analysis;

import com.mes.application.service.ai.support.AiTextSupport;
import com.mes.application.service.equipment.EquipmentService;
import com.mes.application.service.inventory.InventoryService;
import com.mes.application.service.planning.McsTransferClient;
import com.mes.application.service.planning.WorkOrderService;
import com.mes.application.service.production.DefectHistoryService;
import com.mes.application.service.quality.InspectResultService;
import com.mes.domain.ai.dto.CriticalEventDto;
import com.mes.domain.ai.dto.GlobalOperationEvidenceDto;
import com.mes.domain.ai.dto.McsTransferSummaryDto;
import com.mes.domain.ai.dto.OperationDomainSummaryDto;
import com.mes.domain.ai.dto.WorkOrderSummaryDto;
import com.mes.domain.inventory.stock.dto.StockDto;
import com.mes.domain.inventory.stock.dto.StockSearchDto;
import com.mes.domain.planning.workorder.dto.WorkOrderDto;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 전체 운영 브리핑에 필요한 MES/MCS 현황 데이터를 수집한다.
 */
@Service
public class OperationEvidenceCollector {

    private static final DateTimeFormatter SPACE_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final WorkOrderService workOrderService;
    private final McsTransferClient mcsTransferClient;
    private final InventoryService inventoryService;
    private final InspectResultService inspectResultService;
    private final DefectHistoryService defectHistoryService;
    private final EquipmentService equipmentService;

    public OperationEvidenceCollector(
            WorkOrderService workOrderService,
            McsTransferClient mcsTransferClient,
            InventoryService inventoryService,
            InspectResultService inspectResultService,
            DefectHistoryService defectHistoryService,
            EquipmentService equipmentService
    ) {
        this.workOrderService = workOrderService;
        this.mcsTransferClient = mcsTransferClient;
        this.inventoryService = inventoryService;
        this.inspectResultService = inspectResultService;
        this.defectHistoryService = defectHistoryService;
        this.equipmentService = equipmentService;
    }

    /**
     * AI 분석과 fallback 분석이 공통으로 사용할 운영 스냅샷을 만든다.
     */
    public GlobalOperationEvidenceDto collect() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);

        WorkOrderSummaryDto woSummary = collectWorkOrders(today, now);
        TransferSnapshot transferSnapshot = collectTransfers();
        McsTransferSummaryDto transferSummary = summarizeTransfers(transferSnapshot.transfers(), today);
        List<CriticalEventDto> criticalEvents = transferSnapshot.events().stream()
                .filter(e -> isRecent(e.getEventDtm(), oneHourAgo, now))
                .filter(this::isCriticalEvent)
                .map(e -> new CriticalEventDto(
                        e.getEventType(),
                        e.getLocationCd(),
                        eventMessage(e),
                        e.getEventDtm()
                ))
                .toList();

        return new GlobalOperationEvidenceDto(woSummary, transferSummary, criticalEvents, collectDomains());
    }

    private WorkOrderSummaryDto collectWorkOrders(LocalDate today, LocalDateTime now) {
        // 날짜 제한 없이 전체 작업지시 조회. 더미 데이터까지 포함해 대시보드 브리핑 기준으로 사용한다.
        List<WorkOrderDto> allOrders = workOrderService.getWorkOrders(null, null, null, null, null, null);
        long total = allOrders.size();
        long inProgress = allOrders.stream().filter(o -> "진행".equals(o.getWoStatus())).count();
        long delayed = allOrders.stream()
                .filter(o -> "대기".equals(o.getWoStatus()))
                .filter(o -> o.getPlanStartDtm() != null && o.getPlanStartDtm().isBefore(now))
                .count();
        long pending = allOrders.stream().filter(o -> "대기".equals(o.getWoStatus())).count();
        long completedToday = allOrders.stream()
                .filter(o -> "완료".equals(o.getWoStatus()))
                .filter(o -> o.getActualEndDtm() != null && today.equals(o.getActualEndDtm().toLocalDate()))
                .count();

        return new WorkOrderSummaryDto(total, inProgress, delayed, pending, completedToday);
    }

    /**
     * MCS 이동 목록과 최근 PLC 이벤트는 서로 독립적이라 동시에 조회한다.
     */
    private TransferSnapshot collectTransfers() {
        CompletableFuture<List<McsTransferClient.McsTransferSummary>> transfersFuture =
                CompletableFuture.supplyAsync(() -> mcsTransferClient.getAllTransfers(100));
        CompletableFuture<List<McsTransferClient.McsPlcEventSummary>> eventsFuture =
                CompletableFuture.supplyAsync(() -> mcsTransferClient.getRecentPlcEvents(10));

        List<McsTransferClient.McsTransferSummary> transfers;
        List<McsTransferClient.McsPlcEventSummary> events;
        try {
            transfers = transfersFuture.orTimeout(5, TimeUnit.SECONDS).join();
        } catch (Exception e) {
            transfers = List.of();
        }
        try {
            events = eventsFuture.orTimeout(5, TimeUnit.SECONDS).join();
        } catch (Exception e) {
            events = List.of();
        }
        return new TransferSnapshot(transfers, events);
    }

    private McsTransferSummaryDto summarizeTransfers(
            List<McsTransferClient.McsTransferSummary> transfers,
            LocalDate today
    ) {
        long activeTransfers = transfers.stream()
                .filter(t -> "REQUESTED".equals(t.getTransferStatus()) || "IN_PROGRESS".equals(t.getTransferStatus()))
                .count();
        long failedTransfers = transfers.stream()
                .filter(t -> "FAILED".equals(t.getTransferStatus()))
                .count();
        long completedTransfersToday = transfers.stream()
                .filter(t -> "COMPLETED".equals(t.getTransferStatus()))
                .filter(t -> isToday(t.getUpdDtm(), today))
                .count();

        return new McsTransferSummaryDto(activeTransfers, failedTransfers, completedTransfersToday);
    }

    /**
     * 품질, 재고, 설비 요약은 한 영역이 실패해도 전체 분석이 멈추지 않게 각각 분리해서 수집한다.
     */
    private OperationDomainSummaryDto collectDomains() {
        OperationDomainSummaryDto domain = new OperationDomainSummaryDto();
        try {
            StockSearchDto search = new StockSearchDto();
            search.setSize(1000);
            List<StockDto> stocks = inventoryService.getStocks(search);
            domain.setStockLow(stocks.stream().filter(s -> "부족".equals(s.getStockStatus())).count());
            domain.setStockRestricted(stocks.stream().filter(s -> "사용 제한".equals(s.getStockStatus())).count());
        } catch (Exception ignored) {
            // 재고 조회 실패 시 0 유지
        }
        try {
            var inspects = inspectResultService.getInspectResults(null, null, null, null, null);
            domain.setInspectTotal(inspects.size());
            domain.setInspectFailed(inspects.stream()
                    .filter(i -> i.getFailQty() != null && i.getFailQty().signum() > 0)
                    .count());
        } catch (Exception ignored) {
            // 검사 조회 실패 시 0 유지
        }
        try {
            domain.setDefectCount(defectHistoryService.getDefectHistories(null, null, null, null).size());
        } catch (Exception ignored) {
            // 불량 조회 실패 시 0 유지
        }
        try {
            var statuses = equipmentService.getOperStatuses(null, null, null, null);
            domain.setEquipRunning(statuses.stream().filter(s -> "가동".equals(s.getOperStatus())).count());
            domain.setEquipDown(statuses.stream().filter(s -> "비가동".equals(s.getOperStatus())).count());
        } catch (Exception ignored) {
            // 설비 가동 조회 실패 시 0 유지
        }
        try {
            domain.setEquipDowntime(equipmentService.getDowntimes(null, null, null, null).size());
        } catch (Exception ignored) {
            // 비가동 이력 조회 실패 시 0 유지
        }
        return domain;
    }

    /**
     * 운영 브리핑에서 즉시 확인해야 할 PLC 이벤트만 중요 이벤트로 추린다.
     */
    private boolean isCriticalEvent(McsTransferClient.McsPlcEventSummary event) {
        String eventType = AiTextSupport.text(event.getEventType()).toUpperCase();
        String eventStatus = AiTextSupport.text(event.getEventStatus()).toUpperCase();
        String processResult = AiTextSupport.text(event.getProcessResult()).toUpperCase();
        return "VALIDATION_FAILED".equals(processResult)
                || "ERROR".equals(eventStatus)
                || eventType.contains("ERROR")
                || eventType.contains("INTERLOCK")
                || eventType.contains("TIMEOUT")
                || eventType.contains("MISMATCH")
                || eventType.contains("FAILED");
    }

    private String eventMessage(McsTransferClient.McsPlcEventSummary event) {
        String processMessage = AiTextSupport.text(event.getProcessMessage());
        if (!"-".equals(processMessage)) {
            return processMessage;
        }
        return AiTextSupport.text(event.getEventMessage());
    }

    private boolean isToday(String value, LocalDate today) {
        LocalDateTime dateTime = parseDateTime(value);
        return dateTime != null && today.equals(dateTime.toLocalDate());
    }

    private boolean isRecent(String value, LocalDateTime from, LocalDateTime to) {
        LocalDateTime dateTime = parseDateTime(value);
        return dateTime != null && !dateTime.isBefore(from) && !dateTime.isAfter(to);
    }

    /**
     * MCS API와 DB에서 들어오는 날짜 문자열 형식이 달라 두 가지 형식을 모두 시도한다.
     */
    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        try {
            return LocalDateTime.parse(normalized);
        } catch (Exception ignored) {
            // DB 스타일 날짜 형식으로 한 번 더 시도한다.
        }
        try {
            return LocalDateTime.parse(normalized, SPACE_DATE_TIME);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * MCS 이동 목록과 PLC 이벤트 목록을 한 번에 들고 다니기 위한 내부 묶음이다.
     */
    private record TransferSnapshot(
            List<McsTransferClient.McsTransferSummary> transfers,
            List<McsTransferClient.McsPlcEventSummary> events
    ) {
    }
}
