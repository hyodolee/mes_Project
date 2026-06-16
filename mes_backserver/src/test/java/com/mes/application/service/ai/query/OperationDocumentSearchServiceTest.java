package com.mes.application.service.ai.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OperationDocumentSearchServiceTest {

    private final VectorStore mockVectorStore = Mockito.mock(VectorStore.class);
    private final OperationDocumentSearchService documentSearchService =
            new OperationDocumentSearchService(mockVectorStore);

    @BeforeEach
    void setup() {
        Mockito.reset(mockVectorStore);
    }

    @Test
    @DisplayName("Chroma 검색 결과가 있으면 문서 조각으로 변환한다")
    void returnsChromaSearchResults() {
        Document document = new Document(
                "TRANSFER_STARTED 이벤트에서 TO_LOCATION_CD 필드를 확인합니다.",
                Map.of(
                        "document", "CV001_transfer_control.md",
                        "section", "이송 이벤트 생성",
                        "line", 12
                )
        );
        Mockito.when(mockVectorStore.similaritySearch(Mockito.any(SearchRequest.class)))
                .thenReturn(List.of(document));

        var results = documentSearchService.search("CV-001 toLocationCd 누락");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDocument()).isEqualTo("CV001_transfer_control.md");
        assertThat(results.get(0).getSection()).isEqualTo("이송 이벤트 생성");
        assertThat(results.get(0).getLine()).isEqualTo(12);
        assertThat(results.get(0).getSnippet()).contains("TO_LOCATION_CD");
    }

    @Test
    @DisplayName("Chroma 검색 결과가 없으면 fallback 없이 빈 결과를 반환한다")
    void returnsEmptyWhenChromaHasNoResult() {
        Mockito.when(mockVectorStore.similaritySearch(Mockito.any(SearchRequest.class)))
                .thenReturn(List.of());

        var results = documentSearchService.search("TRANSFER_STARTED toLocationCd lotNo 누락");

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Chroma 검색이 실패하면 빈 결과를 반환한다")
    void returnsEmptyWhenChromaSearchFails() {
        Mockito.when(mockVectorStore.similaritySearch(Mockito.any(SearchRequest.class)))
                .thenThrow(new IllegalStateException("Chroma connection failed"));

        var results = documentSearchService.search("DB120.STRING20 TO_LOCATION_CD LOT_NO");

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("검색어가 비어 있으면 Chroma를 호출하지 않고 빈 결과를 반환한다")
    void emptyQueryReturnsNoResult() {
        assertThat(documentSearchService.search("")).isEmpty();
        assertThat(documentSearchService.search(null)).isEmpty();
        Mockito.verifyNoInteractions(mockVectorStore);
    }
}
