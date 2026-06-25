package com.mes.application.service.ai.notification;

import com.mes.mcs.domain.plc.event.PlcEventProcessedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * PLC 이벤트 처리 완료 후 알림 생성을 시작하는 이벤트 리스너.
 *
 * <p>
 * PlcEventService는 PLC 이벤트 저장/처리까지만 담당하고,
 * 알림 생성은 이 리스너가 받아서 AiNotificationService에 위임한다.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class PlcEventNotificationListener {

    private final AiNotificationService notificationService;

    /**
     * DB 커밋 이후 비동기로 실행한다.
     *
     * <p>
     * AFTER_COMMIT: 이벤트 저장이 확정된 뒤 알림이 이벤트 상세를 다시 조회할 수 있게 한다.
     * Async: AI 요약, SSE push, 메일 발송이 PLC 이벤트 처리 응답을 지연시키지 않게 한다.
     * </p>
     */
    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PlcEventProcessedEvent event) {
        notificationService.notifyPlcEvent(event.eventId());
    }
}
