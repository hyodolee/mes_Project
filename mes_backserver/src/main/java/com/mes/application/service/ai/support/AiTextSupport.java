package com.mes.application.service.ai.support;

import java.util.List;

/**
 * 모델 응답을 화면에 올리기 전 정리하는 문자열 유틸리티.
 */
public final class AiTextSupport {

    private AiTextSupport() {
    }

    public static String text(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    public static String compactText(String value, int maxLength) {
        String normalized = text(value).replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength).trim() + "...";
    }

    public static String compactText(String value, int maxLength, String fallback) {
        String normalized = text(value).replaceAll("\\s+", " ").trim();
        if ("-".equals(normalized)) {
            normalized = fallback;
        }
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength).trim() + "...";
    }

    public static List<String> safeList(List<String> value) {
        return value == null ? List.of() : value;
    }

    public static List<String> compactList(List<String> value, int maxCount, int maxLength) {
        return safeList(value).stream()
                .map(item -> compactText(item, maxLength))
                .filter(item -> !"-".equals(item))
                .limit(maxCount)
                .toList();
    }
}
