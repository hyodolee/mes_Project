package com.mes.application.service.planning;

import com.mes.global.exception.BusinessException;
import com.mes.global.exception.ErrorCode;
import com.mes.mcs.application.service.plc.PlcEventService;
import com.mes.mcs.application.service.transfer.MaterialRequestService;
import com.mes.mcs.application.service.transfer.TransferService;
import com.mes.mcs.domain.plc.dto.PlcEventDto;
import com.mes.mcs.domain.plc.dto.PlcEventSearchDto;
import com.mes.mcs.domain.transfer.dto.MaterialRequestDto;
import com.mes.mcs.domain.transfer.dto.MaterialRequestResultDto;
import com.mes.mcs.domain.transfer.dto.TransferItemDto;
import com.mes.mcs.domain.transfer.dto.TransferOrderDto;
import com.mes.mcs.domain.transfer.dto.TransferSearchDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MES 업무에서 MCS 기능을 호출하는 어댑터.
 *
 * <p>예전에는 별도 MCS 서버(8081)를 HTTP로 호출했지만, 지금은 MCS 기능이 MES 서버 안으로 통합되었다.
 * 그래서 이 클래스는 기존 MES 코드가 쓰던 메서드 이름과 DTO는 유지하고, 내부에서는 통합된 MCS 서비스를 직접 호출한다.</p>
 */
@Component
public class McsTransferClient {

    private static final String SYSTEM_USER = "SYSTEM";

    private final TransferService transferService;
    private final MaterialRequestService materialRequestService;
    private final PlcEventService plcEventService;

    public McsTransferClient(
            TransferService transferService,
            MaterialRequestService materialRequestService,
            PlcEventService plcEventService
    ) {
        this.transferService = transferService;
        this.materialRequestService = materialRequestService;
        this.plcEventService = plcEventService;
    }

    public Long createTransfer(McsTransferOrderPayload payload) {
        String transferNo = payload.getTransferNo();
        if (transferNo == null || transferNo.isBlank()) {
            transferNo = "TF-" + System.currentTimeMillis();
        }

        return transferService.createTransferOrder(new TransferOrderDto(
                null,
                payload.getPlantCd(),
                transferNo,
                defaultText(payload.getTransferStatus(), "REQUESTED"),
                payload.getFromLocationId(),
                payload.getToLocationId(),
                payload.getTransferReason(),
                SYSTEM_USER,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                payload.getOptimizeRule()
        ));
    }

    public void addTransferItem(Long transferId, McsTransferItemPayload payload) {
        transferService.createTransferItem(transferId, new TransferItemDto(
                null,
                transferId,
                payload.getItemCd(),
                normalizeLotNo(payload.getLotNo()),
                payload.getTransferQty(),
                "REQUESTED",
                SYSTEM_USER,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));
    }

    public McsMaterialRequestResult createMaterialRequest(McsMaterialRequestPayload payload) {
        MaterialRequestResultDto result = materialRequestService.createMaterialRequest(new MaterialRequestDto(
                payload.getSourceSystem(),
                payload.getWoId(),
                payload.getWoNo(),
                payload.getPlantCd(),
                payload.getItemCd(),
                payload.getRequestQty(),
                payload.getWorkcenterCd(),
                payload.getOptimizeRule(),
                payload.getRequestReason()
        ));
        return toMaterialRequestResult(result);
    }

    public List<McsTransferSummary> getTransfersByWorkOrder(Long woId) {
        TransferSearchDto searchDto = new TransferSearchDto();
        searchDto.setTransferNo("MES-" + woId + "-");
        searchDto.setPage(1);
        searchDto.setSize(100);
        return transferService.getTransferList(searchDto).getContent().stream()
                .map(this::toTransferSummary)
                .toList();
    }

    public List<McsTransferSummary> getAllTransfers(int size) {
        TransferSearchDto searchDto = new TransferSearchDto();
        searchDto.setPage(1);
        searchDto.setSize(size);
        return transferService.getTransferList(searchDto).getContent().stream()
                .map(this::toTransferSummary)
                .toList();
    }

    public List<McsPlcEventSummary> getRecentPlcEvents(int size) {
        PlcEventSearchDto searchDto = new PlcEventSearchDto();
        searchDto.setPage(1);
        searchDto.setSize(size);
        return plcEventService.getEventList(searchDto).getContent().stream()
                .map(this::toPlcEventSummary)
                .toList();
    }

    public McsPlcEventSummary getPlcEvent(Long eventId) {
        PlcEventSearchDto searchDto = new PlcEventSearchDto();
        searchDto.setEventId(eventId);
        searchDto.setPage(1);
        searchDto.setSize(1);
        return plcEventService.getEventList(searchDto).getContent().stream()
                .findFirst()
                .map(this::toPlcEventSummary)
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_ERROR, "PLC event not found: " + eventId));
    }

    public List<McsPlcEventSummary> getPlcEventsByTransfer(Long transferId, int size) {
        PlcEventSearchDto searchDto = new PlcEventSearchDto();
        searchDto.setTargetType("TRANSFER");
        searchDto.setTargetId(transferId);
        searchDto.setPage(1);
        searchDto.setSize(size);
        return plcEventService.getEventList(searchDto).getContent().stream()
                .map(this::toPlcEventSummary)
                .toList();
    }

    public void cancelTransfer(Long transferId) {
        transferService.changeOrderStatus(transferId, "CANCELLED", SYSTEM_USER);
    }

    public int cancelMaterialRequestsByWorkOrder(Long woId) {
        return materialRequestService.cancelMaterialRequestsByWorkOrder(woId);
    }

    private McsMaterialRequestResult toMaterialRequestResult(MaterialRequestResultDto result) {
        if (result == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "MCS 자재 요청 생성 결과가 비어 있습니다.");
        }
        return new McsMaterialRequestResult(
                result.getTransferId(),
                result.getTransferNo(),
                result.getFromLocationId(),
                result.getFromLocationCd(),
                result.getToLocationId(),
                result.getToLocationCd(),
                result.getItemCd(),
                result.getLotNo(),
                result.getTransferQty(),
                result.getOptimizeRule()
        );
    }

    private McsTransferSummary toTransferSummary(TransferOrderDto dto) {
        return new McsTransferSummary(
                dto.getTransferId(),
                dto.getTransferNo(),
                dto.getTransferStatus(),
                dto.getTransferStatusNm(),
                dto.getFromLocationCd(),
                dto.getToLocationCd(),
                dto.getOptimizeRule(),
                format(dto.getRegDtm()),
                format(dto.getUpdDtm())
        );
    }

    private McsPlcEventSummary toPlcEventSummary(PlcEventDto dto) {
        return new McsPlcEventSummary(
                dto.getEventId(),
                dto.getEquipmentCd(),
                dto.getEventType(),
                dto.getEventStatus(),
                dto.getTargetType(),
                dto.getTargetId(),
                dto.getLocationCd(),
                dto.getErrorCode(),
                dto.getEventMessage(),
                dto.getRawPayload(),
                format(dto.getEventDtm()),
                dto.getProcessedYn(),
                dto.getProcessResult(),
                dto.getProcessMessage(),
                format(dto.getProcessedDtm()),
                dto.getRegUserId(),
                format(dto.getRegDtm())
        );
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String normalizeLotNo(String lotNo) {
        return lotNo == null ? "" : lotNo.trim();
    }

    private String format(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    @lombok.Getter @lombok.Setter @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class McsTransferOrderPayload {
        private String plantCd;
        private String transferNo;
        private String transferStatus;
        private Long fromLocationId;
        private Long toLocationId;
        private String transferReason;
        private String optimizeRule;
    }

    @lombok.Getter @lombok.Setter @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class McsTransferItemPayload {
        private String itemCd;
        private String lotNo;
        private Double transferQty;
    }

    @lombok.Getter @lombok.Setter @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class McsMaterialRequestPayload {
        private String sourceSystem;
        private Long woId;
        private String woNo;
        private String plantCd;
        private String itemCd;
        private Double requestQty;
        private String workcenterCd;
        private String optimizeRule;
        private String requestReason;
    }

    @lombok.Getter @lombok.Setter @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class McsMaterialRequestResult {
        private Long transferId;
        private String transferNo;
        private Long fromLocationId;
        private String fromLocationCd;
        private Long toLocationId;
        private String toLocationCd;
        private String itemCd;
        private String lotNo;
        private Double transferQty;
        private String optimizeRule;
    }

    @lombok.Getter @lombok.Setter @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class McsTransferSummary {
        private Long transferId;
        private String transferNo;
        private String transferStatus;
        private String transferStatusNm;
        private String fromLocationCd;
        private String toLocationCd;
        private String optimizeRule;
        private String regDtm;
        private String updDtm;
    }

    @lombok.Getter @lombok.Setter @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class McsPlcEventSummary {
        private Long eventId;
        private String equipmentCd;
        private String eventType;
        private String eventStatus;
        private String targetType;
        private Long targetId;
        private String locationCd;
        private String errorCode;
        private String eventMessage;
        private String rawPayload;
        private String eventDtm;
        private String processedYn;
        private String processResult;
        private String processMessage;
        private String processedDtm;
        private String regUserId;
        private String regDtm;
    }
}
