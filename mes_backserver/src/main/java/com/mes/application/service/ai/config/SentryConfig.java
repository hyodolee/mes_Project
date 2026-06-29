package com.mes.application.service.ai.config;

import com.mes.application.service.ai.support.SensitiveDataSanitizer;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.Message;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Applies one last sanitizing step before events are sent to Sentry.
 *
 * <p>
 * The Sentry SDK already disables default PII collection through application.yml.
 * This hook additionally masks sensitive strings that may appear in exception
 * messages, logger names, or transaction names.
 * </p>
 */
@Configuration
public class SentryConfig {

    @Bean
    public SentryOptions.BeforeSendCallback sentryBeforeSendCallback(
            SensitiveDataSanitizer sensitiveDataSanitizer
    ) {
        return (event, hint) -> sanitizeEvent(event, sensitiveDataSanitizer);
    }

    private SentryEvent sanitizeEvent(SentryEvent event, SensitiveDataSanitizer sensitiveDataSanitizer) {
        Message message = event.getMessage();
        if (message != null) {
            message.setMessage(mask(message.getMessage(), sensitiveDataSanitizer));
            message.setFormatted(mask(message.getFormatted(), sensitiveDataSanitizer));
        }

        event.setTransaction(mask(event.getTransaction(), sensitiveDataSanitizer));
        event.setLogger(mask(event.getLogger(), sensitiveDataSanitizer));
        return event;
    }

    private String mask(String value, SensitiveDataSanitizer sensitiveDataSanitizer) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return sensitiveDataSanitizer.mask(value);
    }
}
