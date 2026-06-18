package com.mes.application.service.ai.query;

import com.mes.application.service.ai.support.SensitiveDataSanitizer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Chroma에 색인된 운영 문서를 검색한다.
 *
 * <p>여기서는 로컬 Markdown 파일을 fallback으로 검색하지 않는다.
 * RAG 업로드/색인 상태를 검증해야 하므로, Chroma에 문서가 없으면 빈 결과를 그대로 반환한다.</p>
 */
@Slf4j
@Service
public class OperationDocumentSearchService {

    private static final int MAX_RESULTS = 5;
    private static final int SEARCH_TOP_K = 8;

    private final VectorStore vectorStore;
    private final SensitiveDataSanitizer sensitiveDataSanitizer;

    public OperationDocumentSearchService(VectorStore vectorStore, SensitiveDataSanitizer sensitiveDataSanitizer) {
        this.vectorStore = vectorStore;
        this.sensitiveDataSanitizer = sensitiveDataSanitizer;
    }

    /**
     * Chroma에서 질문과 관련된 문서 조각을 찾는다.
     * 결과가 없으면 등록된 RAG 문서가 없거나 아직 색인되지 않은 상태로 판단한다.
     */
    public List<DocumentSnippet> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        try {
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(expandQuery(query))
                            .topK(SEARCH_TOP_K)
                            .build()
            );
            return docs.stream()
                    .map(this::toSnippet)
                    .sorted((left, right) -> Integer.compare(right.getScore(), left.getScore()))
                    .limit(MAX_RESULTS)
                    .toList();
        } catch (Exception e) {
            log.warn("[RAG] Chroma 검색 실패: {}", e.getMessage());
            return List.of();
        }
    }

    public String vectorStoreClassName() {
        return vectorStore.getClass().getName();
    }

    private DocumentSnippet toSnippet(Document doc) {
        Map<String, Object> meta = doc.getMetadata();
        Object lineObj = meta.getOrDefault("line", 0);
        int line = lineObj instanceof Number ? ((Number) lineObj).intValue() : 0;

        return new DocumentSnippet(
                String.valueOf(meta.getOrDefault("document", "unknown")),
                String.valueOf(meta.getOrDefault("section", "")),
                line,
                sensitiveDataSanitizer.mask(doc.getText()),
                score(doc)
        );
    }

    private String expandQuery(String query) {
        String normalized = normalize(query);
        StringBuilder expanded = new StringBuilder(query);

        if (normalized.contains("tolocationcd") || normalized.contains("to_location_cd") || normalized.contains("목적지")) {
            expanded.append(" TO_LOCATION_CD toLocationCd DEST_ADDR DEST_SENSOR_OK I0.3 DB120.DBX0.0");
            expanded.append(" FC_BUILD_TRANSFER_STARTED 목적지 센서 목적지 설정부 목적지 미확정");
        }
        if (normalized.contains("lotno") || normalized.contains("lot_no") || normalized.contains("로트")) {
            expanded.append(" LOT_NO lotNo LOT_REG SCAN_OK M10.0 DB120.DBX22.0 바코드 스캐너");
        }
        if (normalized.contains("transferstarted") || normalized.contains("transfer_started") || normalized.contains("이동시작")) {
            expanded.append(" TRANSFER_STARTED FC_BUILD_TRANSFER_STARTED payload JSON VALIDATION_FAILED");
        }
        if (normalized.contains("코드") || normalized.contains("어디") || normalized.contains("위치")) {
            expanded.append(" 코드 위치 함수 조건 분기 확인 순서 PLC 온라인 모니터");
        }

        return expanded.toString();
    }

    private int score(Document doc) {
        String text = normalize(doc.getText());
        String documentName = normalize(String.valueOf(doc.getMetadata().getOrDefault("document", "")));
        int score = 0;

        score += contains(text, "fc_build_transfer_started") ? 8 : 0;
        score += contains(text, "dest_sensor_ok") ? 7 : 0;
        score += contains(text, "dest_addr") ? 6 : 0;
        score += contains(text, "i03") ? 5 : 0;
        score += contains(text, "db120dbx00") ? 5 : 0;
        score += contains(text, "tolocationcd") ? 4 : 0;
        score += contains(text, "validation_failed") ? 3 : 0;
        score += contains(documentName, "cv001") ? 3 : 0;

        return score;
    }

    private boolean contains(String value, String token) {
        return value.contains(normalize(token));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(".", "");
    }

    @Getter
    @AllArgsConstructor
    public static class DocumentSnippet {
        private String document;
        private String section;
        private int line;
        private String snippet;
        private int score;
    }
}
