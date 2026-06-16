package com.mes.application.service.ai.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseEmitterService {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterService.class);
    private static final long TIMEOUT = 30 * 60 * 1000L; // 30분

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        // 연결 직후 ping (연결 확인용)
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

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
