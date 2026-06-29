package com.mes.interfaces.api.dev;

import com.mes.global.response.ApiResponse;
import io.sentry.Sentry;
import io.sentry.protocol.SentryId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Sentry integration smoke-test API.
 *
 * <p>
 * This controller is disabled by default. Enable it only in local launch settings
 * with {@code SENTRY_TEST_ENDPOINT_ENABLED=true}, then call the endpoint once and
 * confirm that the test event appears in the Sentry project.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/dev/sentry-test")
@ConditionalOnProperty(name = "app.sentry.test-endpoint-enabled", havingValue = "true")
public class SentryTestApiController {

    @GetMapping
    public ApiResponse<Map<String, Object>> captureTestEvent() {
        IllegalStateException testException = new IllegalStateException("Sentry backend test event");
        SentryId eventId = Sentry.captureException(testException);

        // Send immediately so a short local smoke test can be checked in Sentry right away.
        Sentry.flush(2000);

        return ApiResponse.ok(Map.of(
                "eventId", eventId.toString(),
                "message", "Sentry test event sent",
                "sentAt", OffsetDateTime.now().toString()
        ));
    }
}
