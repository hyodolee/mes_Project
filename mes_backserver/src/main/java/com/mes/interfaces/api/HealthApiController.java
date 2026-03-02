package com.mes.interfaces.api;

import com.mes.global.exception.BusinessException;
import com.mes.global.exception.ErrorCode;
import com.mes.global.response.ApiResponse;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HealthApiController {

    @GetMapping("/ping")
    public ApiResponse<Map<String, Object>> ping() {
        return ApiResponse.ok(Map.of(
                "status", "ok",
                "service", "mes_backserver",
                "timestamp", LocalDateTime.now().toString()));
    }

    @GetMapping("/ping/error")
    public ApiResponse<Void> errorSample() {
        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "샘플 비즈니스 예외입니다.");
    }
}
