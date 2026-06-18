package com.mes.application.service.ai.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 운영 알림을 이메일로도 발송한다.
 *
 * <p>화면(SSE) 알림에 더해, 설정이 켜져 있고 심각도 기준을 넘는 알림을 메일로 보낸다.</p>
 *
 * <p>설계 원칙: <b>이메일은 부가 채널</b>이므로, 미설정/발송 실패가 알림 생성 자체를 막지 않는다.
 * JavaMailSender 빈이 없거나(=SMTP 미설정) enabled=false면 조용히 건너뛴다.</p>
 */
@Component
public class NotificationEmailSender {

    private static final Logger log = LoggerFactory.getLogger(NotificationEmailSender.class);

    // 심각도 순위 (높을수록 중요). 기준 이상만 발송.
    private static final List<String> SEVERITY_ORDER = List.of("INFO", "WARNING", "ERROR");

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean enabled;
    private final String from;
    private final List<String> to;
    private final String minSeverity;

    public NotificationEmailSender(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.notification.email.enabled:false}") boolean enabled,
            @Value("${app.notification.email.from:}") String from,
            @Value("${app.notification.email.to:}") String to,
            @Value("${app.notification.email.min-severity:WARNING}") String minSeverity
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.enabled = enabled;
        this.from = from == null ? "" : from.trim();
        this.to = parseRecipients(to);
        this.minSeverity = normalizeSeverity(minSeverity);
    }

    /**
     * 알림 1건을 메일로 발송한다. 발송 가능 조건이 아니면 조용히 건너뛴다.
     */
    public void send(String title, String message, String severity) {
        if (!enabled) {
            return;
        }
        if (from.isEmpty() || to.isEmpty()) {
            log.debug("[알림메일] 발신/수신 주소 미설정 — 건너뜀");
            return;
        }
        if (!meetsThreshold(severity)) {
            log.debug("[알림메일] 심각도 {} < 기준 {} — 건너뜀", severity, minSeverity);
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.debug("[알림메일] SMTP(JavaMailSender) 미설정 — 건너뜀");
            return;
        }

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(from);
            mail.setTo(to.toArray(new String[0]));
            mail.setSubject("[MES/MCS 운영알림][" + normalizeSeverity(severity) + "] " + title);
            mail.setText(message + "\n\n— MES/MCS 운영 알림 시스템");
            mailSender.send(mail);
            log.info("[알림메일] 발송 완료: {} (수신 {}명)", title, to.size());
        } catch (Exception e) {
            // 메일 실패가 알림 저장/화면push를 막지 않도록 삼킨다.
            log.warn("[알림메일] 발송 실패 ({}) — 화면 알림은 정상", e.getMessage());
        }
    }

    private boolean meetsThreshold(String severity) {
        int sev = SEVERITY_ORDER.indexOf(normalizeSeverity(severity));
        int min = SEVERITY_ORDER.indexOf(minSeverity);
        return sev >= min;
    }

    private String normalizeSeverity(String value) {
        if (value == null) {
            return "INFO";
        }
        String upper = value.trim().toUpperCase(Locale.ROOT);
        return SEVERITY_ORDER.contains(upper) ? upper : "WARNING";
    }

    private List<String> parseRecipients(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
