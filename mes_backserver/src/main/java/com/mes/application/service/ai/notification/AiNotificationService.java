package com.mes.application.service.ai.notification;

import com.mes.application.service.ai.support.AiClientGateway;
import com.mes.application.service.ai.support.AiTextSupport;
import com.mes.domain.ai.dto.AiNotificationDto;
import com.mes.infra.persistence.mybatis.mapper.ai.AiNotificationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AiNotificationService.class);

    private static final String ALERT_PROMPT = """
            다음 PLC 설비 이벤트 정보를 보고, 현장 담당자에게 보낼 짧은 알림을 작성하세요.

            규칙:
            - 제목(title): 15자 이내, 무슨 설비에서 무슨 문제인지 한 줄
            - 내용(message): 2문장 이내, 상황과 권고 조치
            - 심각도(severity): ERROR(즉시조치) / WARNING(주의) / INFO(참고) 중 하나
            - 기술 코드(예: MOTOR_OVERLOAD)는 한국어로 풀어 쓰세요
            - 반드시 JSON 형식으로만 응답하세요:
              {"title":"...", "message":"...", "severity":"..."}

            이벤트 정보:
            """;

    private final AiNotificationMapper mapper;
    private final SseEmitterService sseEmitterService;
    private final com.mes.application.service.planning.McsTransferClient mcsTransferClient;
    private final AiClientGateway aiClientGateway;

    public AiNotificationService(
            AiNotificationMapper mapper,
            SseEmitterService sseEmitterService,
            com.mes.application.service.planning.McsTransferClient mcsTransferClient,
            AiClientGateway aiClientGateway
    ) {
        this.mapper = mapper;
        this.sseEmitterService = sseEmitterService;
        this.mcsTransferClient = mcsTransferClient;
        this.aiClientGateway = aiClientGateway;
    }

    @Scheduled(fixedDelay = 60_000)
    public void detectAndNotify() {
        try {
            List<com.mes.application.service.planning.McsTransferClient.McsPlcEventSummary> events =
                    mcsTransferClient.getRecentPlcEvents(20);

            events.stream()
                    .filter(this::isAlertTarget)
                    .filter(e -> mapper.countBySourceRef("PLC_EVENT#" + e.getEventId()) == 0)
                    .forEach(this::createNotification);

        } catch (Exception e) {
            log.debug("PLC 이벤트 조회 실패 (MCS 미연결): {}", e.getMessage());
        }
    }

    /**
     * 최근 PLC 이벤트 중 운영자 알림으로 만들 이벤트인지 판단한다.
     */
    private boolean isAlertTarget(com.mes.application.service.planning.McsTransferClient.McsPlcEventSummary event) {
        String eventType = AiTextSupport.text(event.getEventType()).toUpperCase();
        String eventStatus = AiTextSupport.text(event.getEventStatus()).toUpperCase();
        String processResult = AiTextSupport.text(event.getProcessResult()).toUpperCase();
        return "VALIDATION_FAILED".equals(processResult)
                || "ERROR".equals(eventStatus)
                || eventType.contains("ERROR")
                || eventType.contains("INTERLOCK")
                || eventType.contains("FAILED")
                || eventType.contains("TIMEOUT")
                || eventType.contains("MISMATCH")
                || eventType.contains("WRONG_LOCATION");
    }

    /**
     * PLC 이벤트 1건을 알림 문구로 변환해 저장하고 실시간 화면에 전파한다.
     */
    private void createNotification(com.mes.application.service.planning.McsTransferClient.McsPlcEventSummary event) {
        try {
            String sourceRef = "PLC_EVENT#" + event.getEventId();
            String eventInfo = String.format("설비: %s, 유형: %s, 오류코드: %s, 메시지: %s",
                    event.getEquipmentCd(), event.getEventType(), event.getErrorCode(), event.getEventMessage());

            String title;
            String message;
            String severity;

            ChatClient.Builder builder = aiClientGateway.getBuilderOrNull();
            if (builder != null) {
                String json = builder.build()
                        .prompt()
                        .user(ALERT_PROMPT + eventInfo)
                        .call()
                        .content();
                var parsed = parseJson(json);
                title = parsed[0];
                message = parsed[1];
                severity = parsed[2];
            } else {
                title = event.getEquipmentCd() + " 오류 발생";
                message = event.getEquipmentCd() + " 설비에서 " + event.getEventType() + " 이벤트가 발생했습니다. 확인이 필요합니다.";
                severity = defaultSeverity(event.getEventType());
            }

            title = AiTextSupport.compactText(title, 30, "설비 알림");
            message = AiTextSupport.compactText(message, 160, "설비 이상이 감지되었습니다.");
            severity = normalizeSeverity(severity, event.getEventType());

            AiNotificationDto dto = new AiNotificationDto(null, title, message, severity, sourceRef, false, null);
            mapper.insert(dto);
            sseEmitterService.pushNewNotification();
            log.info("AI 알림 생성: [{}] {}", severity, title);

        } catch (Exception e) {
            log.warn("알림 생성 실패 (eventId={}): {}", event.getEventId(), e.getMessage(), e);
        }
    }

    /**
     * 모델이 만든 알림 JSON을 제목, 내용, 심각도 값으로 변환한다.
     */
    private String[] parseJson(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new String[]{"설비 알림", "설비 이상이 감지되었습니다.", "WARNING"};
            }
            String cleaned = json.replaceAll("```json|```", "").trim();
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start >= 0 && end >= start) {
                cleaned = cleaned.substring(start, end + 1);
            }
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = om.readTree(cleaned);
            return new String[]{
                    node.path("title").asText("알림"),
                    node.path("message").asText("설비 이상이 감지되었습니다."),
                    node.path("severity").asText("WARNING")
            };
        } catch (Exception e) {
            return new String[]{"설비 알림", AiTextSupport.compactText(json, 160, "설비 이상이 감지되었습니다."), "WARNING"};
        }
    }

    public List<AiNotificationDto> getRecent(int limit) {
        return mapper.findRecent(limit);
    }

    public int getUnreadCount() {
        return mapper.countUnread();
    }

    public void markAsRead(Long id) {
        mapper.markAsRead(id);
    }

    public void markAllAsRead() {
        mapper.markAllAsRead();
    }

    /**
     * 심각도 값이 비어 있거나 잘못됐을 때 이벤트 타입 기준으로 보정한다.
     */
    private String normalizeSeverity(String severity, String eventType) {
        String normalized = AiTextSupport.text(severity).toUpperCase();
        if ("ERROR".equals(normalized) || "WARNING".equals(normalized) || "INFO".equals(normalized)) {
            return normalized;
        }
        return defaultSeverity(eventType);
    }

    /**
     * 이벤트 타입만 보고 기본 알림 심각도를 계산한다.
     */
    private String defaultSeverity(String eventType) {
        String normalized = AiTextSupport.text(eventType).toUpperCase();
        if (normalized.contains("ERROR") || normalized.contains("FAILED") || normalized.contains("TIMEOUT")) {
            return "ERROR";
        }
        if (normalized.contains("INTERLOCK") || normalized.contains("MISMATCH") || normalized.contains("WRONG_LOCATION")) {
            return "WARNING";
        }
        return "INFO";
    }

}
