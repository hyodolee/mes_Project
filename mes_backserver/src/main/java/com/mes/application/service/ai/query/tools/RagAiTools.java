package com.mes.application.service.ai.query.tools;

import com.mes.application.service.ai.rag.OperationDocumentSearchService;
import com.mes.application.service.ai.rag.OperationGraphSearchService;
import com.mes.domain.ai.dto.GraphPathDto;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;

/**
 * LLM이 RAG 근거를 직접 조회할 때 사용하는 Tool 모음이다.
 *
 * <p>
 * Graph RAG는 개념 간 관계를, Document RAG는 업로드된 사내 문서 조각을 찾는다.
 * 두 도구 모두 실제 운영 이벤트 조회가 아니라 근거 검색 도구라는 점이 중요하다.
 * </p>
 */
public class RagAiTools {

    private final OperationGraphSearchService graphSearchService;
    private final OperationDocumentSearchService documentSearchService;
    private final List<String> dataPoints;

    public RagAiTools(
            OperationGraphSearchService graphSearchService,
            OperationDocumentSearchService documentSearchService,
            List<String> dataPoints
    ) {
        this.graphSearchService = graphSearchService;
        this.documentSearchService = documentSearchService;
        this.dataPoints = dataPoints;
    }

    // 관계 지식 검색: 특정 사건이 실제로 발생했다는 뜻이 아니라 확인 순서를 찾는 용도다.
    @Tool(description = "PLC 이벤트, payload 필수값, PLC 태그, SOP 조치 관계를 Neo4j 그래프로 검색한다. "
            + "목적지 누락, LOT 누락, 목적지 점유, 위치 불일치, VALIDATION_FAILED, 태그 매핑의 일반적인 확인 절차 질문에 사용한다. "
            + "이 도구 결과는 실제 발생 이벤트가 아니라 관계 지식이므로 특정 설비나 특정 이동에 발생했다고 단정하지 않는다.")
    public List<GraphPathDto> searchOperationGraph(
            @ToolParam(description = "관계 그래프에서 찾을 질문 또는 키워드. 예: 목적지 정보 누락, toLocationCd, TRANSFER_STARTED")
            String query
    ) {
        var results = graphSearchService.search(query);
        dataPoints.add("Graph RAG 관계 경로 " + results.size() + "건 조회");
        return results;
    }

    // 문서 근거 검색: PLC 정의서, 태그 매핑표, SOP처럼 업로드된 문서가 필요할 때 사용한다.
    @Tool(description = "PLC-MCS 통신 정의서, 필수 필드, PLC 태그 매핑표, 오류 조치 SOP 문서를 검색한다. "
            + "PLC 이벤트 필드 누락, 태그 매핑, 통신 규격, 오류 원인 설명 등 문서 근거가 필요할 때 사용한다.")
    public List<OperationDocumentSearchService.DocumentSnippet> searchOperationDocuments(
            @ToolParam(description = "문서에서 찾을 키워드나 질문. 예: TRANSFER_STARTED toLocationCd 누락, LOT 태그, 인터락 오류")
            String query
    ) {
        var results = documentSearchService.search(query);
        dataPoints.add("운영 문서 근거 " + results.size() + "건 조회");
        return results;
    }
}
