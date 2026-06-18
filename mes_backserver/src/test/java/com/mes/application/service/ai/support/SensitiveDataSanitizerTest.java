package com.mes.application.service.ai.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataSanitizerTest {

    private final SensitiveDataSanitizer sanitizer = new SensitiveDataSanitizer();

    @Test
    void masksCommonSensitiveData() {
        String input = """
                email=test@example.com phone=010-1234-5678
                rrn=900101-1234567 password=secret123
                Authorization: Bearer abcdefghijklmnopqrstuvwxyz123456
                openai=sk-test_abcdefghijklmnopqrstuvwxyz
                """;

        String masked = sanitizer.mask(input);

        assertThat(masked).contains("[EMAIL]", "[PHONE]", "[RRN]", "password=[SECRET]", "Bearer [TOKEN]", "[API_KEY]");
        assertThat(masked).doesNotContain("test@example.com", "010-1234-5678", "900101-1234567", "secret123");
    }
}
