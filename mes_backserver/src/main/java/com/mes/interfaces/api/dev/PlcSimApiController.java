package com.mes.interfaces.api.dev;

import com.mes.global.response.ApiResponse;
import com.mes.mcs.application.service.plc.PlcEventService;
import com.mes.mcs.domain.plc.dto.PlcEventRequest;
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
 * PLC 이벤트 시뮬레이터 (데모/테스트용).
 *
 * <p>CMD에서 PowerShell 스크립트를 돌리는 대신, 화면 버튼으로 PLC 이벤트를 발생시킨다.
 * 가짜 이벤트를 주입하므로 <b>운영 배포 시에는 비활성화</b>한다:
 * {@code app.plc-sim.enabled=false}. (기본값은 켜짐 — 로컬/데모)</p>
 *
 * <p>인증된 사용자만 호출 가능(/api/v1/** 보호 경로). 내부적으로 병합된
 * {@link PlcEventService#receiveEvent}를 직접 호출한다.</p>
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

    /** 화면에 버튼으로 노출할 시나리오 목록 */
    @GetMapping("/scenarios")
    public ApiResponse<List<Map<String, String>>> scenarios() {
        return ApiResponse.ok(List.of(
                Map.of("code", "NORMAL", "label", "정상 이동", "desc", "시작→완료 정상 흐름"),
                Map.of("code", "MISSING_TO_LOCATION", "label", "목적지 누락", "desc", "toLocationCd 빠짐 → 검증 실패"),
                Map.of("code", "MISSING_LOT", "label", "LOT 누락", "desc", "lotNo 빠짐 → 검증 실패"),
                Map.of("code", "EQUIPMENT_ERROR", "label", "설비 오류", "desc", "모터 과부하"),
                Map.of("code", "INTERLOCK", "label", "인터락 차단", "desc", "목적지 점유로 차단"),
                Map.of("code", "WRONG_LOCATION", "label", "오도착", "desc", "다른 위치에서 감지")
        ));
    }

    /** 선택한 이동오더에 대해 시나리오 이벤트를 발생시킨다. */
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
                // toLocationCd 누락 → MCS 검증 실패(VALIDATION_FAILED)
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
