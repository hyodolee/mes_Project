package com.mes.application.service.planning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.global.exception.BusinessException;
import com.mes.global.exception.ErrorCode;
import com.mes.global.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpHeaders;
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
            JwtTokenProvider jwtTokenProvider,
            @Value("${mcs.api.base-url:http://127.0.0.1:8081}") String baseUrl,
            @Value("${mcs.api.service-username:admin}") String serviceUsername
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);

        this.restClient = builder
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().set(HttpHeaders.AUTHORIZATION,
                            "Bearer " + jwtTokenProvider.generateToken(serviceUsername, List.of("ROLE_ADMIN")));
                    return execution.execute(request, body);
                })
                .build();
        this.objectMapper = objectMapper;
    }

    public Long createTransfer(McsTransferOrderPayload payload) {
        try {
            McsApiResponse<Long> response = restClient.post()
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
            McsApiResponse<Void> response = restClient.post()
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
            McsApiResponse<McsMaterialRequestResult> response = restClient.post()
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
            McsApiResponse<TransferPage> response = restClient.get()
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

    public List<McsTransferSummary> getAllTransfers(int size) {
        try {
            McsApiResponse<TransferPage> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/transfers")
                            .queryParam("page", 1)
                            .queryParam("size", size)
                            .build())
                    .retrieve()
                    .body(TransferListResponse.class);
            if (response == null || !response.success() || response.data() == null) {
                return List.of();
            }
            return response.data().content() == null ? List.of() : response.data().content();
        } catch (RestClientResponseException | ResourceAccessException e) {
            return List.of();
        }
    }

    public List<McsPlcEventSummary> getRecentPlcEvents(int size) {
        try {
            McsApiResponse<PlcEventPage> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/plc/events")
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

    public List<McsPlcEventSummary> getPlcEventsByTransfer(Long transferId, int size) {
        try {
            McsApiResponse<PlcEventPage> response = restClient.get()
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
            McsApiResponse<Void> response = restClient.post()
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
            McsApiResponse<Integer> response = restClient.post()
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

    private interface McsApiResponse<T> {
        boolean success();
        String message();
        T data();
    }

    private record TransferIdResponse(boolean success, String code, String message, Long data) implements McsApiResponse<Long> {
    }

    private record VoidResponse(boolean success, String code, String message, Void data) implements McsApiResponse<Void> {
    }

    private record MaterialRequestResponse(boolean success, String code, String message, McsMaterialRequestResult data)
            implements McsApiResponse<McsMaterialRequestResult> {
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
            implements McsApiResponse<TransferPage> {
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
            implements McsApiResponse<PlcEventPage> {
    }

    private record CancelMaterialRequestResponse(boolean success, String code, String message, Integer data)
            implements McsApiResponse<Integer> {
    }
}
