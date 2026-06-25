package com.mes.application.service.ai.query.tools;

/**
 * 질문 1건에서 사용할 Tool 객체 묶음이다.
 *
 * <p>
 * ChatClient는 여러 Tool 객체를 받을 수 있으므로, MES/MCS/RAG Tool을 기능별로 나누고
 * 여기서 한 번에 넘길 수 있게 모은다.
 * </p>
 */
public record OperationToolSet(
        McsAiTools mcs,
        MesAiTools mes,
        OperationStatusAiTools status,
        RagAiTools rag
) {
    /**
     * Spring AI ChatClient의 tools(...) 메서드에 전달할 배열로 변환한다.
     */
    public Object[] asArray() {
        return new Object[] {mcs, mes, status, rag};
    }
}
