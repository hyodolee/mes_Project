package com.mes.application.service.ai.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 브라우저와 SSE(Server-Sent Events) 연결을 유지하고 새 알림 신호를 push한다.
 *
 * <p>
 * 실제 알림 목록 데이터는 REST API로 다시 조회하고,
 * SSE는 "새 알림이 생겼다"는 가벼운 신호만 보내는 역할이다.
 * </p>
 */
@Service
public class SseEmitterService {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterService.class);
    private static final long TIMEOUT = 30 * 60 * 1000L; // 30분

    // 여러 사용자가 동시에 접속할 수 있으므로 순회 중 제거가 안전한 CopyOnWriteArrayList를 사용한다.
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * 프론트가 알림 스트림에 접속할 때 호출된다.
     */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            try {
                emitter.complete();
            } catch (Exception ignored) {
                // The async request may already be closed by the container.
            }
        });
        emitter.onError(e -> emitters.remove(emitter));

        // 연결 직후 ping (연결 확인용)
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    /**
     * 새 알림이 저장됐음을 모든 접속 브라우저에 알려준다.
     */
    public void pushNewNotification() {
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("notification").data("new"));
            } catch (Exception e) {
                // 브라우저가 연결을 끊은 경우(IOException) 또는 이미 완료된 emitter(IllegalStateException) 등.
                // 죽은 연결이므로 목록에서 제거한다. (Tomcat 비동기 쓰기 실패 로그는 컨테이너가 별도로 남길 수 있음)
                emitters.remove(emitter);
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                    // 이미 종료된 경우 무시
                }
                log.debug("SSE emitter 제거 (전송 실패): {}", e.getMessage());
            }
        });
    }
}
