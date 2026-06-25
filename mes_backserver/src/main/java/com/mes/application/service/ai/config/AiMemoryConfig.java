package com.mes.application.service.ai.config;

import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 대화 기억 기능을 설정한다.
 *
 * <p>
 * MessageChatMemoryAdvisor는 직접 만든 Advisor가 아니라 Spring AI 제공 기능이다.
 * 그래서 구현 클래스는 ai.advisor가 아니라, Bean 조립 책임을 가진 config 패키지에 둔다.
 * </p>
 */
@Configuration
public class AiMemoryConfig {

    private static final int MAX_CHAT_MEMORY_MESSAGES = 12;

    /**
     * conversationId별 최근 대화 메시지를 메모리에 보관한다.
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(MAX_CHAT_MEMORY_MESSAGES)
                .build();
    }

    /**
     * ChatMemory를 ChatClient 요청에 붙여 후속 질문이 이전 대화 맥락을 참고하게 한다.
     */
    @Bean
    public MessageChatMemoryAdvisor chatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }
}
