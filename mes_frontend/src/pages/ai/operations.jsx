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
  AlertOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  ThunderboltOutlined,
  WarningOutlined
} from '@ant-design/icons';

import MainCard from 'components/MainCard';
import { aiApi } from 'api/mes/ai';
import { StatCard, getApiData, useOperationsDashboard } from 'sections/operations/operationsDashboard';

const AI_OPERATIONS_SUMMARY_STORAGE_KEY = 'mes.ai.operations.summary.v1';

function loadSavedAiSummary() {
  if (typeof window === 'undefined') return null;

  try {
    const raw = window.localStorage.getItem(AI_OPERATIONS_SUMMARY_STORAGE_KEY);
    if (!raw) return null;
    return JSON.parse(raw)?.summary || null;
  } catch {
    return null;
  }
}

function saveAiSummary(summary) {
  if (typeof window === 'undefined' || !summary) return;

  window.localStorage.setItem(
    AI_OPERATIONS_SUMMARY_STORAGE_KEY,
    JSON.stringify({
      savedAt: new Date().toISOString(),
      summary
    })
  );
}

function AiBriefingSection({ summaryData, loading, error, refreshing, onRefresh }) {
  if (loading) return <LinearProgress sx={{ mb: 2 }} />;

  if (!summaryData) {
    return (
      <MainCard sx={{ mb: 2 }}>
        <Stack spacing={1.5}>
          <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
            <Box sx={{ color: 'warning.main', fontSize: 20 }}><AlertOutlined /></Box>
            <Typography variant="h5" sx={{ flex: 1 }}>AI 실시간 운영 브리핑</Typography>
            <Button size="small" variant="contained" onClick={onRefresh} disabled={refreshing}>
              {refreshing ? '분석 중' : '분석 시작'}
            </Button>
          </Stack>
          <Alert severity={error ? 'error' : 'info'}>
            {error
              ? '운영 브리핑을 불러오지 못했습니다. 분석 시작 버튼으로 다시 요청하세요.'
              : '아직 저장된 운영 브리핑이 없습니다. 분석 시작 버튼을 누르면 현재 운영 데이터를 한 번 분석합니다.'}
          </Alert>
        </Stack>
      </MainCard>
    );
  }

  const severityColor = summaryData.severity === 'CRITICAL' ? 'error' : summaryData.severity === 'WARNING' ? 'warning' : 'success';
  const severityLabel = summaryData.severity === 'CRITICAL' ? '긴급' : summaryData.severity === 'WARNING' ? '주의' : '정상';

  const evidence = summaryData.evidence || {};
  const wo = evidence.workOrders || {};
  const tr = evidence.transfers || {};
  const criticalEvents = evidence.criticalEvents || [];
  const metrics = [
    { label: '전체 작업', value: wo.total ?? 0 },
    { label: '진행', value: wo.inProgress ?? 0 },
    { label: '대기', value: wo.pending ?? 0 },
    { label: '지연', value: wo.delayed ?? 0, warn: (wo.delayed ?? 0) > 0 },
    { label: '오늘 완료', value: wo.completedToday ?? 0 },
    { label: '이송 진행', value: tr.active ?? 0 },
    { label: '이송 실패', value: tr.failed ?? 0, warn: (tr.failed ?? 0) > 0 },
    { label: '이송 완료', value: tr.completedToday ?? 0 }
  ];

  return (
    <MainCard sx={{ borderLeft: '4px solid', borderLeftColor: `${severityColor}.main`, mb: 2 }}>
      <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', mb: 2, flexWrap: 'wrap', rowGap: 1 }}>
        <Box sx={{ color: `${severityColor}.main`, fontSize: 20 }}>
          {summaryData.severity === 'NORMAL' ? <CheckCircleOutlined /> : <WarningOutlined />}
        </Box>
        <Typography variant="h5" sx={{ flex: 1 }}>AI 실시간 운영 브리핑</Typography>
        <Chip label={severityLabel} size="small" color={severityColor} />
        {summaryData.aiGenerated && (
          <Typography variant="caption" color="text.secondary">
            AI 분석 · {summaryData.model}
          </Typography>
        )}
        <Button size="small" variant="outlined" onClick={onRefresh} disabled={refreshing}>
          {refreshing ? '분석 중' : '다시 분석'}
        </Button>
      </Stack>

      <Typography variant="h6" color={`${severityColor}.main`} gutterBottom>
        {summaryData.summary}
      </Typography>
      {summaryData.inference && summaryData.inference !== '-' && (
        <Typography variant="body2" color="text.secondary">
          {summaryData.inference}
        </Typography>
      )}
      {summaryData.productionImpact && summaryData.productionImpact !== '-' && (
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
          {summaryData.productionImpact}
        </Typography>
      )}

      {/* 운영 지표 요약 */}
      <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, display: 'block', mt: 2 }}>
        운영 지표
      </Typography>
      <Grid container spacing={0.75} sx={{ mt: 0.25 }}>
        {metrics.map((m) => (
          <Grid key={m.label} size={{ xs: 6, sm: 4, md: 3 }}>
            <Box
              sx={{
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: 1,
                px: 1.25,
                py: 0.5,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between'
              }}
            >
              <Typography variant="caption" color="text.secondary">
                {m.label}
              </Typography>
              <Typography variant="subtitle1" sx={{ fontWeight: 600 }} color={m.warn ? 'warning.main' : 'text.primary'}>
                {m.value}
              </Typography>
            </Box>
          </Grid>
        ))}
      </Grid>

      <Divider sx={{ my: 2 }} />

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 6 }}>
          <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600 }}>
            주요 이슈
          </Typography>
          <Stack direction="row" spacing={0.75} sx={{ flexWrap: 'wrap', gap: 0.75, mt: 0.75 }}>
            {(summaryData.keyIssues || []).slice(0, 6).map((issue, idx) => (
              <Chip key={idx} label={issue} size="small" variant="outlined" color={severityColor} />
            ))}
          </Stack>
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600 }}>
            권장 조치
          </Typography>
          <Stack spacing={0.5} sx={{ mt: 0.75 }}>
            {(summaryData.recommendedActions || []).slice(0, 5).map((action, idx) => (
              <Typography key={idx} variant="body2">{idx + 1}. {action}</Typography>
            ))}
          </Stack>
        </Grid>
      </Grid>

      {/* 최근 주요 이벤트 */}
      {criticalEvents.length > 0 && (
        <>
          <Divider sx={{ my: 2 }} />
          <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, display: 'block' }}>
            최근 주요 이벤트 (최근 1시간 {criticalEvents.length}건)
          </Typography>
          <Stack spacing={0.75} sx={{ mt: 0.75 }}>
            {criticalEvents.slice(0, 5).map((ev, idx) => (
              <Stack key={idx} direction="row" spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap', rowGap: 0.5 }}>
                <Chip label={ev.type} size="small" color={severityColor} variant="outlined" />
                <Typography variant="body2" sx={{ flex: 1, minWidth: 0 }}>
                  {ev.location && ev.location !== '-' ? `${ev.location} · ` : ''}{ev.message}
                </Typography>
                {ev.time && ev.time !== '-' && (
                  <Typography variant="caption" color="text.secondary">{ev.time}</Typography>
                )}
              </Stack>
            ))}
          </Stack>
        </>
      )}
    </MainCard>
  );
}

export default function AiOperations() {
  const { dashboard, isLoading, errors } = useOperationsDashboard();

  const [savedAiSummary, setSavedAiSummary] = useState(() => loadSavedAiSummary());
  const [aiSummaryRefreshing, setAiSummaryRefreshing] = useState(false);
  const [aiSummaryRefreshError, setAiSummaryRefreshError] = useState(null);

  const { data: aiSummaryResponse, error: aiSummaryError, isLoading: aiSummaryLoading } = useSWR(
    savedAiSummary ? null : ['ai-operations-summary'],
    () => aiApi.getSummary(),
    {
      keepPreviousData: true,
      revalidateOnFocus: false,
      revalidateOnReconnect: false,
      revalidateIfStale: false
    }
  );

  const fetchedAiSummary = getApiData(aiSummaryResponse, null);

  useEffect(() => {
    if (!fetchedAiSummary) return;
    saveAiSummary(fetchedAiSummary);
    setSavedAiSummary(fetchedAiSummary);
  }, [fetchedAiSummary]);

  const refreshAiSummary = async () => {
    setAiSummaryRefreshing(true);
    setAiSummaryRefreshError(null);
    try {
      const response = await aiApi.getSummary(true);
      const freshSummary = getApiData(response, null);
      if (freshSummary) {
        saveAiSummary(freshSummary);
        setSavedAiSummary(freshSummary);
      }
    } catch (error) {
      setAiSummaryRefreshError(error);
    } finally {
      setAiSummaryRefreshing(false);
    }
  };

  const aiSummary = savedAiSummary || fetchedAiSummary;
  const isAiSummaryLoading = !aiSummary && aiSummaryLoading;

  return (
    <Stack spacing={3}>
      <Box>
        <Typography variant="h3">AI 운영 분석</Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mt: 0.75 }}>
          AI가 현재 운영 데이터를 해석한 브리핑과 종합 판단을 확인합니다. 세부 현황은 MES/MCS 대시보드에서 봅니다.
        </Typography>
      </Box>

      <AiBriefingSection
        summaryData={aiSummary}
        loading={isAiSummaryLoading}
        error={aiSummaryRefreshError || aiSummaryError}
        refreshing={aiSummaryRefreshing}
        onRefresh={refreshAiSummary}
      />

      {isLoading && <LinearProgress />}
      {errors.map((error, index) => (
        <Alert key={index} severity="error">{error.message}</Alert>
      ))}

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <StatCard title="품질 이상" value={dashboard.defectCount + dashboard.failedInspectionCount} caption={`불량 ${dashboard.defectCount}건 / 검사 부적합 ${dashboard.failedInspectionCount}건`} color={dashboard.defectCount + dashboard.failedInspectionCount > 0 ? 'warning' : 'success'} icon={<WarningOutlined />} />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <StatCard title="재고 확인" value={dashboard.lowStockCount + dashboard.restrictedStockCount} caption={`부족 ${dashboard.lowStockCount}건 / 사용 제한 ${dashboard.restrictedStockCount}건`} color={dashboard.lowStockCount + dashboard.restrictedStockCount > 0 ? 'warning' : 'success'} icon={<AlertOutlined />} />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <StatCard title="설비 이상" value={dashboard.equipmentIssueCount} caption={`비가동 ${dashboard.downEquipmentCount}건 / 확인 필요 ${dashboard.unknownEquipmentCount}건`} color={dashboard.equipmentIssueCount > 0 ? 'warning' : 'success'} icon={<ExclamationCircleOutlined />} />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <StatCard title="이동/신호 문제" value={dashboard.failedTransferCount + dashboard.dataMissingCount} caption={`이동 실패 ${dashboard.failedTransferCount}건 / 데이터 누락 ${dashboard.dataMissingCount}건`} color={dashboard.failedTransferCount + dashboard.dataMissingCount > 0 ? 'error' : 'success'} icon={<ThunderboltOutlined />} />
        </Grid>
      </Grid>

      <MainCard title="데이터 기반 종합 판단">
        <Alert severity={dashboard.defectCount + dashboard.failedInspectionCount + dashboard.lowStockCount + dashboard.equipmentIssueCount + dashboard.failedTransferCount + dashboard.dataMissingCount > 0 ? 'warning' : 'success'}>
          {dashboard.mainCause}
        </Alert>
      </MainCard>
    </Stack>
  );
}
