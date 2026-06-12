package com.mes.interfaces.api.ai;

import com.mes.application.service.ai.notification.AiNotificationService;
import com.mes.application.service.ai.notification.SseEmitterService;
import com.mes.domain.ai.dto.AiNotificationDto;
import com.mes.global.response.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class AiNotificationApiController {

    private final AiNotificationService notificationService;
    private final SseEmitterService sseEmitterService;

    public AiNotificationApiController(AiNotificationService notificationService, SseEmitterService sseEmitterService) {
        this.notificationService = notificationService;
        this.sseEmitterService = sseEmitterService;
    }

    @GetMapping
    public ApiResponse<List<AiNotificationDto>> getRecent(
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        return ApiResponse.ok(notificationService.getRecent(limit));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Integer>> getUnreadCount() {
        return ApiResponse.ok(Map.of("count", notificationService.getUnreadCount()));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable("id") Long id) {
        notificationService.markAsRead(id);
        return ApiResponse.ok();
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ApiResponse.ok();
    }

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        return sseEmitterService.subscribe();
    }
}
