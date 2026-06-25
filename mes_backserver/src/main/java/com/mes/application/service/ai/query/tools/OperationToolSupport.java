package com.mes.application.service.ai.query.tools;

import com.mes.application.service.planning.McsTransferClient;

import java.math.BigDecimal;

final class OperationToolSupport {

    private OperationToolSupport() {
    }

    static OperationToolViews.PlcEventView toPlcView(McsTransferClient.McsPlcEventSummary event) {
        return new OperationToolViews.PlcEventView(
                safe(event.getEquipmentCd()),
                safe(event.getEventType()),
                safe(event.getEventStatus()),
                safe(event.getLocationCd()),
                safe(event.getErrorCode()),
                safe(event.getEventMessage()),
                safe(event.getProcessResult()),
                safe(event.getProcessMessage()),
                safe(event.getEventDtm()));
    }

    static String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    static Double num(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    static String str(Object value) {
        return value == null ? "-" : value.toString();
    }

    static boolean hasAny(String value, String... keywords) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String upper = value.toUpperCase();
        for (String keyword : keywords) {
            if (upper.contains(keyword.toUpperCase())) {
                return true;
            }
        }
        return false;
    }
}
