package com.mes.application.service.ai.support;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 모델 응답 JSON 처리 유틸리티.
 *
 * <p>LLM은 JSON만 요청해도 ```json 코드블록이나 앞뒤 설명을 섞어 보내는 경우가 있다.
 * 서비스 코드가 이런 문자열 정리 로직을 직접 가지지 않도록 이곳에 모았다.</p>
 */
public final class AiJsonSupport {

    private AiJsonSupport() {
    }

    public static String extractJsonObject(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("AI response is empty");
        }

        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
        }

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("AI response does not contain a JSON object");
        }
        return trimmed.substring(start, end + 1);
    }

    public static List<String> nodeToList(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return List.of();
        }

        if (node.isArray()) {
            List<String> values = new ArrayList<>();
            for (JsonNode item : node) {
                String value = nodeToText(item);
                if (!"-".equals(value)) {
                    values.add(value);
                }
            }
            return values;
        }

        String value = nodeToText(node);
        return "-".equals(value) ? List.of() : List.of(value);
    }

    public static String nodeToText(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return "-";
        }

        if (node.isTextual()) {
            return AiTextSupport.text(node.asText());
        }

        if (node.isNumber() || node.isBoolean()) {
            return node.asText();
        }

        if (node.isArray()) {
            List<String> values = new ArrayList<>();
            for (JsonNode item : node) {
                String value = nodeToText(item);
                if (!"-".equals(value)) {
                    values.add(value);
                }
            }
            return values.isEmpty() ? "-" : String.join("\n", values);
        }

        if (node.isObject()) {
            List<String> values = new ArrayList<>();
            node.fields().forEachRemaining(entry -> {
                String value = nodeToText(entry.getValue());
                if (!"-".equals(value)) {
                    values.add(entry.getKey() + ": " + value);
                }
            });
            return values.isEmpty() ? "-" : String.join("\n", values);
        }

        return AiTextSupport.text(node.asText());
    }
}
