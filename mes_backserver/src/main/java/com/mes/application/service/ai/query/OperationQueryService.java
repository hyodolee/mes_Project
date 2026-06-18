package com.mes.application.service.ai.query;

import com.mes.application.service.ai.support.AiClientGateway;
import com.mes.application.service.ai.support.SensitiveDataSanitizer;
import com.mes.application.service.planning.McsTransferClient;
import com.mes.application.service.planning.WorkOrderService;
import com.mes.domain.ai.dto.NaturalLanguageQueryRequest;
import com.mes.domain.ai.dto.NaturalLanguageQueryResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * 운영 현황을 자연어 질문으로 조회하는 서비스.
 *
 * <p>사용자 질문을 받아 필요한 조회 기능을 모델에 제공하고,
 * 모델이 조회 결과를 참고해 답변을 만들도록 한다.</p>
 */
@Service
public class OperationQueryService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OperationQueryService.class);

    /**
     * 토큰 사이 최대 대기(반응형 스트림 무응답 감지). 이 간격을 넘으면 끊긴 것으로 본다.
     * 툴콜이 많은 질문은 모든 도구 호출이 끝나야 첫 토큰이 나오므로 첫 토큰까지 40초 이상 걸릴 수 있다.
     * 너무 짧으면(예: 45초) 정상 응답이 폴백으로 떨어지므로 넉넉히 잡는다(SSE wall-clock 180초보다 작게).
     */
    private static final int AI_TIMEOUT_SECONDS = 120;

    /**
     * SSE 연결 전체 wall-clock 제한(ms).
     * RAG 검색(임베딩+Chroma) + 툴콜 + 긴 답변 생성은 30초를 쉽게 넘기므로,
     * 토큰 간 타임아웃({@link #AI_TIMEOUT_SECONDS})보다 넉넉히 길게 잡아 정상 응답이 중간에 끊기지 않게 한다.
     */
    private static final long SSE_TIMEOUT_MILLIS = 180_000L;

    private final WorkOrderService workOrderService;
    private final McsTransferClient mcsTransferClient;
    private final OperationToolsFactory operationToolsFactory;
    private final AiClientGateway aiClientGateway;
    private final ChatMemory chatMemory;
    private final MessageChatMemoryAdvisor chatMemoryAdvisor;
    private final SensitiveDataSanitizer sensitiveDataSanitizer;

    public OperationQueryService(
            WorkOrderService workOrderService,
            McsTransferClient mcsTransferClient,
            OperationToolsFactory operationToolsFactory,
            AiClientGateway aiClientGateway,
            ChatMemory chatMemory,
            MessageChatMemoryAdvisor chatMemoryAdvisor,
            SensitiveDataSanitizer sensitiveDataSanitizer
    ) {
        this.workOrderService = workOrderService;
        this.mcsTransferClient = mcsTransferClient;
        this.operationToolsFactory = operationToolsFactory;
        this.aiClientGateway = aiClientGateway;
        this.chatMemory = chatMemory;
        this.chatMemoryAdvisor = chatMemoryAdvisor;
        this.sensitiveDataSanitizer = sensitiveDataSanitizer;
    }

    public NaturalLanguageQueryResponse query(NaturalLanguageQueryRequest request) {
        // 모델 호출 준비. API 키나 클라이언트 설정이 없으면 기본 현황 요약으로 대체한다.
        ChatClient.Builder builder = aiClientGateway.getBuilderOrNull();
        if (builder == null) {
            return ruleBasedFallback(request.getQuestion(), "AI 설정이 없어 기본 조회 결과로 답변합니다.");
        }

        // 이번 질문에서 사용할 조회 도구를 준비한다.
        // dataPoints에는 실제 조회한 내역이 쌓이며, 화면의 "근거 데이터"로 보여준다.
        List<String> dataPoints = new CopyOnWriteArrayList<>();
        OperationTools tools = operationToolsFactory.create(dataPoints);

        try {
            // 모델 응답이 오래 걸리면 화면이 멈추지 않도록 제한 시간을 둔다.
            String answer = CompletableFuture.supplyAsync(() -> {
                try {
                    return callAi(builder, request, tools);
                } catch (Exception e) {
                    throw new java.util.concurrent.CompletionException(e);
                }
            }).orTimeout(AI_TIMEOUT_SECONDS, TimeUnit.SECONDS).join();

            return new NaturalLanguageQueryResponse(answer, "AI_TOOL", List.copyOf(dataPoints), true, aiClientGateway.getModel());
        } catch (Exception e) {
            log.warn("AI 질의 실패, 규칙 기반 답변으로 폴백. 원인: {}", e.getMessage());
            return ruleBasedFallback(request.getQuestion(), "AI 분석 호출이 실패해 기본 조회 결과로 답변합니다.");
        }
    }

    public SseEmitter streamQuery(NaturalLanguageQueryRequest request) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        emitter.onTimeout(() -> {
            log.warn("AI 스트리밍 SSE 타임아웃({}ms) — 연결 종료", SSE_TIMEOUT_MILLIS);
            emitter.complete();
        });
        emitter.onError(e -> log.warn("AI 스트리밍 SSE 오류: {}", e.getMessage()));

        ChatClient.Builder builder = aiClientGateway.getBuilderOrNull();

        if (builder == null) {
            NaturalLanguageQueryResponse fallback = ruleBasedFallback(
                    request.getQuestion(),
                    "AI 설정이 없어 기본 조회 결과로 답변합니다."
            );
            sendEvent(emitter, "token", fallback.getAnswer());
            sendEvent(emitter, "done", fallback);
            emitter.complete();
            return emitter;
        }

        List<String> dataPoints = new CopyOnWriteArrayList<>();
        OperationTools tools = operationToolsFactory.create(dataPoints);

        CompletableFuture.runAsync(() -> {
            StringBuilder answer = new StringBuilder();
            try {
                ChatClient.ChatClientRequestSpec spec = buildRequestSpec(builder, request, tools);
                spec.stream()
                        .content()
                        .timeout(java.time.Duration.ofSeconds(AI_TIMEOUT_SECONDS))
                        .doOnNext(token -> {
                            answer.append(token);
                            sendEvent(emitter, "token", token);
                        })
                        .blockLast();

                NaturalLanguageQueryResponse response = new NaturalLanguageQueryResponse(
                        answer.toString(),
                        "AI_TOOL",
                        List.copyOf(dataPoints),
                        true,
                        aiClientGateway.getModel()
                );
                sendEvent(emitter, "data-points", response.getDataPoints());
                sendEvent(emitter, "done", response);
                emitter.complete();
            } catch (Exception e) {
                log.warn("AI 스트리밍 질의 실패. 원인: {}", e.getMessage());
                if (answer.isEmpty()) {
                    NaturalLanguageQueryResponse fallback = ruleBasedFallback(
                            request.getQuestion(),
                            "AI 분석 호출이 지연되어 기본 조회 결과로 먼저 답변합니다."
                    );
                    sendEvent(emitter, "token", fallback.getAnswer());
                    sendEvent(emitter, "done", fallback);
                } else {
                    sendEvent(emitter, "error", "응답 생성 중 연결이 중단되었습니다.");
                }
                emitter.complete();
            }
        });

        return emitter;
    }

    public void clearMemory(String conversationId) {
        String normalized = normalizeConversationId(conversationId);
        if (normalized != null) {
            chatMemory.clear(normalized);
        }
    }

    private String callAi(
            ChatClient.Builder builder,
            NaturalLanguageQueryRequest request,
            OperationTools tools
    ) {
        return buildRequestSpec(builder, request, tools)
                .call()
                .content();
    }

    private ChatClient.ChatClientRequestSpec buildRequestSpec(
            ChatClient.Builder builder,
            NaturalLanguageQueryRequest request,
            OperationTools tools
    ) {
        String conversationId = normalizeConversationId(request.getConversationId());

        // system: 답변 역할과 규칙
        // user: 사용자의 실제 질문
        // tools: 답변에 필요한 데이터를 조회할 수 있는 메서드 목록
        ChatClient.ChatClientRequestSpec spec = builder.build()
                .prompt()
                .system(OperationQueryPrompt.SYSTEM)
                .user(buildUserPrompt(request, conversationId))
                .tools(tools);

        // 같은 대화방의 이전 질문/답변을 함께 참고하도록 대화 ID를 지정한다.
        // 프론트가 매번 전체 대화 이력을 보내지 않아도 후속 질문 맥락을 이어갈 수 있다.
        if (conversationId != null) {
            spec.advisors(advisor -> advisor
                    .param(ChatMemory.CONVERSATION_ID, conversationId)
                    .advisors(this.chatMemoryAdvisor));
        }

        return spec;
    }

    private String buildUserPrompt(NaturalLanguageQueryRequest request, String conversationId) {
        StringBuilder user = new StringBuilder();

        // 대화 ID가 없으면 서버 메모리를 사용할 수 없으므로 최근 대화 일부를 질문에 직접 붙인다.
        if (conversationId == null && request.getHistory() != null && !request.getHistory().isEmpty()) {
            user.append("직전 대화:\n");
            request.getHistory().stream()
                    .skip(Math.max(0, request.getHistory().size() - 6))
                    .forEach(turn -> user.append("user".equals(turn.getRole()) ? "담당자: " : "도우미: ")
                            .append(sensitiveDataSanitizer.mask(turn.getText())).append("\n"));
            user.append("\n");
        }

        user.append("질문: ").append(sensitiveDataSanitizer.mask(request.getQuestion()));
        return user.toString();
    }

    /**
     * AI 호출이 실패했을 때 사용하는 기본 조회 답변.
     *
     * <p>모델이 만든 분석처럼 보이면 사용자가 오해할 수 있으므로,
     * 기본 조회로 전환했다는 사실을 답변에 함께 표시한다.</p>
     */
    private NaturalLanguageQueryResponse ruleBasedFallback(String question, String reason) {
        List<String> dataPoints = new ArrayList<>();
        List<String> issues = new ArrayList<>();
        List<String> summaries = new ArrayList<>();
        OperationTools tools = operationToolsFactory.create(dataPoints);

        collectTransferSummary(tools, issues, summaries);
        collectPlcSummary(tools, issues, summaries);
        collectWorkOrderSummary(tools, issues, summaries);

        if (isIssueQuestion(question)) {
            collectStockSummary(tools, issues, summaries);
            collectEquipmentSummary(tools, issues, summaries);
            collectQualitySummary(tools, issues, summaries);
            collectDefectSummary(tools, issues, summaries);
        }

        String answer;
        if (dataPoints.isEmpty()) {
            answer = "지금은 데이터를 가져오지 못했어요. 잠시 후 다시 시도해 주세요.";
        } else if (issues.isEmpty()) {
            answer = reason + "\n현재 기본 조회에서 바로 드러난 문제는 없습니다.\n"
                    + "확인 범위: " + String.join(", ", summaries) + ".";
        } else {
            answer = reason + "\n현재 기본 조회에서 확인된 문제는 "
                    + String.join(", ", issues) + "입니다.\n"
                    + "확인 범위: " + String.join(", ", summaries) + ".";
        }
        return new NaturalLanguageQueryResponse(answer, "FALLBACK", dataPoints, false, null);
    }

    private void collectTransferSummary(OperationTools tools, List<String> issues, List<String> summaries) {
        try {
            var transfers = tools.getTransfers();
            long failed = transfers.stream().filter(t -> "FAILED".equals(t.getStatus())).count();
            summaries.add("자재 이동 " + transfers.size() + "건");
            addIssue(issues, "자재 이동 실패", failed);
        } catch (Exception e) {
            log.debug("fallback 자재 이동 조회 실패", e);
        }
    }

    private void collectPlcSummary(OperationTools tools, List<String> issues, List<String> summaries) {
        try {
            var events = tools.getRecentPlcEvents();
            long errors = events.stream()
                    .filter(e -> hasAny(e.getEventStatus(), "ERROR", "FAIL", "FAILED", "INTERLOCK")
                            || hasAny(e.getProcessResult(), "ERROR", "FAIL", "FAILED", "VALIDATION_FAILED"))
                    .count();
            summaries.add("PLC 이벤트 " + events.size() + "건");
            addIssue(issues, "PLC 오류/검증 실패", errors);
        } catch (Exception e) {
            log.debug("fallback PLC 이벤트 조회 실패", e);
        }
    }

    private void collectWorkOrderSummary(OperationTools tools, List<String> issues, List<String> summaries) {
        try {
            var orders = tools.getWorkOrders();
            long inProgress = orders.stream().filter(o -> "진행".equals(o.getStatus())).count();
            long pending = orders.stream().filter(o -> "대기".equals(o.getStatus())).count();
            summaries.add("작업지시 " + orders.size() + "건");
            if (pending > 0) {
                issues.add("대기 작업지시 " + pending + "건");
            } else if (inProgress > 0) {
                summaries.add("진행 작업지시 " + inProgress + "건");
            }
        } catch (Exception e) {
            log.debug("fallback 작업지시 조회 실패", e);
        }
    }

    private void collectStockSummary(OperationTools tools, List<String> issues, List<String> summaries) {
        try {
            var stocks = tools.getStocks();
            long blocked = stocks.stream()
                    .filter(s -> isAbnormalStockStatus(s.getStockStatus()) || zeroOrLess(s.getAvailableQty()))
                    .count();
            summaries.add("재고 " + stocks.size() + "건");
            addIssue(issues, "가용 불가/비정상 재고", blocked);
        } catch (Exception e) {
            log.debug("fallback 재고 조회 실패", e);
        }
    }

    private void collectEquipmentSummary(OperationTools tools, List<String> issues, List<String> summaries) {
        try {
            var statuses = tools.getEquipmentStatus();
            long abnormal = statuses.stream()
                    .filter(s -> isAbnormalEquipmentStatus(s.getOperStatus()))
                    .count();
            summaries.add("설비 상태 " + statuses.size() + "건");
            addIssue(issues, "비정상 설비 상태", abnormal);
        } catch (Exception e) {
            log.debug("fallback 설비 상태 조회 실패", e);
        }

        try {
            var downtimes = tools.getEquipmentDowntimes();
            summaries.add("설비 비가동 " + downtimes.size() + "건");
            addIssue(issues, "설비 비가동", downtimes.size());
        } catch (Exception e) {
            log.debug("fallback 설비 비가동 조회 실패", e);
        }
    }

    private void collectQualitySummary(OperationTools tools, List<String> issues, List<String> summaries) {
        try {
            var inspections = tools.getInspectResults();
            long failed = inspections.stream()
                    .filter(i -> positive(i.getFailQty()) || hasAny(i.getJudgeResult(), "FAIL", "NG", "부적합", "불합격"))
                    .count();
            summaries.add("품질 검사 " + inspections.size() + "건");
            addIssue(issues, "검사 부적합", failed);
        } catch (Exception e) {
            log.debug("fallback 품질 검사 조회 실패", e);
        }
    }

    private void collectDefectSummary(OperationTools tools, List<String> issues, List<String> summaries) {
        try {
            var defects = tools.getDefects();
            double defectQty = defects.stream()
                    .map(OperationTools.DefectView::getDefectQty)
                    .filter(v -> v != null && v > 0)
                    .mapToDouble(Double::doubleValue)
                    .sum();
            summaries.add("불량 이력 " + defects.size() + "건");
            if (defectQty > 0) {
                issues.add("불량 수량 " + formatNumber(defectQty));
            } else {
                addIssue(issues, "불량 이력", defects.size());
            }
        } catch (Exception e) {
            log.debug("fallback 불량 이력 조회 실패", e);
        }
    }

    private boolean isIssueQuestion(String question) {
        return hasAny(question, "문제", "이상", "오류", "실패", "불량", "부적합", "부족", "정지", "그 밖", "또", "전체");
    }

    private void addIssue(List<String> issues, String label, long count) {
        if (count > 0) {
            issues.add(label + " " + count + "건");
        }
    }

    private boolean hasAny(String value, String... keywords) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String upper = value.toUpperCase();
        for (String keyword : keywords) {
            if (upper.contains(keyword.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean zeroOrLess(Double value) {
        return value != null && value <= 0;
    }

    private boolean positive(Double value) {
        return value != null && value > 0;
    }

    private boolean isAbnormalStockStatus(String status) {
        return status != null && !status.isBlank()
                && !hasAny(status, "정상", "NORMAL", "AVAILABLE", "OK", "-");
    }

    private boolean isAbnormalEquipmentStatus(String status) {
        return status != null && !status.isBlank()
                && !hasAny(status, "정상", "가동", "RUN", "RUNNING", "OK", "-");
    }

    private String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format("%.1f", value);
    }

    private void sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private String normalizeConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return null;
        }
        String normalized = conversationId.trim();
        return normalized.length() > 120 ? normalized.substring(0, 120) : normalized;
    }
}
