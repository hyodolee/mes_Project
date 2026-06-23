import { useEffect, useState } from 'react';
import useSWR from 'swr';

import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Divider from '@mui/material/Divider';
import Grid from '@mui/material/Grid';
import LinearProgress from '@mui/material/LinearProgress';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

import {
  CheckCircleOutlined,
  DatabaseOutlined,
  ExperimentOutlined,
  RobotOutlined,
  SettingOutlined,
  SwapOutlined,
  ToolOutlined,
  WarningOutlined
} from '@ant-design/icons';

import MainCard from 'components/MainCard';
import { aiApi } from 'api/mes/ai';
import { getApiData, useOperationsDashboard } from 'sections/operations/operationsDashboard';

const AI_OPERATIONS_SUMMARY_STORAGE_KEY = 'mes.ai.operations.summary.v1';

function loadSaved() {
  if (typeof window === 'undefined') return { summary: null, savedAt: null };
  try {
    const raw = window.localStorage.getItem(AI_OPERATIONS_SUMMARY_STORAGE_KEY);
    if (!raw) return { summary: null, savedAt: null };
    const parsed = JSON.parse(raw);
    return { summary: parsed?.summary || null, savedAt: parsed?.savedAt || null };
  } catch {
    return { summary: null, savedAt: null };
  }
}

function saveSummary(summary) {
  if (typeof window === 'undefined' || !summary) return new Date().toISOString();
  const savedAt = new Date().toISOString();
  window.localStorage.setItem(AI_OPERATIONS_SUMMARY_STORAGE_KEY, JSON.stringify({ savedAt, summary }));
  return savedAt;
}

function timeAgo(iso) {
  if (!iso) return null;
  const min = Math.floor((Date.now() - new Date(iso).getTime()) / 60000);
  if (min < 1) return '방금 전';
  if (min < 60) return `${min}분 전`;
  const hr = Math.floor(min / 60);
  if (hr < 24) return `${hr}시간 전`;
  return `${Math.floor(hr / 24)}일 전`;
}

const SEVERITY = {
  CRITICAL: { color: 'error', label: '긴급' },
  WARNING: { color: 'warning', label: '주의' },
  NORMAL: { color: 'success', label: '정상' }
};

// 여러 문장이 한 줄로 길게 이어진 텍스트를 문장 단위로 분리한다.
// 종결부호(. ! ?) 뒤에 공백이 오는 지점에서만 끊어 "3.5" 같은 소수점은 보존한다.
function toSentences(text) {
  if (!text || text === '-') return [];
  return String(text)
    .replace(/([.!?])\s+/g, '$1\n')
    .split('\n')
    .map((sentence) => sentence.trim())
    .filter(Boolean);
}

// 문단을 문장별 불릿 줄로 렌더링 — 한눈에 읽히도록 가독성을 높인다.
function BulletLines({ text, variant = 'body2', color = 'text.secondary', dotColor, sx }) {
  const lines = toSentences(text);
  if (lines.length === 0) return null;
  return (
    <Stack spacing={0.75} sx={{ mt: 0.5, ...sx }}>
      {lines.map((line, idx) => (
        <Stack key={idx} direction="row" spacing={1} sx={{ alignItems: 'flex-start' }}>
          <Box sx={{ flexShrink: 0, mt: '2px', color: dotColor || 'text.disabled', lineHeight: 1.6 }}>•</Box>
          <Typography variant={variant} color={color} sx={{ lineHeight: 1.6 }}>
            {line}
          </Typography>
        </Stack>
      ))}
    </Stack>
  );
}

// ── ① AI 운영 브리핑 (AI 종합 판단) ───────────────────────────────────────────
function AiBriefingSection({ summaryData, savedAt, loading, error, refreshing, onRefresh }) {
  if (loading) return <LinearProgress sx={{ mb: 1 }} />;

  const sev = SEVERITY[summaryData?.severity] || SEVERITY.NORMAL;

  if (!summaryData) {
    return (
      <MainCard>
        <Stack spacing={1.5}>
          <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
            <Box sx={{ color: 'warning.main', fontSize: 18 }}>
              <RobotOutlined />
            </Box>
            <Typography variant="h5" sx={{ flex: 1 }}>
              AI 운영 브리핑
            </Typography>
            <Button size="small" variant="contained" onClick={onRefresh} disabled={refreshing}>
              {refreshing ? '분석 중' : '분석 시작'}
            </Button>
          </Stack>
          <Alert severity={error ? 'error' : 'info'}>
            {error
              ? '브리핑을 불러오지 못했습니다. 분석 시작 버튼으로 다시 요청하세요.'
              : '분석 시작 버튼을 누르면 AI가 작업·이송·품질·재고·설비를 종합 분석합니다.'}
          </Alert>
        </Stack>
      </MainCard>
    );
  }

  const issues = (summaryData.keyIssues || []).slice(0, 6);
  const actions = (summaryData.recommendedActions || []).slice(0, 5);

  return (
    <MainCard sx={{ borderLeft: '4px solid', borderLeftColor: `${sev.color}.main` }}>
      <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap', rowGap: 0.5 }}>
        <Box sx={{ color: `${sev.color}.main`, fontSize: 18 }}>
          {summaryData.severity === 'NORMAL' ? <CheckCircleOutlined /> : <WarningOutlined />}
        </Box>
        <Typography variant="h5">AI 운영 브리핑</Typography>
        <Chip label={sev.label} size="small" color={sev.color} />
        <Box sx={{ flex: 1 }} />
        <Typography variant="caption" color="text.secondary">
          마지막 분석: {timeAgo(savedAt) || '방금 전'}
          {summaryData.aiGenerated ? ` · ${summaryData.model}` : ''}
        </Typography>
        <Button size="small" variant="outlined" onClick={onRefresh} disabled={refreshing}>
          {refreshing ? '분석 중' : '다시 분석'}
        </Button>
      </Stack>
      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
        AI가 작업·이송·품질·재고·설비를 모두 종합해 핵심 문제와 조치를 알려줍니다
      </Typography>

      {/* 종합 요약 */}
      <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, display: 'block', mt: 2 }}>
        종합 요약
      </Typography>
      <BulletLines
        text={summaryData.summary}
        variant="subtitle1"
        color={`${sev.color}.main`}
        dotColor={`${sev.color}.main`}
        sx={{ mt: 0.75 }}
      />

      {/* 원인 추정 / 생산 영향 */}
      <Grid container spacing={2} sx={{ mt: 0.5 }}>
        {summaryData.inference && summaryData.inference !== '-' && (
          <Grid size={{ xs: 12, md: 6 }}>
            <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600 }}>
              원인 추정
            </Typography>
            <BulletLines text={summaryData.inference} />
          </Grid>
        )}
        {summaryData.productionImpact && summaryData.productionImpact !== '-' && (
          <Grid size={{ xs: 12, md: 6 }}>
            <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600 }}>
              생산 영향
            </Typography>
            <BulletLines text={summaryData.productionImpact} />
          </Grid>
        )}
      </Grid>

      <Divider sx={{ my: 2 }} />

      {/* 핵심 이슈 / 권장 조치 */}
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 6 }}>
          <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600 }}>
            핵심 이슈
          </Typography>
          <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 0.75, mt: 0.75 }}>
            {issues.length === 0 && (
              <Typography variant="body2" color="text.secondary">
                특이 이슈 없음
              </Typography>
            )}
            {issues.map((issue, idx) => (
              <Chip key={idx} label={issue} size="small" variant="outlined" color={sev.color} />
            ))}
          </Stack>
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600 }}>
            권장 조치 (우선순위)
          </Typography>
          <Stack spacing={0.5} sx={{ mt: 0.75 }}>
            {actions.map((action, idx) => (
              <Typography key={idx} variant="body2">
                {idx + 1}. {action}
              </Typography>
            ))}
          </Stack>
        </Grid>
      </Grid>
    </MainCard>
  );
}

// ── ② 영역별 진단 카드 ────────────────────────────────────────────────────────
function AreaCard({ icon, name, status, value, unit, comment }) {
  const sev = SEVERITY[status] || SEVERITY.NORMAL;
  return (
    <MainCard contentSX={{ p: 1.75 }} sx={{ borderTop: '3px solid', borderTopColor: `${sev.color}.main`, height: '100%' }}>
      <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
        <Box
          sx={{
            width: 28,
            height: 28,
            borderRadius: 1,
            bgcolor: 'secondary.lighter',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'text.secondary'
          }}
        >
          {icon}
        </Box>
        <Typography variant="subtitle2" sx={{ flex: 1 }}>
          {name}
        </Typography>
        <Chip label={sev.label} size="small" color={sev.color} variant={status === 'NORMAL' ? 'outlined' : 'filled'} />
      </Stack>
      <Typography variant="h4" sx={{ mt: 1.25, color: status === 'NORMAL' ? 'text.primary' : `${sev.color}.main` }}>
        {value}
        <Typography component="span" variant="caption" color="text.secondary" sx={{ ml: 0.5 }}>
          {unit}
        </Typography>
      </Typography>
      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5, lineHeight: 1.5 }}>
        {comment}
      </Typography>
    </MainCard>
  );
}

function buildAreas(d, aiSummary) {
  const quality = d.defectCount + d.failedInspectionCount;
  const stock = d.lowStockCount + d.restrictedStockCount;
  // AI가 영역별 진단을 줬으면 그 코멘트/상태를 우선 사용 (숫자는 대시보드 데이터 유지)
  const ai = {};
  (aiSummary?.areaAssessments || []).forEach((a) => {
    if (a?.area) ai[a.area] = a;
  });
  const merge = (name, base) => {
    const a = ai[name];
    return a ? { ...base, status: a.status || base.status, comment: a.comment || base.comment } : base;
  };
  const base = [
    {
      icon: <ToolOutlined />,
      name: '생산',
      value: d.inProgressWorkOrderCount,
      unit: '건 진행',
      status: 'NORMAL',
      comment: `진행 ${d.inProgressWorkOrderCount}건 · 대기 ${d.waitingWorkOrderCount}건`
    },
    {
      icon: <SwapOutlined />,
      name: '이송',
      value: d.failedTransferCount,
      unit: '건 실패',
      status: d.failedTransferCount > 0 ? 'WARNING' : 'NORMAL',
      comment:
        d.failedTransferCount > 0
          ? `이송 ${d.failedTransferCount}건 실패 — 확인 필요`
          : `이송 정상 진행 (이동 중 ${d.movingTransferCount}건)`
    },
    {
      icon: <ExperimentOutlined />,
      name: '품질',
      value: quality,
      unit: '건 이상',
      status: quality > 0 ? 'WARNING' : 'NORMAL',
      comment:
        quality > 0 ? `불량 ${d.defectCount} · 검사 부적합 ${d.failedInspectionCount}` : `검사 ${d.passedInspectionCount}건 합격, 이상 없음`
    },
    {
      icon: <DatabaseOutlined />,
      name: '재고',
      value: stock,
      unit: '품목 주의',
      status: stock > 0 ? 'WARNING' : 'NORMAL',
      comment: stock > 0 ? `부족 ${d.lowStockCount} · 사용 제한 ${d.restrictedStockCount}` : '재고 정상 범위'
    },
    {
      icon: <SettingOutlined />,
      name: '설비',
      value: d.equipmentIssueCount,
      unit: '대 이상',
      status: d.equipmentIssueCount > 0 ? 'WARNING' : 'NORMAL',
      comment:
        d.equipmentIssueCount > 0
          ? `비가동 ${d.downEquipmentCount} · 확인 ${d.unknownEquipmentCount}`
          : `${d.runningEquipmentCount}대 정상 가동`
    }
  ];
  return base.map((b) => merge(b.name, b));
}

export default function AiOperations() {
  const { dashboard, isLoading, errors } = useOperationsDashboard();

  const [{ summary: savedAiSummary, savedAt: savedAiAt }, setSaved] = useState(() => loadSaved());
  const [refreshing, setRefreshing] = useState(false);
  const [refreshError, setRefreshError] = useState(null);

  const {
    data: aiSummaryResponse,
    error: aiSummaryError,
    isLoading: aiSummaryLoading
  } = useSWR(savedAiSummary ? null : ['ai-operations-summary'], () => aiApi.getSummary(), {
    keepPreviousData: true,
    revalidateOnFocus: false,
    revalidateOnReconnect: false,
    revalidateIfStale: false
  });

  const fetchedAiSummary = getApiData(aiSummaryResponse, null);

  useEffect(() => {
    if (!fetchedAiSummary) return;
    const savedAt = saveSummary(fetchedAiSummary);
    setSaved({ summary: fetchedAiSummary, savedAt });
  }, [fetchedAiSummary]);

  const refreshAiSummary = async () => {
    setRefreshing(true);
    setRefreshError(null);
    try {
      const response = await aiApi.getSummary(true);
      const fresh = getApiData(response, null);
      if (fresh) {
        const savedAt = saveSummary(fresh);
        setSaved({ summary: fresh, savedAt });
      }
    } catch (e) {
      setRefreshError(e);
    } finally {
      setRefreshing(false);
    }
  };

  const aiSummary = savedAiSummary || fetchedAiSummary;
  const areas = buildAreas(dashboard, aiSummary);

  return (
    <Stack spacing={3}>
      <Box>
        <Typography variant="h3">AI 운영 분석</Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mt: 0.75 }}>
          AI가 공장 전체 데이터를 종합한 브리핑과, 영역별 현황을 한눈에 확인합니다.
        </Typography>
      </Box>

      <AiBriefingSection
        summaryData={aiSummary}
        savedAt={savedAiAt}
        loading={!aiSummary && aiSummaryLoading}
        error={refreshError || aiSummaryError}
        refreshing={refreshing}
        onRefresh={refreshAiSummary}
      />

      <Box>
        <Typography variant="h5" sx={{ mb: 0.25 }}>
          영역별 진단
        </Typography>
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1.5 }}>
          각 영역의 상태와 수치를 한눈에 (정상 / 주의 / 긴급)
        </Typography>
        {isLoading && <LinearProgress sx={{ mb: 1 }} />}
        {errors.map((error, idx) => (
          <Alert key={idx} severity="error" sx={{ mb: 1 }}>
            {error.message}
          </Alert>
        ))}
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: 'repeat(2, 1fr)', sm: 'repeat(3, 1fr)', md: 'repeat(5, 1fr)' },
            gap: 2
          }}
        >
          {areas.map((area) => (
            <AreaCard key={area.name} {...area} />
          ))}
        </Box>
      </Box>
    </Stack>
  );
}
