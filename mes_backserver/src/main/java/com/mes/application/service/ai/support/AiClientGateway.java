package com.mes.application.service.ai.support;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AI 모델 호출에 필요한 클라이언트 설정을 한 곳에서 관리한다.
 *
 * <p>각 서비스가 API 키 확인과 클라이언트 조회를 반복하지 않도록 모아 둔 클래스다.</p>
 */
@Component
public class AiClientGateway {

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final String apiKey;
    private final String model;

    public AiClientGateway(
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}") String model
    ) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.apiKey = apiKey;
        this.model = model;
    }

    public ChatClient.Builder getBuilderOrNull() {
        // API 키가 없으면 모델 호출을 시도하지 않고 기본 응답으로 대체한다.
        if (!hasApiKey()) {
            return null;
        }

        // 등록된 AI 클라이언트가 없으면 기본 응답으로 대체할 수 있도록 null을 반환한다.
        return chatClientBuilderProvider.getIfAvailable();
    }

    public String getModel() {
        return model;
    }

    private boolean hasApiKey() {
        return apiKey != null
                && !apiKey.isBlank()
                && !"missing".equals(apiKey)
                && !apiKey.startsWith("PUT_");
    }
}
