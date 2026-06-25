package com.mes.application.service.ai.advisor;

import com.mes.application.service.ai.support.SensitiveDataSanitizer;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 호출 직전에 프롬프트 안의 민감정보를 마스킹한다.
 *
 * <p>
 * 실제 마스킹 규칙은 SensitiveDataSanitizer가 담당하고,
 * 이 Advisor는 ChatClient 요청에 들어가는 메시지들을 순회하며 규칙을 적용한다.
 * </p>
 */
@Component
public class SensitiveDataMaskingAdvisor implements BaseAdvisor {

    // 대화 기억 Advisor보다 먼저 실행해, 메모리에 저장되기 전의 사용자 입력도 마스킹한다.
    private static final int ORDER = Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER - 100;

    private final SensitiveDataSanitizer sensitiveDataSanitizer;

    public SensitiveDataMaskingAdvisor(SensitiveDataSanitizer sensitiveDataSanitizer) {
        this.sensitiveDataSanitizer = sensitiveDataSanitizer;
    }

    /**
     * 모델 호출 전에 system/user/tool 메시지의 문자열 값을 마스킹한다.
     */
    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        Prompt prompt = chatClientRequest.prompt();
        List<Message> maskedMessages = prompt.getInstructions().stream()
                .map(this::maskMessage)
                .toList();

        Prompt maskedPrompt = prompt.mutate()
                .messages(maskedMessages)
                .build();

        return chatClientRequest.mutate()
                .prompt(maskedPrompt)
                .build();
    }

    /**
     * 현재는 응답 후처리를 하지 않는다. 필요하면 AI 응답 마스킹도 이 지점에 추가할 수 있다.
     */
    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    /**
     * 메시지 종류별로 문자열이 들어 있는 위치가 달라 타입별로 복사하며 마스킹한다.
     */
    private Message maskMessage(Message message) {
        if (message instanceof UserMessage userMessage) {
            return userMessage.mutate()
                    .text(mask(userMessage.getText()))
                    .build();
        }
        if (message instanceof SystemMessage systemMessage) {
            return systemMessage.mutate()
                    .text(mask(systemMessage.getText()))
                    .build();
        }
        if (message instanceof AssistantMessage assistantMessage) {
            return new AssistantMessage(
                    mask(assistantMessage.getText()),
                    Map.copyOf(assistantMessage.getMetadata()),
                    maskToolCalls(assistantMessage.getToolCalls()),
                    List.copyOf(assistantMessage.getMedia()));
        }
        if (message instanceof ToolResponseMessage toolResponseMessage) {
            return new ToolResponseMessage(
                    maskToolResponses(toolResponseMessage.getResponses()),
                    Map.copyOf(toolResponseMessage.getMetadata()));
        }
        return message;
    }

    /**
     * Tool 호출 인자에도 사용자 입력이 들어갈 수 있어 함께 마스킹한다.
     */
    private List<AssistantMessage.ToolCall> maskToolCalls(List<AssistantMessage.ToolCall> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return List.of();
        }
        List<AssistantMessage.ToolCall> masked = new ArrayList<>();
        for (AssistantMessage.ToolCall toolCall : toolCalls) {
            masked.add(new AssistantMessage.ToolCall(
                    toolCall.id(),
                    toolCall.type(),
                    toolCall.name(),
                    mask(toolCall.arguments())));
        }
        return masked;
    }

    /**
     * Tool 응답에는 DB 조회 결과가 들어올 수 있으므로 응답 데이터도 마스킹 대상에 포함한다.
     */
    private List<ToolResponseMessage.ToolResponse> maskToolResponses(List<ToolResponseMessage.ToolResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            return List.of();
        }
        List<ToolResponseMessage.ToolResponse> masked = new ArrayList<>();
        for (ToolResponseMessage.ToolResponse response : responses) {
            masked.add(new ToolResponseMessage.ToolResponse(
                    response.id(),
                    response.name(),
                    mask(response.responseData())));
        }
        return masked;
    }

    private String mask(String value) {
        return sensitiveDataSanitizer.mask(value);
    }
}
