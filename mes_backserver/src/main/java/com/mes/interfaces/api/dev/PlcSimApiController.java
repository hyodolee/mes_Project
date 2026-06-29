package com.mes.interfaces.api.dev;

import com.mes.global.response.ApiResponse;
import com.mes.mcs.application.service.plc.PlcEventService;
import com.mes.mcs.domain.plc.dto.PlcEventRequest;
import io.sentry.Sentry;
import io.sentry.protocol.SentryId;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * PLC event simulator for local/demo testing.
 *
 * <p>
 * The UI calls this controller to generate representative PLC events without
 * running an external script. Disable it in production unless a temporary smoke
 * test is needed: {@code app.plc-sim.enabled=false}.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/dev/plc-sim")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.plc-sim.enabled", havingValue = "true", matchIfMissing = true)
public class PlcSimApiController {

    private static final String EQUIPMENT_CD = "CV-001";
    private static final String FROM_LOCATION = "NCM-01-01";
    private static final String TO_LOCATION = "NCM-01-02";
    private static final String LOT_NO = "LOT-SIM-001";

    private final PlcEventService plcEventService;

    /** Scenario list rendered as buttons on the PLC simulator screen. */
    @GetMapping("/scenarios")
    public ApiResponse<List<Map<String, String>>> scenarios() {
        return ApiResponse.ok(List.of(
                Map.of("code", "NORMAL", "label", "정상 이동", "desc", "시작부터 완료까지 정상 흐름"),
                Map.of("code", "MISSING_TO_LOCATION", "label", "목적지 누락", "desc", "toLocationCd 빠짐 → 검증 실패"),
                Map.of("code", "MISSING_LOT", "label", "LOT 누락", "desc", "lotNo 빠짐 → 검증 실패"),
                Map.of("code", "EQUIPMENT_ERROR", "label", "설비 오류", "desc", "모터 과부하"),
                Map.of("code", "INTERLOCK", "label", "인터락 차단", "desc", "목적지 점유로 차단"),
                Map.of("code", "WRONG_LOCATION", "label", "오도착", "desc", "다른 위치에서 감지")
        ));
    }

    /**
     * Sends a deliberate error to Sentry and returns a 500 response to the UI.
     *
     * <p>
     * This is a smoke-test path for "screen button -> backend error -> Sentry".
     * It does not create a PLC event and does not change transfer data.
     * </p>
     */
    @PostMapping("/sentry-error")
    public ApiResponse<Void> fireSentryError(@RequestBody(required = false) PlcSimRequest body) {
        Long transferId = body == null ? null : body.getTransferId();
        String transferText = transferId == null ? "none" : String.valueOf(transferId);
        IllegalStateException exception = new IllegalStateException(
                "PLC simulator Sentry error test. transferId=" + transferText
        );

        SentryId eventId = Sentry.captureException(exception);
        Sentry.flush(2000);

        throw new IllegalStateException(
                "PLC simulator Sentry error test sent. sentryEventId=" + eventId,
                exception
        );
    }

    /** Generates PLC events for the selected transfer order and scenario. */
    @PostMapping
    public ApiResponse<Map<String, Object>> fire(@RequestBody PlcSimRequest body) {
        if (body == null || body.getTransferId() == null) {
            throw new IllegalArgumentException("transferId는 필수입니다.");
        }
        String scenario = body.getScenario() == null ? "" : body.getScenario().trim().toUpperCase();
        Long transferId = body.getTransferId();

        int sent = switch (scenario) {
            case "NORMAL" -> {
                send(transferId, base("TRANSFER_STARTED", "NORMAL", TO_LOCATION, LOT_NO, null, "이동 시작"));
                send(transferId, base("TRANSFER_COMPLETED", "NORMAL", TO_LOCATION, LOT_NO, null, "이동 완료"));
                yield 2;
            }
            case "MISSING_TO_LOCATION" -> {
                // Missing toLocationCd should be stored as VALIDATION_FAILED by MCS validation.
                send(transferId, base("TRANSFER_STARTED", "NORMAL", null, LOT_NO, null, "목적지 누락 이동 시작"));
                yield 1;
            }
            case "MISSING_LOT" -> {
                send(transferId, base("TRANSFER_STARTED", "NORMAL", TO_LOCATION, null, null, "LOT 누락 이동 시작"));
                yield 1;
            }
            case "EQUIPMENT_ERROR" -> {
                send(transferId, base("EQUIPMENT_ERROR", "ERROR", TO_LOCATION, LOT_NO, "MOTOR_OVERLOAD", "CV-001 모터 과부하"));
                yield 1;
            }
            case "INTERLOCK" -> {
                send(transferId, base("INTERLOCK_BLOCKED", "INTERLOCK", TO_LOCATION, LOT_NO, "DESTINATION_BLOCKED", "목적지 점유로 이동 차단"));
                yield 1;
            }
            case "WRONG_LOCATION" -> {
                PlcEventRequest req = base("ARRIVED_WRONG_LOCATION", "INTERLOCK", TO_LOCATION, LOT_NO, "SENSOR_LOCATION_MISMATCH", "예상과 다른 위치에서 감지");
                req.setLocationCd("WRONG-LOCATION");
                send(transferId, req);
                yield 1;
            }
            default -> throw new IllegalArgumentException("알 수 없는 시나리오: " + scenario);
        };

        return ApiResponse.ok(Map.of(
                "transferId", transferId,
                "scenario", scenario,
                "eventsSent", sent,
                "message", sent + "건의 PLC 이벤트를 발생시켰습니다."
        ));
    }

    private void send(Long transferId, PlcEventRequest req) {
        req.setTargetId(transferId);
        plcEventService.receiveEvent(req);
    }

    private PlcEventRequest base(String eventType, String eventStatus, String toLocation,
                                 String lotNo, String errorCode, String message) {
        PlcEventRequest req = new PlcEventRequest();
        req.setEquipmentCd(EQUIPMENT_CD);
        req.setEventType(eventType);
        req.setEventStatus(eventStatus);
        req.setTargetType("TRANSFER");
        req.setLocationCd(FROM_LOCATION);
        req.setFromLocationCd(FROM_LOCATION);
        req.setToLocationCd(toLocation);
        req.setLotNo(lotNo);
        req.setErrorCode(errorCode);
        req.setMessage(message);
        req.setEventDtm(LocalDateTime.now());
        return req;
    }

    @lombok.Getter
    @lombok.Setter
    public static class PlcSimRequest {
        private Long transferId;
        private String scenario;
    }
}
