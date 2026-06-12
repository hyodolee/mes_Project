package com.mes.interfaces.api.ai;

import com.mes.application.service.ai.query.OperationQueryService;
import com.mes.domain.ai.dto.NaturalLanguageQueryRequest;
import com.mes.domain.ai.dto.NaturalLanguageQueryResponse;
import com.mes.global.response.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class OperationQueryApiController {

    private final OperationQueryService operationQueryService;

    public OperationQueryApiController(OperationQueryService operationQueryService) {
        this.operationQueryService = operationQueryService;
    }

    @PostMapping("/query")
    public ApiResponse<NaturalLanguageQueryResponse> query(@RequestBody NaturalLanguageQueryRequest request) {
        return ApiResponse.ok(operationQueryService.query(request));
    }

    @PostMapping(value = "/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamQuery(@RequestBody NaturalLanguageQueryRequest request) {
        return operationQueryService.streamQuery(request);
    }

    @PostMapping("/query/memory/clear")
    public ApiResponse<Void> clearMemory(@RequestBody Map<String, String> request) {
        operationQueryService.clearMemory(request.get("conversationId"));
        return ApiResponse.ok();
    }
}
