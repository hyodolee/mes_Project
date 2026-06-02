package com.mes.application.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.application.service.planning.McsTransferClient;
import com.mes.application.service.planning.WorkOrderService;
import com.mes.domain.ai.dto.WorkOrderAiAnalysisResponse;
import com.mes.domain.planning.workorder.dto.WorkOrderDto;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class WorkOrderAiAnalysisService {

    private final WorkOrderService workOrderService;
    private final McsTransferClient mcsTransferClient;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public WorkOrderAiAnalysisService(
            WorkOrderService workOrderService,
            McsTransferClient mcsTransferClient,
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            ObjectMapper objectMapper,
            @Value("${OPENAI_API_KEY:}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}") String model
    ) {
        this.workOrderService = workOrderService;
        this.mcsTransferClient = mcsTransferClient;
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    public WorkOrderAiAnalysisResponse analyze(Long woId) {
        WorkOrderDto workOrder = workOrderService.getWorkOrder(woId);
        List<McsTransferClient.McsTransferSummary> transfers = mcsTransferClient.getTransfersByWorkOrder(woId);
        McsTransferClient.McsTransferSummary activeTransfer = findActiveTransfer(transfers);
        List<McsTransferClient.McsPlcEventSummary> plcEvents = activeTransfer == null
                ? List.of()
                : mcsTransferClient.getPlcEventsByTransfer(activeTransfer.transferId(), 10);

        WorkOrderAiAnalysisResponse.Evidence evidence = toEvidence(workOrder, activeTransfer, plcEvents);

        if (!hasApiKey()) {
            return ruleBasedAnalysis(evidence, false, "local-preview");
        }

        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            return ruleBasedAnalysis(evidence, false, "spring-ai-unavailable");
        }

        try {
            String content = builder.build()
                    .prompt()
                    .system(systemPrompt())
                    .user(userPrompt(evidence))
                    .call()
                    .content();
            WorkOrderAiAnalysisResponse parsed = objectMapper.readValue(content, WorkOrderAiAnalysisResponse.class);
            return new WorkOrderAiAnalysisResponse(
                    parsed.summary(),
                    safeList(parsed.facts()),
                    parsed.inference(),
                    parsed.impact(),
                    safeList(parsed.recommendedActions()),
                    evidence,
                    true,
                    model
            );
        } catch (Exception e) {
            return ruleBasedAnalysis(evidence, false, "fallback: " + e.getClass().getSimpleName());
        }
    }

    private McsTransferClient.McsTransferSummary findActiveTransfer(List<McsTransferClient.McsTransferSummary> transfers) {
        return transfers.stream()
                .filter(transfer -> !"CANCELLED".equals(transfer.transferStatus()))
                .findFirst()
                .orElse(transfers.isEmpty() ? null : transfers.get(0));
    }

    private WorkOrderAiAnalysisResponse.Evidence toEvidence(
            WorkOrderDto workOrder,
            McsTransferClient.McsTransferSummary transfer,
            List<McsTransferClient.McsPlcEventSummary> plcEvents
    ) {
        WorkOrderAiAnalysisResponse.WorkOrderEvidence workOrderEvidence = new WorkOrderAiAnalysisResponse.WorkOrderEvidence(
                workOrder.woId(),
                workOrder.woNo(),
                workOrder.plantNm(),
                workOrder.itemCd(),
                workOrder.itemNm(),
                workOrder.woStatus(),
                workOrder.lotNo()
        );

        WorkOrderAiAnalysisResponse.McsTransferEvidence transferEvidence = transfer == null ? null :
                new WorkOrderAiAnalysisResponse.McsTransferEvidence(
                        transfer.transferId(),
                        transfer.transferNo(),
                        transfer.transferStatus(),
                        transfer.transferStatusNm(),
                        transfer.fromLocationCd(),
                        transfer.toLocationCd(),
                        transfer.optimizeRule()
                );

        List<WorkOrderAiAnalysisResponse.PlcEventEvidence> eventEvidence = plcEvents.stream()
                .map(event -> new WorkOrderAiAnalysisResponse.PlcEventEvidence(
                        event.eventId(),
                        event.equipmentCd(),
                        event.eventType(),
                        event.eventStatus(),
                        event.errorCode(),
                        event.eventMessage(),
                        event.eventDtm(),
                        event.processResult()
                ))
                .toList();

        return new WorkOrderAiAnalysisResponse.Evidence(workOrderEvidence, transferEvidence, eventEvidence);
    }

    private WorkOrderAiAnalysisResponse ruleBasedAnalysis(
            WorkOrderAiAnalysisResponse.Evidence evidence,
            boolean aiGenerated,
            String modelLabel
    ) {
        List<String> facts = new ArrayList<>();
        WorkOrderAiAnalysisResponse.WorkOrderEvidence workOrder = evidence.workOrder();
        WorkOrderAiAnalysisResponse.McsTransferEvidence transfer = evidence.mcsTransfer();
        List<WorkOrderAiAnalysisResponse.PlcEventEvidence> events = evidence.plcEvents();

        facts.add("작업오더 " + workOrder.woNo() + "의 현재 MES 상태는 " + text(workOrder.woStatus()) + "입니다.");

        String summary;
        String inference;
        String impact;
        List<String> actions = new ArrayList<>();

        if (transfer == null) {
            summary = "MCS 자재 이동 요청이 아직 생성되지 않았습니다.";
            facts.add("연결된 MCS 이동오더가 없습니다.");
            inference = "작업 시작 전 필요한 자재 이동 요청 단계가 누락된 상태입니다.";
            impact = "자재 도착 여부를 확인할 수 없어 작업 시작이 차단될 수 있습니다.";
            actions.add("MES 작업오더에서 자재 요청을 생성합니다.");
            actions.add("MCS가 LOT, 출발/도착 Location, 경로를 자동 배정했는지 확인합니다.");
        } else if ("COMPLETED".equals(transfer.transferStatus())) {
            summary = "MCS 자재 이동이 완료되어 작업 시작이 가능한 상태입니다.";
            facts.add("MCS 이동오더 " + transfer.transferNo() + "가 COMPLETED 상태입니다.");
            inference = "자재 이동과 재고 반영이 완료된 정상 흐름으로 판단됩니다.";
            impact = "MES 작업 시작을 진행할 수 있습니다.";
            actions.add("작업 시작 버튼으로 생산 작업을 진행합니다.");
            actions.add("필요하면 MCS 재고 이력에서 이동 반영을 확인합니다.");
        } else if ("FAILED".equals(transfer.transferStatus())) {
            summary = "MCS 자재 이동 실패로 작업 시작이 차단된 상태입니다.";
            facts.add("MCS 이동오더 " + transfer.transferNo() + "가 FAILED 상태입니다.");
            addLatestEventFact(facts, events);
            inference = latestEventInference(events);
            impact = "자재가 도착하지 않았거나 재고 반영이 완료되지 않아 MES 작업 시작이 차단됩니다.";
            actions.add("MCS 이동오더 상세에서 PLC 이벤트와 실패 메시지를 확인합니다.");
            actions.add("실패 이동오더를 취소합니다.");
            actions.add("MES에서 자재 요청을 다시 수행해 새 이동오더를 생성합니다.");
        } else {
            summary = "MCS 자재 이동이 아직 완료되지 않았습니다.";
            facts.add("MCS 이동오더 " + transfer.transferNo() + "의 현재 상태는 " + transfer.transferStatus() + "입니다.");
            addLatestEventFact(facts, events);
            inference = "자재 이동 완료 이벤트가 확인되기 전까지는 작업 시작을 보류해야 합니다.";
            impact = "MES 작업 시작은 MCS 이동 완료 후 가능하도록 제한됩니다.";
            actions.add("PLC 이동 진행 상태를 확인합니다.");
            actions.add("MCS 이동오더가 COMPLETED가 된 뒤 작업을 시작합니다.");
        }

        return new WorkOrderAiAnalysisResponse(
                summary,
                facts,
                inference,
                impact,
                actions,
                evidence,
                aiGenerated,
                modelLabel
        );
    }

    private void addLatestEventFact(List<String> facts, List<WorkOrderAiAnalysisResponse.PlcEventEvidence> events) {
        if (events.isEmpty()) {
            facts.add("조회된 PLC 이벤트가 없습니다.");
            return;
        }
        WorkOrderAiAnalysisResponse.PlcEventEvidence latest = events.get(0);
        facts.add("최근 PLC 이벤트는 " + latest.eventType() + "이며 메시지는 '" + text(latest.eventMessage()) + "'입니다.");
    }

    private String latestEventInference(List<WorkOrderAiAnalysisResponse.PlcEventEvidence> events) {
        if (events.isEmpty()) {
            return "PLC 이벤트 로그가 없어 실패 원인은 MCS 이동오더 상세에서 추가 확인이 필요합니다.";
        }

        WorkOrderAiAnalysisResponse.PlcEventEvidence latest = events.get(0);
        return switch (text(latest.eventType())) {
            case "EQUIPMENT_ERROR" -> "설비 오류 이벤트가 발생했기 때문에 컨베이어 또는 이송 장치 이상으로 자재 이동이 중단된 가능성이 높습니다.";
            case "INTERLOCK_BLOCKED" -> "인터락 차단 이벤트가 발생했기 때문에 안전 조건이나 선행 조건 미충족으로 이동이 막힌 것으로 보입니다.";
            case "ARRIVED_WRONG_LOCATION" -> "도착 위치 불일치 이벤트가 발생해 센서 감지 또는 Location 매핑 오류 가능성이 있습니다.";
            default -> "최근 PLC 이벤트를 기준으로 자재 이동 상태를 추가 확인해야 합니다.";
        };
    }

    private String systemPrompt() {
        return """
                당신은 MES/MCS 제조 운영 분석 보조자입니다.
                입력으로 제공된 근거 데이터만 사용해 한국어로 답하세요.
                DB 변경, 작업 시작, 재고 변경, 이동오더 취소를 직접 수행한다고 말하지 마세요.
                반드시 JSON만 반환하세요. JSON 필드는 summary, facts, inference, impact, recommendedActions 입니다.
                facts와 recommendedActions는 문자열 배열입니다.
                """;
    }

    private String userPrompt(WorkOrderAiAnalysisResponse.Evidence evidence) throws Exception {
        return """
                다음 작업오더의 현재 상태를 분석하세요.
                출력은 JSON만 반환하세요.

                근거 데이터:
                %s
                """.formatted(objectMapper.writeValueAsString(evidence));
    }

    private boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    private List<String> safeList(List<String> value) {
        return value == null ? List.of() : value;
    }

    private String text(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
