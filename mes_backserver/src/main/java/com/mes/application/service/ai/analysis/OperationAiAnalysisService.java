package com.mes.application.service.ai.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.application.service.ai.support.AiClientGateway;
import com.mes.application.service.ai.support.AiJsonSupport;
import com.mes.application.service.ai.support.AiTextSupport;
import com.mes.application.service.ai.support.SensitiveDataSanitizer;
import com.mes.application.service.equipment.EquipmentService;
import com.mes.application.service.inventory.InventoryService;
import com.mes.application.service.planning.McsTransferClient;
import com.mes.application.service.planning.WorkOrderService;
import com.mes.application.service.production.DefectHistoryService;
import com.mes.application.service.quality.InspectResultService;
import com.mes.domain.ai.dto.AreaAssessmentDto;
import com.mes.domain.ai.dto.CriticalEventDto;
import com.mes.domain.ai.dto.GlobalOperationAiAnalysisResponse;
import com.mes.domain.ai.dto.GlobalOperationEvidenceDto;
import com.mes.domain.ai.dto.McsTransferSummaryDto;
import com.mes.domain.ai.dto.OperationDomainSummaryDto;
import com.mes.domain.ai.dto.WorkOrderSummaryDto;
import com.mes.domain.inventory.stock.dto.StockDto;
import com.mes.domain.inventory.stock.dto.StockSearchDto;
import com.mes.domain.planning.workorder.dto.WorkOrderDto;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class OperationAiAnalysisService {

    private final WorkOrderService workOrderService;
    private final McsTransferClient mcsTransferClient;
    private final AiClientGateway aiClientGateway;
    private final ObjectMapper objectMapper;
    private final InventoryService inventoryService;
    private final InspectResultService inspectResultService;
    private final DefectHistoryService defectHistoryService;
    private final EquipmentService equipmentService;
    private final SensitiveDataSanitizer sensitiveDataSanitizer;

    public OperationAiAnalysisService(
            WorkOrderService workOrderService,
            McsTransferClient mcsTransferClient,
            AiClientGateway aiClientGateway,
            ObjectMapper objectMapper,
            InventoryService inventoryService,
            InspectResultService inspectResultService,
            DefectHistoryService defectHistoryService,
            EquipmentService equipmentService,
            SensitiveDataSanitizer sensitiveDataSanitizer
    ) {
        this.workOrderService = workOrderService;
        this.mcsTransferClient = mcsTransferClient;
        this.aiClientGateway = aiClientGateway;
        this.objectMapper = objectMapper;
        this.inventoryService = inventoryService;
        this.inspectResultService = inspectResultService;
        this.defectHistoryService = defectHistoryService;
        this.equipmentService = equipmentService;
        this.sensitiveDataSanitizer = sensitiveDataSanitizer;
    }

    private static final long CACHE_TTL_MILLIS = 30_000;
    private static final DateTimeFormatter SPACE_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private volatile GlobalOperationAiAnalysisResponse cachedResponse;
    private volatile long cachedAtMillis;

    public GlobalOperationAiAnalysisResponse analyze() {
        return analyze(false);
    }

    public GlobalOperationAiAnalysisResponse analyze(boolean refresh) {
        GlobalOperationAiAnalysisResponse cached = cachedResponse;
        if (!refresh && cached != null && System.currentTimeMillis() - cachedAtMillis < CACHE_TTL_MILLIS) {
            return cached;
        }
        GlobalOperationAiAnalysisResponse fresh = doAnalyze();
        cachedResponse = fresh;
        cachedAtMillis = System.currentTimeMillis();
        return fresh;
    }

    /**
     * 운영 스냅샷을 수집한 뒤 모델 분석을 시도하고, 실패하면 규칙 기반 요약으로 대체한다.
     */
    private GlobalOperationAiAnalysisResponse doAnalyze() {
        GlobalOperationEvidenceDto evidence = collectEvidence();

        ChatClient.Builder builder = aiClientGateway.getBuilderOrNull();
        if (builder == null) {
            return ruleBasedAnalysis(evidence, false, "local-preview");
        }

        try {
            String content = CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            return callAi(builder, evidence);
                        } catch (Exception e) {
                            throw new java.util.concurrent.CompletionException(e);
                        }
                    })
                    .orTimeout(30, TimeUnit.SECONDS)
                    .join();
            return parseAiResponse(content, evidence);
        } catch (java.util.concurrent.CompletionException e) {
            String label = e.getCause() instanceof TimeoutException
                    ? "timeout(30s)"
                    : "fallback: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            return ruleBasedAnalysis(evidence, false, label);
        } catch (Exception e) {
            return ruleBasedAnalysis(evidence, false, "fallback: " + e.getMessage());
        }
    }

    /**
     * 전체 운영 브리핑에 필요한 작업지시, MCS 이송, PLC 이벤트 통계를 수집한다.
     */
    private GlobalOperationEvidenceDto collectEvidence() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);

        // 날짜 제한 없이 전체 작업지시 조회 (더미 데이터 포함)
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

        WorkOrderSummaryDto woSummary = new WorkOrderSummaryDto(
                total, inProgress, delayed, pending, completedToday
        );

        // MCS HTTP 호출 2개를 병렬로 실행
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

        long activeTransfers = transfers.stream().filter(t -> "REQUESTED".equals(t.getTransferStatus()) || "IN_PROGRESS".equals(t.getTransferStatus())).count();
        long failedTransfers = transfers.stream().filter(t -> "FAILED".equals(t.getTransferStatus())).count();
        long completedTransfersToday = transfers.stream()
                .filter(t -> "COMPLETED".equals(t.getTransferStatus()))
                .filter(t -> isToday(t.getUpdDtm(), today))
                .count();

        McsTransferSummaryDto transferSummary = new McsTransferSummaryDto(
                activeTransfers, failedTransfers, completedTransfersToday
        );

        List<CriticalEventDto> criticalEvents = events.stream()
                .filter(e -> isRecent(e.getEventDtm(), oneHourAgo, now))
                .filter(this::isCriticalEvent)
                .map(e -> new CriticalEventDto(
                        e.getEventType(), e.getLocationCd(), eventMessage(e), e.getEventDtm()
                )).toList();

        return new GlobalOperationEvidenceDto(woSummary, transferSummary, criticalEvents, collectDomains());
    }

    /**
     * 품질·재고·설비 요약을 수집한다. 한 영역 조회가 실패해도 나머지는 채운다.
     */
    private OperationDomainSummaryDto collectDomains() {
        OperationDomainSummaryDto d = new OperationDomainSummaryDto();
        try {
            StockSearchDto search = new StockSearchDto();
            search.setSize(1000);
            List<StockDto> stocks = inventoryService.getStocks(search);
            d.setStockLow(stocks.stream().filter(s -> "부족".equals(s.getStockStatus())).count());
            d.setStockRestricted(stocks.stream().filter(s -> "사용 제한".equals(s.getStockStatus())).count());
        } catch (Exception ignored) { /* 재고 조회 실패 시 0 유지 */ }
        try {
            var inspects = inspectResultService.getInspectResults(null, null, null, null);
            d.setInspectTotal(inspects.size());
            d.setInspectFailed(inspects.stream()
                    .filter(i -> i.getFailQty() != null && i.getFailQty().signum() > 0).count());
        } catch (Exception ignored) { /* 검사 조회 실패 시 0 유지 */ }
        try {
            d.setDefectCount(defectHistoryService.getDefectHistories(null, null, null, null).size());
        } catch (Exception ignored) { /* 불량 조회 실패 시 0 유지 */ }
        try {
            var statuses = equipmentService.getOperStatuses(null, null, null, null);
            d.setEquipRunning(statuses.stream().filter(s -> "가동".equals(s.getOperStatus())).count());
            d.setEquipDown(statuses.stream().filter(s -> "비가동".equals(s.getOperStatus())).count());
        } catch (Exception ignored) { /* 설비 가동 조회 실패 시 0 유지 */ }
        try {
            d.setEquipDowntime(equipmentService.getDowntimes(null, null, null, null).size());
        } catch (Exception ignored) { /* 비가동 이력 조회 실패 시 0 유지 */ }
        return d;
    }

    /**
     * 운영 스냅샷을 모델에 전달해 JSON 분석 결과를 요청한다.
     */
    private String callAi(ChatClient.Builder builder, GlobalOperationEvidenceDto evidence) throws Exception {
        return builder.build()
                .prompt()
                .system(systemPrompt())
                .user(userPrompt(evidence))
                .call()
                .content();
    }

    /**
     * 모델이 반환한 JSON 문자열을 운영 브리핑 응답으로 변환하고 화면 표시용 길이를 보정한다.
     */
    private GlobalOperationAiAnalysisResponse parseAiResponse(String content, GlobalOperationEvidenceDto evidence) throws Exception {
        String json = AiJsonSupport.extractJsonObject(content);
        JsonNode root = objectMapper.readTree(json);
        return new GlobalOperationAiAnalysisResponse(
                normalizeSeverity(AiJsonSupport.nodeToText(root.get("severity")), evidence),
                AiTextSupport.compactText(AiJsonSupport.nodeToText(root.get("summary")), 400),
                AiTextSupport.compactList(AiJsonSupport.nodeToList(root.get("keyIssues")), 8, 40),
                AiTextSupport.compactText(AiJsonSupport.nodeToText(root.get("inference")), 260),
                AiTextSupport.compactText(AiJsonSupport.nodeToText(root.get("productionImpact")), 160),
                AiTextSupport.compactList(AiJsonSupport.nodeToList(root.get("recommendedActions")), 6, 60),
                parseAreaAssessments(root.get("areaAssessments")),
                evidence,
                true,
                aiClientGateway.getModel()
        );
    }

    /** AI가 반환한 영역별 진단 배열을 파싱한다. */
    private List<AreaAssessmentDto> parseAreaAssessments(JsonNode arr) {
        List<AreaAssessmentDto> list = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (JsonNode n : arr) {
                String area = AiJsonSupport.nodeToText(n.get("area"));
                if (area == null || area.isBlank()) {
                    continue;
                }
                list.add(new AreaAssessmentDto(
                        area,
                        AiJsonSupport.nodeToText(n.get("status")),
                        AiTextSupport.compactText(AiJsonSupport.nodeToText(n.get("comment")), 60)
                ));
            }
        }
        return list;
    }

    /**
     * 모델 호출이 불가능할 때도 운영 분석 화면에 최소한의 상태 요약을 제공한다.
     */
    private GlobalOperationAiAnalysisResponse ruleBasedAnalysis(GlobalOperationEvidenceDto evidence, boolean aiGenerated, String modelLabel) {
        WorkOrderSummaryDto wo = evidence.getWorkOrders();
        McsTransferSummaryDto tr = evidence.getTransfers();
        OperationDomainSummaryDto dm = evidence.getDomains() != null ? evidence.getDomains() : new OperationDomainSummaryDto();
        int criticalCount = evidence.getCriticalEvents().size();
        long domainIssues = dm.getInspectFailed() + dm.getDefectCount() + dm.getStockLow() + dm.getStockRestricted() + dm.getEquipDown();

        String severity = "NORMAL";
        if (tr.getFailed() > 0 || criticalCount > 0 || wo.getDelayed() > 0 || domainIssues > 0) {
            severity = "WARNING";
        }
        if (tr.getFailed() > 3 || criticalCount > 3) {
            severity = "CRITICAL";
        }

        // 주요 이슈: 운영 스냅샷에서 의미 있는 항목을 우선순위대로 최대 6개까지 구성한다.
        List<String> keyIssues = new ArrayList<>();
        if (tr.getFailed() > 0) {
            keyIssues.add("이송 실패 " + tr.getFailed() + "건");
        }
        if (criticalCount > 0) {
            keyIssues.add("최근 1시간 중요 이벤트 " + criticalCount + "건");
        }
        if (dm.getInspectFailed() > 0) {
            keyIssues.add("검사 부적합 " + dm.getInspectFailed() + "건");
        }
        if (dm.getStockLow() + dm.getStockRestricted() > 0) {
            keyIssues.add("재고 주의 " + (dm.getStockLow() + dm.getStockRestricted()) + "품목");
        }
        if (dm.getEquipDown() > 0) {
            keyIssues.add("설비 비가동 " + dm.getEquipDown() + "대");
        }
        if (wo.getDelayed() > 0) {
            keyIssues.add("시작 지연 우려 작업 " + wo.getDelayed() + "건");
        }
        keyIssues.add("작업 진행 " + wo.getInProgress() + "건 · 대기 " + wo.getPending() + "건");
        keyIssues.add("전체 작업 " + wo.getTotal() + "건 · 오늘 완료 " + wo.getCompletedToday() + "건");
        if (tr.getActive() > 0) {
            keyIssues.add("진행 중 이송 " + tr.getActive() + "건");
        }
        keyIssues.add("오늘 이송 완료 " + tr.getCompletedToday() + "건");

        // 상태(이송 실패 → 주의 → 정상)에 따라 요약/추정/영향/조치를 다르게 구성한다.
        String summary;
        String inference;
        String impact;
        List<String> actions = new ArrayList<>();

        if (tr.getFailed() > 0) {
            summary = "현재 공장은 " + severity + " 상태입니다. 이송 실패 " + tr.getFailed()
                    + "건, 진행 작업 " + wo.getInProgress() + "건, 대기 " + wo.getPending()
                    + "건이며, 전체 작업 " + wo.getTotal() + "건 중 오늘 " + wo.getCompletedToday() + "건이 완료됐습니다.";
            inference = "MCS 이송 실패가 남아 있어 해당 작업 지시의 자재 공급이 지연되고 있는 것으로 보입니다. "
                    + (criticalCount > 0
                        ? "최근 1시간 내 PLC 중요 이벤트도 " + criticalCount + "건 발생해 설비·통신 측 원인을 함께 살펴야 합니다."
                        : "현재 진행 중인 이송 " + tr.getActive() + "건이 정상적으로 완료되는지 모니터링이 필요합니다.");
            impact = "실패한 이송과 연결된 작업 지시는 시작이 막혀 오늘 생산 목표 달성이 어려울 수 있습니다. "
                    + "대기 작업 " + wo.getPending() + "건의 착수도 함께 지연될 가능성이 있어 우선 조치가 필요합니다.";
            actions.add("실패한 이송 오더 상세와 실패 사유 확인");
            actions.add("PLC 이벤트 로그에서 오류·검증 실패 점검");
            actions.add("필요 시 자재 요청을 다시 실행해 이송 재생성");
            actions.add("지연 작업의 계획 시작 시각 재조정 검토");
            actions.add("반복 실패 구간은 설비·통신 담당자에게 공유");
        } else if (criticalCount > 0 || wo.getDelayed() > 0) {
            summary = "현재 공장은 " + severity + " 상태입니다. 중요 이벤트 " + criticalCount
                    + "건, 시작 지연 우려 작업 " + wo.getDelayed() + "건이 있으며, 진행 작업 "
                    + wo.getInProgress() + "건, 진행 중 이송 " + tr.getActive() + "건이 운영 중입니다.";
            inference = "이송 실패는 없으나 PLC 중요 이벤트 또는 시작이 지연된 작업이 있어 주의가 필요한 상태입니다. "
                    + "이벤트가 반복되면 이송 흐름에도 영향을 줄 수 있어 조기 확인이 필요합니다.";
            impact = "지연 작업이 누적되면 후속 공정 일정과 당일 생산량에 영향을 줄 수 있습니다. "
                    + "현재 진행 중인 이송과 작업은 정상 흐름을 유지하고 있습니다.";
            actions.add("시작 지연 작업의 계획 시작 시각과 자재 준비 상태 점검");
            actions.add("최근 PLC 중요 이벤트의 발생 설비·원인 확인");
            actions.add("진행 중 이송 " + tr.getActive() + "건의 진행 상태 모니터링");
            actions.add("대기 작업 " + wo.getPending() + "건의 착수 준비 확인");
        } else {
            summary = "현재 공장은 정상 상태입니다. 전체 작업 " + wo.getTotal() + "건 중 진행 "
                    + wo.getInProgress() + "건, 대기 " + wo.getPending() + "건이며, 진행 중 이송 "
                    + tr.getActive() + "건이 정상 흐름으로 운영되고 있습니다.";
            inference = "이송 실패와 중요 이벤트가 없어 자재 흐름과 생산이 안정적으로 진행되고 있습니다. "
                    + "오늘 작업 " + wo.getCompletedToday() + "건, 이송 " + tr.getCompletedToday() + "건이 완료됐습니다.";
            impact = "현재 조건에서는 계획된 작업 지시를 정상적으로 진행할 수 있습니다. "
                    + "대기 작업의 착수 준비 상태만 주기적으로 확인하면 됩니다.";
            actions.add("진행 중 작업과 이송 상태 정기 모니터링");
            actions.add("대기 작업 " + wo.getPending() + "건의 시작 준비 상태 점검");
            actions.add("당일 생산 계획 대비 완료 추이 확인");
        }

        return new GlobalOperationAiAnalysisResponse(
                severity,
                summary,
                keyIssues.stream().limit(8).toList(),
                inference,
                impact,
                actions.stream().limit(6).toList(),
                buildRuleAreas(wo, tr, dm),
                evidence,
                aiGenerated,
                modelLabel
        );
    }

    /** AI 미사용 시 영역별 진단을 규칙으로 생성한다. */
    private List<AreaAssessmentDto> buildRuleAreas(WorkOrderSummaryDto wo, McsTransferSummaryDto tr, OperationDomainSummaryDto dm) {
        List<AreaAssessmentDto> areas = new ArrayList<>();
        areas.add(new AreaAssessmentDto("생산",
                wo.getDelayed() > 0 ? "WARNING" : "NORMAL",
                wo.getDelayed() > 0
                        ? "진행 " + wo.getInProgress() + "건, 시작 지연 우려 " + wo.getDelayed() + "건으로 일정 점검 필요"
                        : "진행 " + wo.getInProgress() + "건 · 대기 " + wo.getPending() + "건, 계획대로 진행 중"));
        areas.add(new AreaAssessmentDto("이송",
                tr.getFailed() > 0 ? "WARNING" : "NORMAL",
                tr.getFailed() > 0
                        ? "이송 " + tr.getFailed() + "건 실패 — 목적지·PLC 송신부 점검 필요"
                        : "진행 " + tr.getActive() + "건 정상 흐름, 실패 없음"));
        long q = dm.getInspectFailed() + dm.getDefectCount();
        areas.add(new AreaAssessmentDto("품질",
                q > 0 ? "WARNING" : "NORMAL",
                q > 0
                        ? "검사 부적합 " + dm.getInspectFailed() + "건 · 불량 " + dm.getDefectCount() + "건, 재작업 부담 발생"
                        : "검사 이상 없음, 불량 미발생"));
        long stock = dm.getStockLow() + dm.getStockRestricted();
        areas.add(new AreaAssessmentDto("재고",
                stock > 0 ? "WARNING" : "NORMAL",
                stock > 0
                        ? "부족 " + dm.getStockLow() + " · 사용제한 " + dm.getStockRestricted() + "품목, 공급 확인 필요"
                        : "재고 정상 범위, 부족 품목 없음"));
        areas.add(new AreaAssessmentDto("설비",
                dm.getEquipDown() > 0 ? "WARNING" : "NORMAL",
                dm.getEquipDown() > 0
                        ? "비가동 " + dm.getEquipDown() + "대 — 가동 복구 우선"
                        : "가동 " + dm.getEquipRunning() + "대 정상, 비가동 이력 " + dm.getEquipDowntime() + "건"));
        return areas;
    }

    /**
     * 알림/브리핑에서 중요 이벤트로 취급할 PLC 이벤트인지 판단한다.
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

    /**
     * 문자열 날짜가 오늘에 해당하는지 확인한다.
     */
    private boolean isToday(String value, LocalDate today) {
        LocalDateTime dateTime = parseDateTime(value);
        return dateTime != null && today.equals(dateTime.toLocalDate());
    }

    /**
     * 문자열 날짜가 지정한 시간 범위 안에 있는지 확인한다.
     */
    private boolean isRecent(String value, LocalDateTime from, LocalDateTime to) {
        LocalDateTime dateTime = parseDateTime(value);
        return dateTime != null && !dateTime.isBefore(from) && !dateTime.isAfter(to);
    }

    /**
     * MCS/MES에서 들어오는 ISO 형식 또는 DB 스타일 날짜 문자열을 LocalDateTime으로 변환한다.
     */
    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        try {
            return LocalDateTime.parse(normalized);
        } catch (Exception ignored) {
            // Try the DB-style format below.
        }
        try {
            return LocalDateTime.parse(normalized, SPACE_DATE_TIME);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 심각도 값이 비어 있거나 잘못됐을 때 운영 스냅샷 기준으로 보정한다.
     */
    private String normalizeSeverity(String value, GlobalOperationEvidenceDto evidence) {
        String normalized = AiTextSupport.text(value).toUpperCase();
        if ("NORMAL".equals(normalized) || "WARNING".equals(normalized) || "CRITICAL".equals(normalized)) {
            return normalized;
        }
        if (evidence.getTransfers().getFailed() > 3) {
            return "CRITICAL";
        }
        if (evidence.getTransfers().getFailed() > 0 || !evidence.getCriticalEvents().isEmpty()) {
            return "WARNING";
        }
        return "NORMAL";
    }

    /**
     * 전체 운영 브리핑용 시스템 프롬프트.
     */
    private String systemPrompt() {
        return """
                당신은 공장 운영 AI 분석관입니다. 제공된 통계 데이터만 근거로 한국어로 분석하세요.
                반드시 JSON 객체만 반환하세요. 다른 텍스트 없이 JSON만 출력하세요.

                분석 대상 영역(5개)을 모두 함께 살펴 종합 판단하세요:
                - 생산(workOrders): 진행/대기/지연/완료
                - 이송(transfers): 진행/실패/완료, criticalEvents(PLC 중요 이벤트)
                - 품질(domains): inspectTotal/inspectFailed(검사 부적합), defectCount(불량)
                - 재고(domains): stockLow(부족), stockRestricted(사용 제한)
                - 설비(domains): equipRunning(가동)/equipDown(비가동)/equipDowntime(비가동 이력)

                필드:
                - severity: NORMAL, WARNING, CRITICAL 중 하나 (이송 실패·검사 부적합·재고 부족·설비 비가동이 있으면 WARNING 이상)
                - summary: 현재 상황을 3~4문장으로 충분히 풍부하게. 5개 영역을 두루 언급하고, 핵심 수치와 영역 간 연결을 포함하세요.
                - areaAssessments: 5개 영역 각각의 한 줄 진단 배열. 반드시 5개(생산/이송/품질/재고/설비) 모두 포함.
                  각 항목 = {"area":"생산|이송|품질|재고|설비", "status":"NORMAL|WARNING|CRITICAL", "comment":"해당 영역 진단 한 줄, 50자 이내"}
                  정상 영역도 "정상 가동 중" 식으로 반드시 코멘트를 채우세요. 단순 숫자 나열이 아니라 의미를 짚으세요.
                - keyIssues: 핵심 이슈 5~8개, 각 30자 이내 (영역을 가로질러: 이송 실패, 검사 부적합, 재고 부족, 설비 비가동 등 구체적 수치로)
                - inference: 원인·병목 추정 2~3문장, 200자 이내. 여러 영역이 연결된 문제면 그 연결을 짚으세요(예: 이송 실패 + 해당 자재 재고 부족).
                - productionImpact: 생산 영향 2문장, 140자 이내
                - recommendedActions: 즉시 조치 5~6개, 각 40자 이내 (구체적인 확인 대상과 행동으로, 우선순위대로)
                모든 수치가 0이면 severity는 NORMAL, 각 영역 코멘트는 정상 내용으로 채우세요.
                영문 필드명(stockLow, stockRestricted, inspectFailed, equipDown 등)을 답변에 그대로 쓰지 말고
                반드시 한국어로 풀어 쓰세요(예: stockLow -> "부족 재고", inspectFailed -> "검사 부적합").
                제공된 통계 수치를 최대한 활용해 운영 담당자가 바로 판단할 수 있게 구체적이고 풍부하게 작성하세요.
                """;
    }

    /**
     * 운영 스냅샷 근거 데이터를 JSON으로 직렬화해 사용자 질문 영역에 넣는다.
     */
    private String userPrompt(GlobalOperationEvidenceDto evidence) throws Exception {
        return "현재 공장 운영 스냅샷 데이터:\n" + sensitiveDataSanitizer.mask(objectMapper.writeValueAsString(evidence));
    }

}
