package com.mes.application.service.ai.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OperationDocumentSearchServiceTest {

    private final OperationDocumentSearchService documentSearchService =
            new OperationDocumentSearchService("../docs");

    @Test
    @DisplayName("PLC 이벤트 필드 누락 질문이면 RAG 원본 문서에서 근거를 찾는다")
    void searchTransferStartedMissingFields() {
        var results = documentSearchService.search("TRANSFER_STARTED toLocationCd lotNo 누락");

        assertThat(results).isNotEmpty();
        assertThat(results)
                .anySatisfy(result -> {
                    assertThat(result.getDocument()).isNotBlank();
                    assertThat(result.getSnippet()).containsIgnoringCase("TRANSFER_STARTED");
                });
    }

    @Test
    @DisplayName("PLC 태그명을 질문하면 태그 매핑 문서를 찾는다")
    void searchPlcTagMapping() {
        var results = documentSearchService.search("TO_LOCATION_CD LOT_NO PLC 태그");

        assertThat(results).isNotEmpty();
        assertThat(results)
                .anySatisfy(result -> assertThat(result.getDocument()).isEqualTo("plc-tag-mapping.md"));
    }

    @Test
    @DisplayName("검색어가 비어 있으면 빈 결과를 반환한다")
    void emptyQueryReturnsNoResult() {
        assertThat(documentSearchService.search("")).isEmpty();
        assertThat(documentSearchService.search(null)).isEmpty();
    }
}
