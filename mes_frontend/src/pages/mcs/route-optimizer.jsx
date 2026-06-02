import { useMemo, useState } from 'react';
import useSWR from 'swr';

import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import CircularProgress from '@mui/material/CircularProgress';
import FormControl from '@mui/material/FormControl';
import Grid from '@mui/material/Grid';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import Select from '@mui/material/Select';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';

import { NodeIndexOutlined } from '@ant-design/icons';

import MainCard from 'components/MainCard';
import { locationApi } from 'api/mcs/locations';
import { mcsReferenceApi } from 'api/mcs/references';
import { routeApi } from 'api/mcs/routes';

const optimizeRules = [
  { value: 'SHORTEST_TIME', label: '최단 시간' },
  { value: 'SHORTEST_DISTANCE', label: '최단 거리' },
  { value: 'AVOID_CONGESTION', label: '혼잡 회피' }
];

const edgeStatusLabels = {
  AVAILABLE: '사용 가능',
  CONGESTED: '혼잡',
  BLOCKED: '막힘',
  INTERLOCKED: '인터락',
  MAINTENANCE: '점검'
};

function getApiData(response, fallback) {
  return response?.data ?? fallback;
}

function getPlantLabel(plant) {
  return `${plant.plantCd}${plant.plantNm ? ` - ${plant.plantNm}` : ''}`;
}

function getLocationLabel(location) {
  return `${location.locationCd}${location.locationNm ? ` - ${location.locationNm}` : ''}`;
}

function getRuleLabel(value) {
  return optimizeRules.find((rule) => rule.value === value)?.label ?? value;
}

function getEdgeStatusLabel(value) {
  return edgeStatusLabels[value] ?? value;
}

function buildRouteText(steps) {
  if (!steps?.length) return '-';
  return [steps[0].fromNodeCd, ...steps.map((step) => step.toNodeCd)].join(' -> ');
}

export default function McsRouteOptimizer() {
  const [form, setForm] = useState({ plantCd: 'P001', fromLocationId: '', toLocationId: '', optimizeRule: 'SHORTEST_TIME' });
  const [result, setResult] = useState(null);
  const [pending, setPending] = useState(false);
  const [message, setMessage] = useState(null);

  const { data: plantResponse, error: plantError, isLoading: plantLoading } = useSWR('mcs-route-plants', () => mcsReferenceApi.plants());
  const plants = getApiData(plantResponse, []);

  const locationParams = useMemo(() => ({ plantCd: form.plantCd, page: 1, size: 200 }), [form.plantCd]);
  const { data: locationResponse, error: locationError, isLoading: locationLoading } = useSWR(['mcs-route-locations', locationParams], () =>
    locationApi.list(locationParams)
  );
  const locations = getApiData(locationResponse, { content: [] }).content ?? [];

  const handleValue = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
    setResult(null);
    setMessage(null);
  };

  const handlePlantChange = (plantCd) => {
    setForm({ plantCd, fromLocationId: '', toLocationId: '', optimizeRule: form.optimizeRule });
    setResult(null);
    setMessage(null);
  };

  const handleOptimize = async () => {
    setPending(true);
    setMessage(null);
    setResult(null);
    try {
      const response = await routeApi.optimize({
        plantCd: form.plantCd,
        fromLocationId: Number(form.fromLocationId),
        toLocationId: Number(form.toLocationId),
        optimizeRule: form.optimizeRule
      });
      setResult(response.data);
      if (!response.data?.routeAvailable) {
        setMessage({ severity: 'warning', text: response.data?.message || '현재 상태에서 이동 가능한 경로가 없습니다.' });
      }
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    } finally {
      setPending(false);
    }
  };

  return (
    <Stack spacing={3}>
      <Box>
        <Typography variant="h3">MCS 경로 최적화</Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mt: 0.75 }}>
          출발지와 도착지를 기준으로 현재 사용 가능한 이동 경로를 계산합니다.
        </Typography>
      </Box>

      <MainCard title="이동 조건">
        <Grid container spacing={2} alignItems="center">
          <Grid size={{ xs: 12, md: 2.4 }}>
            <FormControl fullWidth size="small">
              <InputLabel>공장</InputLabel>
              <Select label="공장" value={form.plantCd} onChange={(event) => handlePlantChange(event.target.value)} disabled={plantLoading}>
                {plants.map((plant) => (
                  <MenuItem key={plant.plantCd} value={plant.plantCd}>
                    {getPlantLabel(plant)}
                  </MenuItem>
                ))}
                {!plants.length && <MenuItem value={form.plantCd}>{form.plantCd}</MenuItem>}
              </Select>
            </FormControl>
          </Grid>
          <Grid size={{ xs: 12, md: 2.8 }}>
            <FormControl fullWidth size="small">
              <InputLabel>출발 위치</InputLabel>
              <Select label="출발 위치" value={form.fromLocationId} onChange={(event) => handleValue('fromLocationId', event.target.value)}>
                <MenuItem value="">선택</MenuItem>
                {locations.map((location) => (
                  <MenuItem key={location.locationId} value={location.locationId}>
                    {getLocationLabel(location)}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid size={{ xs: 12, md: 2.8 }}>
            <FormControl fullWidth size="small">
              <InputLabel>도착 위치</InputLabel>
              <Select label="도착 위치" value={form.toLocationId} onChange={(event) => handleValue('toLocationId', event.target.value)}>
                <MenuItem value="">선택</MenuItem>
                {locations.map((location) => (
                  <MenuItem key={location.locationId} value={location.locationId}>
                    {getLocationLabel(location)}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid size={{ xs: 12, md: 2 }}>
            <FormControl fullWidth size="small">
              <InputLabel>계산 기준</InputLabel>
              <Select label="계산 기준" value={form.optimizeRule} onChange={(event) => handleValue('optimizeRule', event.target.value)}>
                {optimizeRules.map((rule) => (
                  <MenuItem key={rule.value} value={rule.value}>
                    {rule.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid size={{ xs: 12, md: 2 }}>
            <Button
              fullWidth
              variant="contained"
              startIcon={pending ? <CircularProgress size={16} color="inherit" /> : <NodeIndexOutlined />}
              disabled={pending || locationLoading || !form.fromLocationId || !form.toLocationId}
              onClick={handleOptimize}
            >
              경로 계산
            </Button>
          </Grid>
        </Grid>
        {plantError && <Alert severity="error" sx={{ mt: 2 }}>공장 목록 조회 중 오류가 발생했습니다.</Alert>}
        {locationError && <Alert severity="error" sx={{ mt: 2 }}>위치 목록 조회 중 오류가 발생했습니다.</Alert>}
      </MainCard>

      {message && <Alert severity={message.severity}>{message.text}</Alert>}

      {result && (
        <MainCard title="계산 결과">
          <Stack spacing={2.5}>
            <Grid container spacing={2}>
              <Grid size={{ xs: 12, md: 3 }}>
                <Typography variant="caption" color="text.secondary">이동 가능 여부</Typography>
                <Typography variant="h4">{result.routeAvailable ? '가능' : '불가'}</Typography>
              </Grid>
              <Grid size={{ xs: 12, md: 3 }}>
                <Typography variant="caption" color="text.secondary">총 이동 거리</Typography>
                <Typography variant="h4">{result.totalDistanceM}m</Typography>
              </Grid>
              <Grid size={{ xs: 12, md: 3 }}>
                <Typography variant="caption" color="text.secondary">예상 소요 시간</Typography>
                <Typography variant="h4">{result.totalTimeSec}초</Typography>
              </Grid>
              <Grid size={{ xs: 12, md: 3 }}>
                <Typography variant="caption" color="text.secondary">계산 점수</Typography>
                <Typography variant="h4">{result.totalCost}</Typography>
              </Grid>
            </Grid>

            <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
              <Chip label={`기준: ${getRuleLabel(result.optimizeRule)}`} color="primary" variant="light" />
              <Typography variant="body2" color="text.secondary">
                {buildRouteText(result.steps)}
              </Typography>
            </Stack>

            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>순서</TableCell>
                    <TableCell>이동 구간</TableCell>
                    <TableCell>출발 노드</TableCell>
                    <TableCell>도착 노드</TableCell>
                    <TableCell align="right">예상 시간</TableCell>
                    <TableCell>구간 상태</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {(result.steps || []).map((step) => (
                    <TableRow key={`${step.routeEdgeId}-${step.stepSeq}`} hover>
                      <TableCell>{step.stepSeq}</TableCell>
                      <TableCell>
                        <Typography variant="subtitle2">{step.edgeCd}</Typography>
                        <Typography variant="caption" color="text.secondary">
                          {step.fromNodeCd} -&gt; {step.toNodeCd}
                        </Typography>
                      </TableCell>
                      <TableCell>{step.fromNodeCd}</TableCell>
                      <TableCell>{step.toNodeCd}</TableCell>
                      <TableCell align="right">{step.expectedTimeSec}초</TableCell>
                      <TableCell>{getEdgeStatusLabel(step.edgeStatus)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Stack>
        </MainCard>
      )}
    </Stack>
  );
}
