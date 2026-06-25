package com.mes.application.service.ai.analysis;

import com.mes.domain.ai.dto.AreaAssessmentDto;
import com.mes.domain.ai.dto.GlobalOperationAiAnalysisResponse;
import com.mes.domain.ai.dto.GlobalOperationEvidenceDto;
import com.mes.domain.ai.dto.McsTransferSummaryDto;
import com.mes.domain.ai.dto.OperationDomainSummaryDto;
import com.mes.domain.ai.dto.WorkOrderSummaryDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 분석 호출이 불가능할 때 규칙 기반 운영 요약을 만든다.
 *
 * <p>
 * 외부 AI가 실패해도 대시보드가 비어 보이지 않도록,
 * 이미 수집한 운영 스냅샷을 기준으로 심각도, 주요 이슈, 조치 문구를 만든다.
 * </p>
 */
@Service
public class OperationAnalysisFallbackService {

    /**
     * 수집된 운영 근거 데이터만 사용해 GlobalOperationAiAnalysisResponse를 만든다.
     */
    public GlobalOperationAiAnalysisResponse create(
            GlobalOperationEvidenceDto evidence,
            boolean aiGenerated,
            String modelLabel
    ) {
        WorkOrderSummaryDto wo = evidence.getWorkOrders();
        McsTransferSummaryDto tr = evidence.getTransfers();
        OperationDomainSummaryDto dm = evidence.getDomains() != null
                ? evidence.getDomains()
                : new OperationDomainSummaryDto();
        int criticalCount = evidence.getCriticalEvents().size();
        long domainIssues = dm.getInspectFailed()
                + dm.getDefectCount()
                + dm.getStockLow()
                + dm.getStockRestricted()
                + dm.getEquipDown();

        String severity = decideSeverity(wo, tr, criticalCount, domainIssues);
        List<String> keyIssues = buildKeyIssues(wo, tr, dm, criticalCount);
        SummaryText summaryText = buildSummaryText(wo, tr, severity, criticalCount);

        return new GlobalOperationAiAnalysisResponse(
                severity,
                summaryText.summary(),
                keyIssues.stream().limit(8).toList(),
                summaryText.inference(),
                summaryText.impact(),
                summaryText.actions().stream().limit(6).toList(),
                buildRuleAreas(wo, tr, dm),
                evidence,
                aiGenerated,
                modelLabel
        );
    }

    /**
     * 실패/지연/부적합/비가동이 있으면 WARNING 이상으로 올린다.
     */
    private String decideSeverity(
            WorkOrderSummaryDto wo,
            McsTransferSummaryDto tr,
            int criticalCount,
            long domainIssues
    ) {
        if (tr.getFailed() > 3 || criticalCount > 3) {
            return "CRITICAL";
        }
        if (tr.getFailed() > 0 || criticalCount > 0 || wo.getDelayed() > 0 || domainIssues > 0) {
            return "WARNING";
        }
        return "NORMAL";
    }

    /**
     * 화면의 핵심 이슈 목록에 보여줄 짧은 문장들을 우선순위대로 만든다.
     */
    private List<String> buildKeyIssues(
            WorkOrderSummaryDto wo,
            McsTransferSummaryDto tr,
            OperationDomainSummaryDto dm,
            int criticalCount
    ) {
        List<String> keyIssues = new ArrayList<>();
        if (tr.getFailed() > 0) {
            keyIssues.add("이송 실패 " + tr.getFailed() + "건");
        }
        if (criticalCount > 0) {
            keyIssues.add("최근 1시간 중요 이벤트 " + criticalCount + "건");
        }
        if (dm.getInspectFailed() > 0) {
            keyIssues.add("검사 부적합 " + dm.getInspectFailed() + "건");
        }
        if (dm.getStockLow() + dm.getStockRestricted() > 0) {
            keyIssues.add("재고 주의 " + (dm.getStockLow() + dm.getStockRestricted()) + "품목");
        }
        if (dm.getEquipDown() > 0) {
            keyIssues.add("설비 비가동 " + dm.getEquipDown() + "대");
        }
        if (wo.getDelayed() > 0) {
            keyIssues.add("시작 지연 우려 작업 " + wo.getDelayed() + "건");
        }
        keyIssues.add("작업 진행 " + wo.getInProgress() + "건 · 대기 " + wo.getPending() + "건");
        keyIssues.add("전체 작업 " + wo.getTotal() + "건 · 오늘 완료 " + wo.getCompletedToday() + "건");
        if (tr.getActive() > 0) {
            keyIssues.add("진행 중 이송 " + tr.getActive() + "건");
        }
        keyIssues.add("오늘 이송 완료 " + tr.getCompletedToday() + "건");
        return keyIssues;
    }

    /**
     * 상태별로 요약, 원인 추정, 생산 영향, 조치 문구를 다르게 만든다.
     */
    private SummaryText buildSummaryText(
            WorkOrderSummaryDto wo,
            McsTransferSummaryDto tr,
            String severity,
            int criticalCount
    ) {
        if (tr.getFailed() > 0) {
            return failedTransferSummary(wo, tr, severity, criticalCount);
        }
        if (criticalCount > 0 || wo.getDelayed() > 0) {
            return warningSummary(wo, tr, severity, criticalCount);
        }
        return normalSummary(wo, tr);
    }

    private SummaryText failedTransferSummary(
            WorkOrderSummaryDto wo,
            McsTransferSummaryDto tr,
            String severity,
            int criticalCount
    ) {
        String summary = "현재 공장은 " + severity + " 상태입니다. 이송 실패 " + tr.getFailed()
                + "건, 진행 작업 " + wo.getInProgress() + "건, 대기 " + wo.getPending()
                + "건이며, 전체 작업 " + wo.getTotal() + "건 중 오늘 " + wo.getCompletedToday() + "건이 완료됐습니다.";
        String inference = "MCS 이송 실패가 남아 있어 해당 작업 지시의 자재 공급이 지연되고 있는 것으로 보입니다. "
                + (criticalCount > 0
                ? "최근 1시간 내 PLC 중요 이벤트도 " + criticalCount + "건 발생해 설비·통신 측 원인을 함께 살펴야 합니다."
                : "현재 진행 중인 이송 " + tr.getActive() + "건이 정상적으로 완료되는지 모니터링이 필요합니다.");
        String impact = "실패한 이송과 연결된 작업 지시는 시작이 막혀 오늘 생산 목표 달성이 어려울 수 있습니다. "
                + "대기 작업 " + wo.getPending() + "건의 착수도 함께 지연될 가능성이 있어 우선 조치가 필요합니다.";
        List<String> actions = new ArrayList<>();
        actions.add("실패한 이송 오더 상세와 실패 사유 확인");
        actions.add("PLC 이벤트 로그에서 오류·검증 실패 점검");
        actions.add("필요 시 자재 요청을 다시 실행해 이송 재생성");
        actions.add("지연 작업의 계획 시작 시각 재조정 검토");
        actions.add("반복 실패 구간은 설비·통신 담당자에게 공유");
        return new SummaryText(summary, inference, impact, actions);
    }

    private SummaryText warningSummary(
            WorkOrderSummaryDto wo,
            McsTransferSummaryDto tr,
            String severity,
            int criticalCount
    ) {
        String summary = "현재 공장은 " + severity + " 상태입니다. 중요 이벤트 " + criticalCount
                + "건, 시작 지연 우려 작업 " + wo.getDelayed() + "건이 있으며, 진행 작업 "
                + wo.getInProgress() + "건, 진행 중 이송 " + tr.getActive() + "건이 운영 중입니다.";
        String inference = "이송 실패는 없으나 PLC 중요 이벤트 또는 시작이 지연된 작업이 있어 주의가 필요한 상태입니다. "
                + "이벤트가 반복되면 이송 흐름에도 영향을 줄 수 있어 조기 확인이 필요합니다.";
        String impact = "지연 작업이 누적되면 후속 공정 일정과 당일 생산량에 영향을 줄 수 있습니다. "
                + "현재 진행 중인 이송과 작업은 정상 흐름을 유지하고 있습니다.";
        List<String> actions = new ArrayList<>();
        actions.add("시작 지연 작업의 계획 시작 시각과 자재 준비 상태 점검");
        actions.add("최근 PLC 중요 이벤트의 발생 설비·원인 확인");
        actions.add("진행 중 이송 " + tr.getActive() + "건의 진행 상태 모니터링");
        actions.add("대기 작업 " + wo.getPending() + "건의 착수 준비 확인");
        return new SummaryText(summary, inference, impact, actions);
    }

    private SummaryText normalSummary(WorkOrderSummaryDto wo, McsTransferSummaryDto tr) {
        String summary = "현재 공장은 정상 상태입니다. 전체 작업 " + wo.getTotal() + "건 중 진행 "
                + wo.getInProgress() + "건, 대기 " + wo.getPending() + "건이며, 진행 중 이송 "
                + tr.getActive() + "건이 정상 흐름으로 운영되고 있습니다.";
        String inference = "이송 실패와 중요 이벤트가 없어 자재 흐름과 생산이 안정적으로 진행되고 있습니다. "
                + "오늘 작업 " + wo.getCompletedToday() + "건, 이송 " + tr.getCompletedToday() + "건이 완료됐습니다.";
        String impact = "현재 조건에서는 계획된 작업 지시를 정상적으로 진행할 수 있습니다. "
                + "대기 작업의 착수 준비 상태만 주기적으로 확인하면 됩니다.";
        List<String> actions = new ArrayList<>();
        actions.add("진행 중 작업과 이송 상태 정기 모니터링");
        actions.add("대기 작업 " + wo.getPending() + "건의 시작 준비 상태 점검");
        actions.add("당일 생산 계획 대비 완료 추이 확인");
        return new SummaryText(summary, inference, impact, actions);
    }

    /**
     * 생산, 이송, 품질, 재고, 설비 5개 영역의 한 줄 진단을 만든다.
     */
    private List<AreaAssessmentDto> buildRuleAreas(
            WorkOrderSummaryDto wo,
            McsTransferSummaryDto tr,
            OperationDomainSummaryDto dm
    ) {
        List<AreaAssessmentDto> areas = new ArrayList<>();
        areas.add(new AreaAssessmentDto("생산",
                wo.getDelayed() > 0 ? "WARNING" : "NORMAL",
                wo.getDelayed() > 0
                        ? "진행 " + wo.getInProgress() + "건, 시작 지연 우려 " + wo.getDelayed() + "건으로 일정 점검 필요"
                        : "진행 " + wo.getInProgress() + "건 · 대기 " + wo.getPending() + "건, 계획대로 진행 중"));
        areas.add(new AreaAssessmentDto("이송",
                tr.getFailed() > 0 ? "WARNING" : "NORMAL",
                tr.getFailed() > 0
                        ? "이송 " + tr.getFailed() + "건 실패 - 목적지·PLC 송신부 점검 필요"
                        : "진행 " + tr.getActive() + "건 정상 흐름, 실패 없음"));
        long qualityIssues = dm.getInspectFailed() + dm.getDefectCount();
        areas.add(new AreaAssessmentDto("품질",
                qualityIssues > 0 ? "WARNING" : "NORMAL",
                qualityIssues > 0
                        ? "검사 부적합 " + dm.getInspectFailed() + "건 · 불량 " + dm.getDefectCount() + "건, 재작업 부담 발생"
                        : "검사 이상 없음, 불량 미발생"));
        long stockIssues = dm.getStockLow() + dm.getStockRestricted();
        areas.add(new AreaAssessmentDto("재고",
                stockIssues > 0 ? "WARNING" : "NORMAL",
                stockIssues > 0
                        ? "부족 " + dm.getStockLow() + " · 사용제한 " + dm.getStockRestricted() + "품목, 공급 확인 필요"
                        : "재고 정상 범위, 부족 품목 없음"));
        areas.add(new AreaAssessmentDto("설비",
                dm.getEquipDown() > 0 ? "WARNING" : "NORMAL",
                dm.getEquipDown() > 0
                        ? "비가동 " + dm.getEquipDown() + "대 - 가동 복구 우선"
                        : "가동 " + dm.getEquipRunning() + "대 정상, 비가동 이력 " + dm.getEquipDowntime() + "건"));
        return areas;
    }

    private record SummaryText(
            String summary,
            String inference,
            String impact,
            List<String> actions
    ) {
    }
}
