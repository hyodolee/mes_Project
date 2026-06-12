package com.mes.application.service.ai.query;

import com.mes.application.service.ai.support.AiClientGateway;
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

    private final WorkOrderService workOrderService;
    private final McsTransferClient mcsTransferClient;
    private final OperationToolsFactory operationToolsFactory;
    private final AiClientGateway aiClientGateway;
    private final ChatMemory chatMemory;
    private final MessageChatMemoryAdvisor chatMemoryAdvisor;

    public OperationQueryService(
            WorkOrderService workOrderService,
            McsTransferClient mcsTransferClient,
            OperationToolsFactory operationToolsFactory,
            AiClientGateway aiClientGateway,
            ChatMemory chatMemory,
            MessageChatMemoryAdvisor chatMemoryAdvisor
    ) {
        this.workOrderService = workOrderService;
        this.mcsTransferClient = mcsTransferClient;
        this.operationToolsFactory = operationToolsFactory;
        this.aiClientGateway = aiClientGateway;
        this.chatMemory = chatMemory;
        this.chatMemoryAdvisor = chatMemoryAdvisor;
    }

    public NaturalLanguageQueryResponse query(NaturalLanguageQueryRequest request) {
        // 모델 호출 준비. API 키나 클라이언트 설정이 없으면 기본 현황 요약으로 대체한다.
        ChatClient.Builder builder = aiClientGateway.getBuilderOrNull();
        if (builder == null) {
            return ruleBasedFallback(request.getQuestion());
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
            }).orTimeout(25, TimeUnit.SECONDS).join();

            return new NaturalLanguageQueryResponse(answer, "AI_TOOL", List.copyOf(dataPoints), true, aiClientGateway.getModel());
        } catch (Exception e) {
            log.warn("AI 질의 실패, 규칙 기반 답변으로 폴백. 원인: {}", e.getMessage());
            return ruleBasedFallback(request.getQuestion());
        }
    }

    public SseEmitter streamQuery(NaturalLanguageQueryRequest request) {
        SseEmitter emitter = new SseEmitter(30_000L);
        ChatClient.Builder builder = aiClientGateway.getBuilderOrNull();

        if (builder == null) {
            NaturalLanguageQueryResponse fallback = ruleBasedFallback(request.getQuestion());
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
                        .timeout(java.time.Duration.ofSeconds(25))
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
                    NaturalLanguageQueryResponse fallback = ruleBasedFallback(request.getQuestion());
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
                            .append(turn.getText()).append("\n"));
            user.append("\n");
        }

        user.append("질문: ").append(request.getQuestion());
        return user.toString();
    }

    /**
     * 모델을 호출할 수 없을 때 보여줄 단순 현황 요약.
     */
    private NaturalLanguageQueryResponse ruleBasedFallback(String question) {
        List<String> points = new ArrayList<>();
        try {
            var transfers = mcsTransferClient.getAllTransfers(50);
            long failed = transfers.stream().filter(t -> "FAILED".equals(t.getTransferStatus())).count();
            points.add("자재 이동 " + transfers.size() + "건 (실패 " + failed + "건)");
        } catch (Exception ignored) {
            // MCS 미연결 시 무시
        }
        try {
            var orders = workOrderService.getWorkOrders(null, null, null, null, null, null);
            long inProgress = orders.stream().filter(o -> "진행".equals(o.getWoStatus())).count();
            points.add("작업지시 " + orders.size() + "건 (진행 " + inProgress + "건)");
        } catch (Exception ignored) {
            // 무시
        }

        String answer = points.isEmpty()
                ? "지금은 데이터를 가져오지 못했어요. 잠시 후 다시 시도해 주세요."
                : "현재 현황을 알려드릴게요. " + String.join(", ", points) + ".";
        return new NaturalLanguageQueryResponse(answer, "FALLBACK", points, false, null);
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
