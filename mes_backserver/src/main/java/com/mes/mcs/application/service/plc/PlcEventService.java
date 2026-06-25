package com.mes.mcs.application.service.plc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.mcs.application.service.route.RouteService;
import com.mes.mcs.application.service.transfer.TransferService;
import com.mes.mcs.domain.plc.dto.PlcEventDto;
import com.mes.mcs.domain.plc.dto.PlcEventRequest;
import com.mes.mcs.domain.plc.dto.PlcEventSearchDto;
import com.mes.mcs.domain.plc.event.PlcEventProcessedEvent;
import com.mes.global.common.dto.PageResponse;
import com.mes.global.exception.BusinessException;
import com.mes.global.exception.ErrorCode;
import com.mes.mcs.infra.persistence.mybatis.mapper.plc.PlcEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service("mcsPlcEventService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlcEventService {

    private final PlcEventMapper plcEventMapper;
    private final TransferService transferService;
    private final RouteService routeService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public PageResponse<PlcEventDto> getEventList(PlcEventSearchDto searchDto) {
        var list = plcEventMapper.selectPlcEventList(searchDto);
        long total = plcEventMapper.selectPlcEventCount(searchDto);
        return PageResponse.createPagedResponse(list, Math.toIntExact(total), searchDto);
    }

    @Transactional
    public PlcEventDto receiveEvent(PlcEventRequest request) {
        PlcEventDto event = toEventDto(request);
        plcEventMapper.insertPlcEvent(event);

        try {
            PayloadValidation validation = validatePayload(request, event);
            if (!validation.valid()) {
                plcEventMapper.updateProcessResult(event.getEventId(), "VALIDATION_FAILED", validation.message());
            } else {
                processEvent(event);
                plcEventMapper.updateProcessResult(event.getEventId(), "SUCCESS", "processed");
            }
        } catch (RuntimeException e) {
            plcEventMapper.updateProcessResult(event.getEventId(), "FAILED", e.getMessage());
        }

        // PLC 이벤트 저장/처리 트랜잭션이 끝난 뒤 알림 리스너가 이 eventId로 알림을 생성한다.
        // 알림 생성은 별도 관심사라 이 서비스에서 직접 메일/SSE를 호출하지 않는다.
        eventPublisher.publishEvent(new PlcEventProcessedEvent(event.getEventId()));
        return event;
    }

    private void processEvent(PlcEventDto event) {
        if ("ROUTE_EDGE".equals(event.getTargetType()) || "EDGE".equals(event.getTargetType())) {
            processRouteEdgeEvent(event);
            return;
        }

        if (!"TRANSFER".equals(event.getTargetType())) {
            return;
        }
        if (event.getTargetId() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "targetId is required for transfer events.");
        }

        switch (event.getEventType()) {
            case "TRANSFER_STARTED" ->
                    transferService.changeOrderStatus(event.getTargetId(), "IN_PROGRESS", "PLC");
            case "TRANSFER_COMPLETED" ->
                    transferService.changeOrderStatus(event.getTargetId(), "COMPLETED", "PLC");
            case "EQUIPMENT_RUNNING" -> {
                // Logged only. Status does not change.
            }
            case "EQUIPMENT_ERROR", "ARRIVED_WRONG_LOCATION", "INTERLOCK_BLOCKED" ->
                    transferService.markOrderFailed(event.getTargetId(), event.getEventMessage(), "PLC");
            default ->
                    throw new BusinessException(ErrorCode.INVALID_INPUT, "Unsupported PLC event type: " + event.getEventType());
        }
    }

    private void processRouteEdgeEvent(PlcEventDto event) {
        if (event.getTargetId() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "targetId is required for route edge events.");
        }

        String edgeStatus = switch (event.getEventType()) {
            case "EDGE_BLOCKED" -> "BLOCKED";
            case "EDGE_RELEASED" -> "AVAILABLE";
            case "EDGE_CONGESTED" -> "CONGESTED";
            case "EDGE_INTERLOCKED" -> "INTERLOCKED";
            case "EDGE_MAINTENANCE" -> "MAINTENANCE";
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT, "Unsupported route edge event type: " + event.getEventType());
        };
        routeService.changeEdgeStatus(event.getTargetId(), edgeStatus, "PLC");
    }

    private PlcEventDto toEventDto(PlcEventRequest request) {
        return new PlcEventDto(
                nextEventId(),
                defaultText(request.getEquipmentCd(), ""),
                defaultText(request.getEventType(), ""),
                defaultText(request.getEventStatus(), "NORMAL"),
                defaultText(request.getTargetType(), "TRANSFER"),
                request.getTargetId(),
                defaultText(request.getLocationCd(), ""),
                defaultText(request.getErrorCode(), ""),
                defaultText(request.getMessage(), ""),
                toRawPayload(request),
                request.getEventDtm() == null ? LocalDateTime.now() : request.getEventDtm(),
                "N",
                null,
                null,
                null,
                "PLC",
                null
        );
    }

    private PayloadValidation validatePayload(PlcEventRequest request, PlcEventDto event) {
        List<String> missingFields = new ArrayList<>();
        requireText(missingFields, event.getEquipmentCd(), "equipmentCd");
        requireText(missingFields, event.getEventType(), "eventType");

        if ("TRANSFER".equals(event.getTargetType())) {
            requireId(missingFields, event.getTargetId(), "targetId");
        }

        switch (event.getEventType()) {
            case "TRANSFER_STARTED" -> {
                requireText(missingFields, event.getLocationCd(), "locationCd");
                requireText(missingFields, request.getToLocationCd(), "toLocationCd");
                requireText(missingFields, request.getLotNo(), "lotNo");
            }
            case "TRANSFER_COMPLETED" -> {
                requireText(missingFields, event.getLocationCd(), "locationCd");
                requireText(missingFields, request.getLotNo(), "lotNo");
            }
            case "EQUIPMENT_ERROR", "ARRIVED_WRONG_LOCATION", "INTERLOCK_BLOCKED" -> {
                requireText(missingFields, event.getErrorCode(), "errorCode");
                requireText(missingFields, event.getEventMessage(), "message");
            }
            default -> {
                // 지원하지 않는 이벤트 타입은 기본 필수값 확인 후 processEvent에서 명확한 예외로 처리한다.
            }
        }

        if (missingFields.isEmpty()) {
            return new PayloadValidation(true, "OK");
        }

        return new PayloadValidation(
                false,
                "PLC payload validation failed. missingFields=" + String.join(",", missingFields)
                        + ", eventType=" + defaultText(event.getEventType(), "-")
                        + ". Check PLC payload mapping or communication definition."
        );
    }

    private void requireText(List<String> missingFields, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            missingFields.add(fieldName);
        }
    }

    private void requireId(List<String> missingFields, Long value, String fieldName) {
        if (value == null) {
            missingFields.add(fieldName);
        }
    }

    private String toRawPayload(PlcEventRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String defaultText(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private Long nextEventId() {
        return System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(1000);
    }

    private record PayloadValidation(boolean valid, String message) {
    }
}
