package com.mes.application.service.ai.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.application.service.ai.support.AiClientGateway;
import com.mes.application.service.ai.support.AiJsonSupport;
import com.mes.application.service.ai.support.AiTextSupport;
import com.mes.application.service.ai.support.SensitiveDataSanitizer;
import com.mes.domain.ai.dto.AreaAssessmentDto;
import com.mes.domain.ai.dto.GlobalOperationAiAnalysisResponse;
import com.mes.domain.ai.dto.GlobalOperationEvidenceDto;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class OperationAiAnalysisService {

    private static final long CACHE_TTL_MILLIS = 30_000;

    private final OperationEvidenceCollector evidenceCollector;
    private final OperationAnalysisFallbackService fallbackService;
    private final AiClientGateway aiClientGateway;
    private final ObjectMapper objectMapper;
    private final SensitiveDataSanitizer sensitiveDataSanitizer;

    private volatile GlobalOperationAiAnalysisResponse cachedResponse;
    private volatile long cachedAtMillis;

    public OperationAiAnalysisService(
            OperationEvidenceCollector evidenceCollector,
            OperationAnalysisFallbackService fallbackService,
            AiClientGateway aiClientGateway,
            ObjectMapper objectMapper,
            SensitiveDataSanitizer sensitiveDataSanitizer
    ) {
        this.evidenceCollector = evidenceCollector;
        this.fallbackService = fallbackService;
        this.aiClientGateway = aiClientGateway;
        this.objectMapper = objectMapper;
        this.sensitiveDataSanitizer = sensitiveDataSanitizer;
    }

    public GlobalOperationAiAnalysisResponse analyze() {
        return analyze(false);
    }

    public GlobalOperationAiAnalysisResponse analyze(boolean refresh) {
        GlobalOperationAiAnalysisResponse cached = cachedResponse;
        if (!refresh && cached != null && System.currentTimeMillis() - cachedAtMillis < CACHE_TTL_MILLIS) {
            return cached;
        }

        GlobalOperationAiAnalysisResponse fresh = doAnalyze();
        cachedResponse = fresh;
        cachedAtMillis = System.currentTimeMillis();
        return fresh;
    }

    /**
     * 운영 스냅샷을 수집한 뒤 AI 분석을 시도하고, 실패하면 규칙 기반 요약으로 대체한다.
     */
    private GlobalOperationAiAnalysisResponse doAnalyze() {
        GlobalOperationEvidenceDto evidence = evidenceCollector.collect();

        ChatClient.Builder builder = aiClientGateway.getBuilderOrNull();
        if (builder == null) {
            return fallbackService.create(evidence, false, "local-preview");
        }

        try {
            String content = CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            return callAi(builder, evidence);
                        } catch (Exception e) {
                            throw new java.util.concurrent.CompletionException(e);
                        }
                    })
                    .orTimeout(30, TimeUnit.SECONDS)
                    .join();
            return parseAiResponse(content, evidence);
        } catch (java.util.concurrent.CompletionException e) {
            String label = e.getCause() instanceof TimeoutException
                    ? "timeout(30s)"
                    : "fallback: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            return fallbackService.create(evidence, false, label);
        } catch (Exception e) {
            return fallbackService.create(evidence, false, "fallback: " + e.getMessage());
        }
    }

    private String callAi(ChatClient.Builder builder, GlobalOperationEvidenceDto evidence) throws Exception {
        return builder.build()
                .prompt()
                .system(OperationAnalysisPrompt.SYSTEM)
                .user(userPrompt(evidence))
                .call()
                .content();
    }

    /**
     * 모델이 반환한 JSON 문자열을 운영 브리핑 응답으로 변환하고 화면 표시용 길이를 보정한다.
     */
    private GlobalOperationAiAnalysisResponse parseAiResponse(
            String content,
            GlobalOperationEvidenceDto evidence
    ) throws Exception {
        String json = AiJsonSupport.extractJsonObject(content);
        JsonNode root = objectMapper.readTree(json);
        return new GlobalOperationAiAnalysisResponse(
                normalizeSeverity(AiJsonSupport.nodeToText(root.get("severity")), evidence),
                AiTextSupport.compactText(AiJsonSupport.nodeToText(root.get("summary")), 400),
                AiTextSupport.compactList(AiJsonSupport.nodeToList(root.get("keyIssues")), 8, 40),
                AiTextSupport.compactText(AiJsonSupport.nodeToText(root.get("inference")), 260),
                AiTextSupport.compactText(AiJsonSupport.nodeToText(root.get("productionImpact")), 160),
                AiTextSupport.compactList(AiJsonSupport.nodeToList(root.get("recommendedActions")), 6, 60),
                parseAreaAssessments(root.get("areaAssessments")),
                evidence,
                true,
                aiClientGateway.getModel()
        );
    }

    private List<AreaAssessmentDto> parseAreaAssessments(JsonNode arr) {
        List<AreaAssessmentDto> list = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (JsonNode n : arr) {
                String area = AiJsonSupport.nodeToText(n.get("area"));
                if (area == null || area.isBlank()) {
                    continue;
                }
                list.add(new AreaAssessmentDto(
                        area,
                        AiJsonSupport.nodeToText(n.get("status")),
                        AiTextSupport.compactText(AiJsonSupport.nodeToText(n.get("comment")), 60)
                ));
            }
        }
        return list;
    }

    /**
     * 심각도 값이 비어 있거나 잘못됐을 때 운영 스냅샷 기준으로 보정한다.
     */
    private String normalizeSeverity(String value, GlobalOperationEvidenceDto evidence) {
        String normalized = AiTextSupport.text(value).toUpperCase();
        if ("NORMAL".equals(normalized) || "WARNING".equals(normalized) || "CRITICAL".equals(normalized)) {
            return normalized;
        }
        if (evidence.getTransfers().getFailed() > 3) {
            return "CRITICAL";
        }
        if (evidence.getTransfers().getFailed() > 0 || !evidence.getCriticalEvents().isEmpty()) {
            return "WARNING";
        }
        return "NORMAL";
    }

    private String userPrompt(GlobalOperationEvidenceDto evidence) throws Exception {
        return "현재 공장 운영 스냅샷 데이터:\n" + sensitiveDataSanitizer.mask(objectMapper.writeValueAsString(evidence));
    }
}
