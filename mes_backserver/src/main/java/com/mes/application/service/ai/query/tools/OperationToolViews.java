package com.mes.application.service.ai.query.tools;

public final class OperationToolViews {

    private OperationToolViews() {
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class TransferView {
        private Long transferId;
        private String transferNo;
        private String status;
        private String statusNm;
        private String fromLocation;
        private String toLocation;
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class PlcEventView {
        private String equipmentCd;
        private String eventType;
        private String eventStatus;
        private String locationCd;
        private String errorCode;
        private String eventMessage;
        private String processResult;
        private String processMessage;
        private String eventDtm;
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class WorkOrderView {
        private String woNo;
        private String itemCd;
        private String itemNm;
        private Double woQty;
        private Double goodQty;
        private Double defectQty;
        private String status;
        private Integer priority;
        private String workcenterCd;
        private String equipmentCd;
        private String lotNo;
        private String planStart;
        private String planEnd;
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class ProdPlanView {
        private String planNo;
        private String itemNm;
        private Double planQty;
        private Double resultQty;
        private String status;
        private Integer priority;
        private String startDt;
        private String endDt;
        private String deliveryDt;
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class PlantView {
        private String plantCd;
        private String plantNm;
        private String companyNm;
        private String useYn;
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class ItemView {
        private String itemCd;
        private String itemNm;
        private String itemSpec;
        private String itemType;
        private String itemGrp;
        private String unit;
        private String useYn;
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class StockView {
        private String itemCd;
        private String itemNm;
        private String locationCd;
        private String lotNo;
        private Double stockQty;
        private Double availableQty;
        private Double reservedQty;
        private Double safetyStockQty;
        private Boolean belowSafety;
        private String unit;
        private String stockStatus;
        private String expireDt;
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class EquipStatusView {
        private String equipmentCd;
        private String operDt;
        private String shift;
        private String operStatus;
        private Double operTime;
        private Double prodQty;
        private String itemCd;
        private String workerId;
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class DowntimeView {
        private String equipmentCd;
        private String downtimeDt;
        private String downtimeType;
        private String downtimeCd;
        private String downtimeReason;
        private Double downtimeMin;
        private String actionContent;
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class InspectView {
        private String inspectType;
        private String itemCd;
        private String lotNo;
        private Double inspectQty;
        private Double sampleQty;
        private Double passQty;
        private Double failQty;
        private String judgeResult;
        private String inspectDt;
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class DefectView {
        private String defectDt;
        private String itemCd;
        private String defectType;
        private String defectNm;
        private Double defectQty;
        private String defectCause;
        private String defectAction;
        private String disposition;
        private String equipmentCd;
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class RouteEdgeView {
        private Long routeEdgeId;
        private String edgeCd;
        private String edgeNm;
        private String fromNodeCd;
        private String toNodeCd;
        private String edgeStatus;
        private Integer travelTimeSec;
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class TransferRouteView {
        private Long transferRouteId;
        private Long transferId;
        private String routeStatus;
        private Double totalDistanceM;
        private Integer totalTimeSec;
        private String optimizeRule;
        private Integer replanCount;
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class RouteStepView {
        private Integer stepSeq;
        private String edgeCd;
        private String fromNodeCd;
        private String toNodeCd;
        private String stepStatus;
        private String edgeStatus;
        private Integer expectedTimeSec;
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class LocationView {
        private Long locationId;
        private String locationCd;
        private String locationNm;
        private String warehouseNm;
        private String zoneNm;
        private String locationStatus;
        private Double currentUsage;
        private Double maxCapacity;
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class LocationStockView {
        private Long locStockId;
        private String locationCd;
        private String itemCd;
        private String itemNm;
        private String lotNo;
        private Double stockQty;
        private Double availableQty;
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class TransferBlockerSummary {
        private long failedTransferCount;
        private long plcFailureCount;
        private long blockedRouteCount;
    }
}
