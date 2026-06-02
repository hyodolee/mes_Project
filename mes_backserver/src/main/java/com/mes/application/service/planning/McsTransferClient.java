package com.mes.application.service.planning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.global.exception.BusinessException;
import com.mes.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
public class McsTransferClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public McsTransferClient(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${mcs.api.base-url:http://127.0.0.1:8081}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    public Long createTransfer(McsTransferOrderPayload payload) {
        try {
            ApiResponse<Long> response = restClient.post()
                    .uri("/api/transfers")
                    .body(payload)
                    .retrieve()
                    .body(TransferIdResponse.class);
            if (response == null || !response.success() || response.data() == null) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                        response == null ? "MCS 이동 오더 생성에 실패했습니다." : response.message());
            }
            return response.data();
        } catch (RestClientResponseException e) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "MCS 이동 오더 생성 실패: " + extractMessage(e));
        } catch (ResourceAccessException e) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "MCS 서버에 연결할 수 없습니다. MCS 백엔드(8081)가 실행 중인지 확인하세요.");
        }
    }

    public void addTransferItem(Long transferId, McsTransferItemPayload payload) {
        try {
            ApiResponse<Void> response = restClient.post()
                    .uri("/api/transfers/{transferId}/items", transferId)
                    .body(payload)
                    .retrieve()
                    .body(VoidResponse.class);
            if (response != null && !response.success()) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, response.message());
            }
        } catch (RestClientResponseException e) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "MCS 이동 품목 추가 실패: " + extractMessage(e));
        } catch (ResourceAccessException e) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "MCS 서버에 연결할 수 없습니다. MCS 백엔드(8081)가 실행 중인지 확인하세요.");
        }
    }

    public McsMaterialRequestResult createMaterialRequest(McsMaterialRequestPayload payload) {
        try {
            ApiResponse<McsMaterialRequestResult> response = restClient.post()
                    .uri("/api/material-requests")
                    .body(payload)
                    .retrieve()
                    .body(MaterialRequestResponse.class);
            if (response == null || !response.success() || response.data() == null) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                        response == null ? "MCS 자재 요청 생성에 실패했습니다." : response.message());
            }
            return response.data();
        } catch (RestClientResponseException e) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "MCS 자재 요청 생성 실패: " + extractMessage(e));
        } catch (ResourceAccessException e) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "MCS 서버에 연결할 수 없습니다. MCS 백엔드(8081)가 실행 중인지 확인하세요.");
        }
    }

    public List<McsTransferSummary> getTransfersByWorkOrder(Long woId) {
        try {
            ApiResponse<TransferPage> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/transfers")
                            .queryParam("transferNo", "MES-" + woId + "-")
                            .queryParam("page", 1)
                            .queryParam("size", 100)
                            .build())
                    .retrieve()
                    .body(TransferListResponse.class);
            if (response == null || !response.success() || response.data() == null) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                        response == null ? "MCS 이동 오더 조회에 실패했습니다." : response.message());
            }
            return response.data().content() == null ? List.of() : response.data().content();
        } catch (RestClientResponseException e) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "MCS 이동 오더 조회 실패: " + extractMessage(e));
        } catch (ResourceAccessException e) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "MCS 서버에 연결할 수 없습니다. MCS 백엔드(8081)가 실행 중인지 확인하세요.");
        }
    }

    public List<McsPlcEventSummary> getPlcEventsByTransfer(Long transferId, int size) {
        try {
            ApiResponse<PlcEventPage> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/plc/events")
                            .queryParam("targetType", "TRANSFER")
                            .queryParam("targetId", transferId)
                            .queryParam("page", 1)
                            .queryParam("size", size)
                            .build())
                    .retrieve()
                    .body(PlcEventListResponse.class);
            if (response == null || !response.success() || response.data() == null) {
                return List.of();
            }
            return response.data().content() == null ? List.of() : response.data().content();
        } catch (RestClientResponseException | ResourceAccessException e) {
            return List.of();
        }
    }

    public void cancelTransfer(Long transferId) {
        try {
            ApiResponse<Void> response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/transfers/{transferId}/status")
                            .queryParam("status", "CANCELLED")
                            .build(transferId))
                    .retrieve()
                    .body(VoidResponse.class);
            if (response != null && !response.success()) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, response.message());
            }
        } catch (RestClientResponseException e) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "MCS 이동 오더 취소 실패: " + extractMessage(e));
        } catch (ResourceAccessException e) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "MCS 서버에 연결할 수 없습니다. MCS 백엔드(8081)가 실행 중인지 확인하세요.");
        }
    }

    public int cancelMaterialRequestsByWorkOrder(Long woId) {
        try {
            ApiResponse<Integer> response = restClient.post()
                    .uri("/api/material-requests/work-orders/{woId}/cancel", woId)
                    .retrieve()
                    .body(CancelMaterialRequestResponse.class);
            if (response == null || !response.success() || response.data() == null) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                        response == null ? "MCS 자재 이동 요청 취소에 실패했습니다." : response.message());
            }
            return response.data();
        } catch (RestClientResponseException e) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "MCS 자재 이동 요청 취소 실패: " + extractMessage(e));
        } catch (ResourceAccessException e) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "MCS 서버에 연결할 수 없습니다. MCS 백엔드(8081)가 실행 중인지 확인하세요.");
        }
    }

    private String extractMessage(RestClientResponseException e) {
        try {
            JsonNode root = objectMapper.readTree(e.getResponseBodyAsString());
            JsonNode message = root.get("message");
            if (message != null && !message.asText().isBlank()) {
                return message.asText();
            }
        } catch (Exception ignored) {
            // Fall through to the HTTP status text below.
        }
        return e.getStatusCode() + " " + e.getStatusText();
    }

    public record McsTransferOrderPayload(
            String plantCd,
            String transferNo,
            String transferStatus,
            Long fromLocationId,
            Long toLocationId,
            String transferReason,
            String optimizeRule
    ) {
    }

    public record McsTransferItemPayload(
            String itemCd,
            String lotNo,
            Double transferQty
    ) {
    }

    public record McsMaterialRequestPayload(
            String sourceSystem,
            Long woId,
            String woNo,
            String plantCd,
            String itemCd,
            Double requestQty,
            String workcenterCd,
            String optimizeRule,
            String requestReason
    ) {
    }

    public record McsMaterialRequestResult(
            Long transferId,
            String transferNo,
            Long fromLocationId,
            String fromLocationCd,
            Long toLocationId,
            String toLocationCd,
            String itemCd,
            String lotNo,
            Double transferQty,
            String optimizeRule
    ) {
    }

    public record McsTransferSummary(
            Long transferId,
            String transferNo,
            String transferStatus,
            String transferStatusNm,
            String fromLocationCd,
            String toLocationCd,
            String optimizeRule
    ) {
    }

    public record McsPlcEventSummary(
            Long eventId,
            String equipmentCd,
            String eventType,
            String eventStatus,
            String targetType,
            Long targetId,
            String locationCd,
            String errorCode,
            String eventMessage,
            String rawPayload,
            String eventDtm,
            String processedYn,
            String processResult,
            String processMessage,
            String processedDtm,
            String regUserId,
            String regDtm
    ) {
    }

    private interface ApiResponse<T> {
        boolean success();
        String message();
        T data();
    }

    private record TransferIdResponse(boolean success, String code, String message, Long data) implements ApiResponse<Long> {
    }

    private record VoidResponse(boolean success, String code, String message, Void data) implements ApiResponse<Void> {
    }

    private record MaterialRequestResponse(boolean success, String code, String message, McsMaterialRequestResult data)
            implements ApiResponse<McsMaterialRequestResult> {
    }

    private record TransferPage(
            List<McsTransferSummary> content,
            long totalElements,
            int totalPages,
            int currentPage,
            int size
    ) {
    }

    private record TransferListResponse(boolean success, String code, String message, TransferPage data)
            implements ApiResponse<TransferPage> {
    }

    private record PlcEventPage(
            List<McsPlcEventSummary> content,
            long totalElements,
            int totalPages,
            int currentPage,
            int size
    ) {
    }

    private record PlcEventListResponse(boolean success, String code, String message, PlcEventPage data)
            implements ApiResponse<PlcEventPage> {
    }

    private record CancelMaterialRequestResponse(boolean success, String code, String message, Integer data)
            implements ApiResponse<Integer> {
    }
}
