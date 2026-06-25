package com.mes.application.service.ai.query;

import com.mes.application.service.ai.query.tools.OperationToolSet;
import com.mes.application.service.ai.query.tools.OperationToolViews;
import com.mes.domain.ai.dto.NaturalLanguageQueryResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 호출이 실패했을 때 기본 조회 결과로 답변을 만든다.
 *
 * <p>
 * 모델이 만든 분석처럼 보이면 사용자가 오해할 수 있으므로,
 * fallback 답변에는 기본 조회로 전환했다는 안내 문구를 함께 담는다.
 * </p>
 */
@Service
public class OperationQueryFallbackService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OperationQueryFallbackService.class);

    private final OperationToolsFactory operationToolsFactory;

    public OperationQueryFallbackService(OperationToolsFactory operationToolsFactory) {
        this.operationToolsFactory = operationToolsFactory;
    }

    public NaturalLanguageQueryResponse createFallback(String question, String reason) {
        List<String> dataPoints = new ArrayList<>();
        List<String> issues = new ArrayList<>();
        List<String> summaries = new ArrayList<>();
        OperationToolSet tools = operationToolsFactory.create(dataPoints);

        collectTransferSummary(tools, issues, summaries);
        collectPlcSummary(tools, issues, summaries);
        collectWorkOrderSummary(tools, issues, summaries);

        if (isIssueQuestion(question)) {
            collectStockSummary(tools, issues, summaries);
            collectEquipmentSummary(tools, issues, summaries);
            collectQualitySummary(tools, issues, summaries);
            collectDefectSummary(tools, issues, summaries);
        }

        String answer;
        if (dataPoints.isEmpty()) {
            answer = "지금은 데이터를 가져오지 못했어요. 잠시 후 다시 시도해 주세요.";
        } else if (issues.isEmpty()) {
            answer = reason + "\n현재 기본 조회에서 바로 드러난 문제는 없습니다.\n"
                    + "확인 범위: " + String.join(", ", summaries) + ".";
        } else {
            answer = reason + "\n현재 기본 조회에서 확인된 문제는 "
                    + String.join(", ", issues) + "입니다.\n"
                    + "확인 범위: " + String.join(", ", summaries) + ".";
        }
        return new NaturalLanguageQueryResponse(answer, "FALLBACK", dataPoints, false, null);
    }

    private void collectTransferSummary(OperationToolSet tools, List<String> issues, List<String> summaries) {
        try {
            var transfers = tools.mcs().getTransfers();
            long failed = transfers.stream().filter(t -> "FAILED".equals(t.getStatus())).count();
            summaries.add("자재 이동 " + transfers.size() + "건");
            addIssue(issues, "자재 이동 실패", failed);
        } catch (Exception e) {
            log.debug("fallback 자재 이동 조회 실패", e);
        }
    }

    private void collectPlcSummary(OperationToolSet tools, List<String> issues, List<String> summaries) {
        try {
            var events = tools.mcs().getRecentPlcEvents();
            long errors = events.stream()
                    .filter(e -> hasAny(e.getEventStatus(), "ERROR", "FAIL", "FAILED", "INTERLOCK")
                            || hasAny(e.getProcessResult(), "ERROR", "FAIL", "FAILED", "VALIDATION_FAILED"))
                    .count();
            summaries.add("PLC 이벤트 " + events.size() + "건");
            addIssue(issues, "PLC 오류/검증 실패", errors);
        } catch (Exception e) {
            log.debug("fallback PLC 이벤트 조회 실패", e);
        }
    }

    private void collectWorkOrderSummary(OperationToolSet tools, List<String> issues, List<String> summaries) {
        try {
            var orders = tools.mes().getWorkOrders();
            long inProgress = orders.stream().filter(o -> "진행".equals(o.getStatus())).count();
            long pending = orders.stream().filter(o -> "대기".equals(o.getStatus())).count();
            summaries.add("작업지시 " + orders.size() + "건");
            if (pending > 0) {
                issues.add("대기 작업지시 " + pending + "건");
            } else if (inProgress > 0) {
                summaries.add("진행 작업지시 " + inProgress + "건");
            }
        } catch (Exception e) {
            log.debug("fallback 작업지시 조회 실패", e);
        }
    }

    private void collectStockSummary(OperationToolSet tools, List<String> issues, List<String> summaries) {
        try {
            var stocks = tools.status().getStocks();
            long blocked = stocks.stream()
                    .filter(s -> isAbnormalStockStatus(s.getStockStatus()) || zeroOrLess(s.getAvailableQty()))
                    .count();
            summaries.add("재고 " + stocks.size() + "건");
            addIssue(issues, "가용 불가/비정상 재고", blocked);
        } catch (Exception e) {
            log.debug("fallback 재고 조회 실패", e);
        }
    }

    private void collectEquipmentSummary(OperationToolSet tools, List<String> issues, List<String> summaries) {
        try {
            var statuses = tools.status().getEquipmentStatus();
            long abnormal = statuses.stream()
                    .filter(s -> isAbnormalEquipmentStatus(s.getOperStatus()))
                    .count();
            summaries.add("설비 상태 " + statuses.size() + "건");
            addIssue(issues, "비정상 설비 상태", abnormal);
        } catch (Exception e) {
            log.debug("fallback 설비 상태 조회 실패", e);
        }

        try {
            var downtimes = tools.status().getEquipmentDowntimes();
            summaries.add("설비 비가동 " + downtimes.size() + "건");
            addIssue(issues, "설비 비가동", downtimes.size());
        } catch (Exception e) {
            log.debug("fallback 설비 비가동 조회 실패", e);
        }
    }

    private void collectQualitySummary(OperationToolSet tools, List<String> issues, List<String> summaries) {
        try {
            var inspections = tools.status().getInspectResults();
            long failed = inspections.stream()
                    .filter(i -> positive(i.getFailQty()) || hasAny(i.getJudgeResult(), "FAIL", "NG", "부적합", "불합격"))
                    .count();
            summaries.add("품질 검사 " + inspections.size() + "건");
            addIssue(issues, "검사 부적합", failed);
        } catch (Exception e) {
            log.debug("fallback 품질 검사 조회 실패", e);
        }
    }

    private void collectDefectSummary(OperationToolSet tools, List<String> issues, List<String> summaries) {
        try {
            var defects = tools.status().getDefects();
            double defectQty = defects.stream()
                    .map(OperationToolViews.DefectView::getDefectQty)
                    .filter(v -> v != null && v > 0)
                    .mapToDouble(Double::doubleValue)
                    .sum();
            summaries.add("불량 이력 " + defects.size() + "건");
            if (defectQty > 0) {
                issues.add("불량 수량 " + formatNumber(defectQty));
            } else {
                addIssue(issues, "불량 이력", defects.size());
            }
        } catch (Exception e) {
            log.debug("fallback 불량 이력 조회 실패", e);
        }
    }

    private boolean isIssueQuestion(String question) {
        return hasAny(question, "문제", "이상", "오류", "실패", "불량", "부적합", "부족", "정지", "그 밖", "또", "전체");
    }

    private void addIssue(List<String> issues, String label, long count) {
        if (count > 0) {
            issues.add(label + " " + count + "건");
        }
    }

    private boolean hasAny(String value, String... keywords) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String upper = value.toUpperCase();
        for (String keyword : keywords) {
            if (upper.contains(keyword.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean zeroOrLess(Double value) {
        return value != null && value <= 0;
    }

    private boolean positive(Double value) {
        return value != null && value > 0;
    }

    private boolean isAbnormalStockStatus(String status) {
        return status != null && !status.isBlank()
                && !hasAny(status, "정상", "NORMAL", "AVAILABLE", "OK", "-");
    }

    private boolean isAbnormalEquipmentStatus(String status) {
        return status != null && !status.isBlank()
                && !hasAny(status, "정상", "가동", "RUN", "RUNNING", "OK", "-");
    }

    private String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format("%.1f", value);
    }
}
