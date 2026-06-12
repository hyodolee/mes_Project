package com.mes.application.service.ai.query;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 운영 문서 근거 검색.
 *
 * <p>1차 구현은 로컬 Markdown 문서를 키워드로 검색한다.
 * 이후 VectorStore 기반 RAG로 교체할 때도 외부 계약은 유지한다.</p>
 */
@Service
public class OperationDocumentSearchService {

    private static final int MAX_RESULTS = 5;
    private static final Pattern SPLIT_PATTERN = Pattern.compile("[\\s,./:;()\\[\\]{}\"'`|]+");

    private final Path docsRoot;

    public OperationDocumentSearchService(
            @Value("${ai.rag.docs-root:../docs}") String docsRoot
    ) {
        this.docsRoot = Path.of(docsRoot).normalize();
    }

    public List<DocumentSnippet> search(String query) {
        Set<String> tokens = tokenize(query);
        if (tokens.isEmpty()) {
            return List.of();
        }

        List<DocumentSnippet> snippets = new ArrayList<>();
        for (Path path : documentPaths()) {
            snippets.addAll(searchDocument(path, tokens));
        }

        return snippets.stream()
                .sorted(Comparator.comparingInt(DocumentSnippet::getScore).reversed())
                .limit(MAX_RESULTS)
                .map(DocumentSnippet::withoutScore)
                .toList();
    }

    private List<Path> documentPaths() {
        return List.of(
                docsRoot.resolve("rag/plc-mcs-communication-spec.md").normalize(),
                docsRoot.resolve("rag/plc-tag-mapping.md").normalize(),
                docsRoot.resolve("rag/plc-troubleshooting-sop.md").normalize(),
                docsRoot.resolve("design/PLC_MCS_COMMUNICATION_REFERENCE.md").normalize()
        );
    }

    private List<DocumentSnippet> searchDocument(Path path, Set<String> tokens) {
        if (!Files.exists(path)) {
            return List.of();
        }

        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            List<DocumentSnippet> results = new ArrayList<>();
            String currentSection = "";

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.startsWith("#")) {
                    currentSection = line.replaceFirst("^#+\\s*", "").trim();
                }

                int score = score(line, tokens);
                if (score <= 0) {
                    continue;
                }

                String snippet = buildSnippet(lines, i);
                results.add(new DocumentSnippet(
                        path.getFileName().toString(),
                        currentSection,
                        i + 1,
                        compact(snippet, 700),
                        score
                ));
            }
            return results;
        } catch (IOException e) {
            return List.of();
        }
    }

    private Set<String> tokenize(String query) {
        Set<String> tokens = new LinkedHashSet<>();
        if (query == null || query.isBlank()) {
            return tokens;
        }

        for (String token : SPLIT_PATTERN.split(query.toLowerCase(Locale.ROOT))) {
            if (token.length() >= 2) {
                tokens.add(token);
            }
        }

        addDomainSynonyms(tokens);
        return tokens;
    }

    private void addDomainSynonyms(Set<String> tokens) {
        if (tokens.contains("도착") || tokens.contains("도착지")) {
            tokens.add("tolocationcd");
            tokens.add("to_location_cd");
        }
        if (tokens.contains("로트") || tokens.contains("lot")) {
            tokens.add("lotno");
            tokens.add("lot_no");
        }
        if (tokens.contains("이동") || tokens.contains("반송")) {
            tokens.add("transfer");
        }
        if (tokens.contains("설비") || tokens.contains("장비")) {
            tokens.add("equipment");
        }
        if (tokens.contains("인터락")) {
            tokens.add("interlock");
        }
        if (tokens.contains("오류") || tokens.contains("에러")) {
            tokens.add("error");
        }
    }

    private int score(String line, Set<String> tokens) {
        String normalized = line.toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "");
        int score = 0;
        for (String token : tokens) {
            String normalizedToken = token.toLowerCase(Locale.ROOT)
                    .replace("_", "")
                    .replace("-", "");
            if (normalized.contains(normalizedToken)) {
                score += normalizedToken.length() >= 6 ? 3 : 1;
            }
        }
        return score;
    }

    private String buildSnippet(List<String> lines, int index) {
        int from = Math.max(0, index - 2);
        int to = Math.min(lines.size(), index + 3);
        return String.join("\n", lines.subList(from, to));
    }

    private String compact(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    @Getter
    @AllArgsConstructor
    public static class DocumentSnippet {
        private String document;
        private String section;
        private int line;
        private String snippet;
        private int score;

        private DocumentSnippet withoutScore() {
            return new DocumentSnippet(document, section, line, snippet, 0);
        }
    }
}
