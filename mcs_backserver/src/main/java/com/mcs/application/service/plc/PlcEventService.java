package com.mcs.application.service.plc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcs.application.service.route.RouteService;
import com.mcs.application.service.transfer.TransferService;
import com.mcs.domain.plc.dto.PlcEventDto;
import com.mcs.domain.plc.dto.PlcEventRequest;
import com.mcs.domain.plc.dto.PlcEventSearchDto;
import com.mcs.global.common.dto.PageResponse;
import com.mcs.global.exception.BusinessException;
import com.mcs.global.exception.ErrorCode;
import com.mcs.infra.persistence.mybatis.mapper.plc.PlcEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlcEventService {

    private final PlcEventMapper plcEventMapper;
    private final TransferService transferService;
    private final RouteService routeService;
    private final ObjectMapper objectMapper;

    public PageResponse<PlcEventDto> getEventList(PlcEventSearchDto searchDto) {
        var list = plcEventMapper.selectPlcEventList(searchDto);
        long total = plcEventMapper.selectPlcEventCount(searchDto);
        return PageResponse.createPagedResponse(list, total, searchDto);
    }

    @Transactional
    public PlcEventDto receiveEvent(PlcEventRequest request) {
        PlcEventDto event = toEventDto(request);
        plcEventMapper.insertPlcEvent(event);

        try {
            processEvent(event);
            plcEventMapper.updateProcessResult(event.eventId(), "SUCCESS", "processed");
        } catch (RuntimeException e) {
            plcEventMapper.updateProcessResult(event.eventId(), "FAILED", e.getMessage());
        }

        return event;
    }

    private void processEvent(PlcEventDto event) {
        if ("ROUTE_EDGE".equals(event.targetType()) || "EDGE".equals(event.targetType())) {
            processRouteEdgeEvent(event);
            return;
        }

        if (!"TRANSFER".equals(event.targetType())) {
            return;
        }
        if (event.targetId() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "targetId is required for transfer events.");
        }

        switch (event.eventType()) {
            case "TRANSFER_STARTED" ->
                    transferService.changeOrderStatus(event.targetId(), "IN_PROGRESS", "PLC");
            case "TRANSFER_COMPLETED" ->
                    transferService.changeOrderStatus(event.targetId(), "COMPLETED", "PLC");
            case "EQUIPMENT_RUNNING" -> {
                // Logged only. Status does not change.
            }
            case "EQUIPMENT_ERROR", "ARRIVED_WRONG_LOCATION", "INTERLOCK_BLOCKED" ->
                    transferService.markOrderFailed(event.targetId(), event.eventMessage(), "PLC");
            default ->
                    throw new BusinessException(ErrorCode.INVALID_INPUT, "Unsupported PLC event type: " + event.eventType());
        }
    }

    private void processRouteEdgeEvent(PlcEventDto event) {
        if (event.targetId() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "targetId is required for route edge events.");
        }

        String edgeStatus = switch (event.eventType()) {
            case "EDGE_BLOCKED" -> "BLOCKED";
            case "EDGE_RELEASED" -> "AVAILABLE";
            case "EDGE_CONGESTED" -> "CONGESTED";
            case "EDGE_INTERLOCKED" -> "INTERLOCKED";
            case "EDGE_MAINTENANCE" -> "MAINTENANCE";
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT, "Unsupported route edge event type: " + event.eventType());
        };
        routeService.changeEdgeStatus(event.targetId(), edgeStatus, "PLC");
    }

    private PlcEventDto toEventDto(PlcEventRequest request) {
        return new PlcEventDto(
                nextEventId(),
                required(request.equipmentCd(), "equipmentCd"),
                required(request.eventType(), "eventType"),
                defaultText(request.eventStatus(), "NORMAL"),
                defaultText(request.targetType(), "TRANSFER"),
                request.targetId(),
                defaultText(request.locationCd(), ""),
                defaultText(request.errorCode(), ""),
                defaultText(request.message(), ""),
                toRawPayload(request),
                request.eventDtm() == null ? LocalDateTime.now() : request.eventDtm(),
                "N",
                null,
                null,
                null,
                "PLC",
                null
        );
    }

    private String toRawPayload(PlcEventRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " is required.");
        }
        return value.trim();
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
}
