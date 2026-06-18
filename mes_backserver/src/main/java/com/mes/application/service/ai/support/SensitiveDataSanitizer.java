package com.mes.application.service.ai.support;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class SensitiveDataSanitizer {

    private final List<MaskRule> rules = List.of(
            new MaskRule(Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b"), "[EMAIL]"),
            new MaskRule(Pattern.compile("\\b01[016789]-?\\d{3,4}-?\\d{4}\\b"), "[PHONE]"),
            new MaskRule(Pattern.compile("\\b\\d{2,3}-\\d{3,4}-\\d{4}\\b"), "[PHONE]"),
            new MaskRule(Pattern.compile("\\b\\d{6}-[1-4]\\d{6}\\b"), "[RRN]"),
            new MaskRule(Pattern.compile("(?i)\\b(Bearer\\s+)[A-Za-z0-9._~+/=-]{20,}\\b"), "$1[TOKEN]"),
            new MaskRule(Pattern.compile("(?i)\\b(sk-[A-Za-z0-9_-]{20,})\\b"), "[API_KEY]"),
            new MaskRule(Pattern.compile("(?i)\\b(AIza[0-9A-Za-z_-]{20,})\\b"), "[API_KEY]"),
            new MaskRule(Pattern.compile("(?i)\\b(api[-_ ]?key|access[-_ ]?token|secret[-_ ]?key|client[-_ ]?secret)\\s*[:=]\\s*[^\\s,;\"']+"), "$1=[SECRET]"),
            new MaskRule(Pattern.compile("(?i)\\b(password|passwd|pwd)\\s*[:=]\\s*[^\\s,;\"']+"), "$1=[SECRET]"),
            new MaskRule(Pattern.compile("(?i)(jdbc:[^\\s\"']*://[^\\s\"']*?)(:[^:@/\\s\"']+)(@)"), "$1:[SECRET]$3")
    );

    public String mask(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        String masked = value;
        for (MaskRule rule : rules) {
            masked = rule.pattern().matcher(masked).replaceAll(rule.replacement());
        }
        return masked;
    }

    private record MaskRule(Pattern pattern, String replacement) {
    }
}
