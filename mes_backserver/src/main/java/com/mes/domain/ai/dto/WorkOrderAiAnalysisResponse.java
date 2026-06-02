package com.mes.domain.ai.dto;

import java.util.List;

public record WorkOrderAiAnalysisResponse(
        String summary,
        List<String> facts,
        String inference,
        String impact,
        List<String> recommendedActions,
        Evidence evidence,
        boolean aiGenerated,
        String model
) {
    public record Evidence(
            WorkOrderEvidence workOrder,
            McsTransferEvidence mcsTransfer,
            List<PlcEventEvidence> plcEvents
    ) {
    }

    public record WorkOrderEvidence(
            Long woId,
            String woNo,
            String plantNm,
            String itemCd,
            String itemNm,
            String woStatus,
            String lotNo
    ) {
    }

    public record McsTransferEvidence(
            Long transferId,
            String transferNo,
            String transferStatus,
            String transferStatusNm,
            String fromLocationCd,
            String toLocationCd,
            String optimizeRule
    ) {
    }

    public record PlcEventEvidence(
            Long eventId,
            String equipmentCd,
            String eventType,
            String eventStatus,
            String errorCode,
            String eventMessage,
            String eventDtm,
            String processResult
    ) {
    }
}
