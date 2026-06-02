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
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';

import { ReloadOutlined, SearchOutlined } from '@ant-design/icons';

import MainCard from 'components/MainCard';
import { mcsReferenceApi } from 'api/mcs/references';
import { routeApi } from 'api/mcs/routes';

const edgeStatuses = [
  { value: 'AVAILABLE', label: '사용 가능' },
  { value: 'CONGESTED', label: '혼잡' },
  { value: 'BLOCKED', label: '막힘' },
  { value: 'INTERLOCKED', label: '인터락' },
  { value: 'MAINTENANCE', label: '점검' }
];

const nodeTypeLabels = {
  LOCATION: '로케이션',
  CONVEYOR: '컨베이어',
  BUFFER: '버퍼',
  LIFT: '리프트',
  GATE: '게이트'
};

function getApiData(response, fallback) {
  return response?.data ?? fallback;
}

function getPlantLabel(plant) {
  return `${plant.plantCd}${plant.plantNm ? ` - ${plant.plantNm}` : ''}`;
}

function getEdgeColor(status) {
  if (status === 'AVAILABLE') return 'success';
  if (status === 'CONGESTED') return 'warning';
  if (status === 'BLOCKED' || status === 'INTERLOCKED') return 'error';
  return 'default';
}

function getEdgeStatusLabel(value) {
  return edgeStatuses.find((status) => status.value === value)?.label ?? value;
}

function getNodeTypeLabel(value) {
  return nodeTypeLabels[value] ?? value;
}

export default function McsRouteManagement() {
  const [search, setSearch] = useState({ plantCd: 'P001', nodeCd: '', edgeCd: '', edgeStatus: '' });
  const [query, setQuery] = useState({ plantCd: 'P001' });
  const [pendingEdge, setPendingEdge] = useState(null);
  const [message, setMessage] = useState(null);

  const { data: plantResponse, error: plantError, isLoading: plantLoading } = useSWR('mcs-route-management-plants', () => mcsReferenceApi.plants());
  const plants = getApiData(plantResponse, []);

  const params = useMemo(() => ({ ...query }), [query]);
  const { data: nodeResponse, error: nodeError, isLoading: nodeLoading, mutate: mutateNodes } = useSWR(['mcs-route-nodes', params], () => routeApi.nodes(params));
  const { data: edgeResponse, error: edgeError, isLoading: edgeLoading, mutate: mutateEdges } = useSWR(['mcs-route-edges', params], () => routeApi.edges(params));

  const nodes = getApiData(nodeResponse, []);
  const edges = getApiData(edgeResponse, []);

  const handleSearchValue = (field, value) => {
    setSearch((current) => ({ ...current, [field]: value }));
  };

  const handleSearch = () => {
    setQuery({ ...search });
  };

  const handleReset = () => {
    setSearch({ plantCd: 'P001', nodeCd: '', edgeCd: '', edgeStatus: '' });
    setQuery({ plantCd: 'P001' });
  };

  const handleChangeStatus = async (edge, status) => {
    setPendingEdge(`${edge.routeEdgeId}-${status}`);
    setMessage(null);
    try {
      await routeApi.changeEdgeStatus(edge.routeEdgeId, status);
      await mutateEdges();
      setMessage({ severity: 'success', text: `${edge.edgeCd} 구간 상태를 ${getEdgeStatusLabel(status)}으로 변경했습니다.` });
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    } finally {
      setPendingEdge(null);
    }
  };

  const handleReload = () => {
    mutateNodes();
    mutateEdges();
  };

  return (
    <Stack spacing={3}>
      <Box>
        <Typography variant="h3">MCS 경로 관리</Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mt: 0.75 }}>
          이동 경로를 구성하는 지점과 구간 상태를 관리합니다.
        </Typography>
      </Box>

      <MainCard title="검색 조건">
        <Grid container spacing={2} alignItems="center">
          <Grid size={{ xs: 12, md: 2.4 }}>
            <FormControl fullWidth size="small">
              <InputLabel>공장</InputLabel>
              <Select label="공장" value={search.plantCd} onChange={(event) => handleSearchValue('plantCd', event.target.value)} disabled={plantLoading}>
                {plants.map((plant) => (
                  <MenuItem key={plant.plantCd} value={plant.plantCd}>
                    {getPlantLabel(plant)}
                  </MenuItem>
                ))}
                {!plants.length && <MenuItem value={search.plantCd}>{search.plantCd}</MenuItem>}
              </Select>
            </FormControl>
          </Grid>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <TextField fullWidth size="small" label="지점 코드" value={search.nodeCd} onChange={(event) => handleSearchValue('nodeCd', event.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <TextField fullWidth size="small" label="구간 코드" value={search.edgeCd} onChange={(event) => handleSearchValue('edgeCd', event.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <FormControl fullWidth size="small">
              <InputLabel>구간 상태</InputLabel>
              <Select label="구간 상태" value={search.edgeStatus} onChange={(event) => handleSearchValue('edgeStatus', event.target.value)}>
                <MenuItem value="">전체</MenuItem>
                {edgeStatuses.map((status) => (
                  <MenuItem key={status.value} value={status.value}>
                    {status.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <Stack direction="row" spacing={1} justifyContent="flex-end">
              <Button variant="outlined" startIcon={<ReloadOutlined />} onClick={handleReset}>
                초기화
              </Button>
              <Button variant="contained" startIcon={<SearchOutlined />} onClick={handleSearch}>
                조회
              </Button>
            </Stack>
          </Grid>
        </Grid>
        {plantError && <Alert severity="error" sx={{ mt: 2 }}>공장 목록 조회 중 오류가 발생했습니다.</Alert>}
      </MainCard>

      {message && <Alert severity={message.severity}>{message.text}</Alert>}

      <Grid container spacing={2.5}>
        <Grid size={{ xs: 12, xl: 4 }}>
          <MainCard title="경로 지점" secondary={<Button size="small" startIcon={<ReloadOutlined />} onClick={handleReload}>새로고침</Button>}>
            {nodeError && <Alert severity="error">경로 지점 조회 중 오류가 발생했습니다.</Alert>}
            {nodeLoading ? (
              <Stack alignItems="center" sx={{ py: 4 }}>
                <CircularProgress size={24} />
              </Stack>
            ) : (
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>지점</TableCell>
                      <TableCell>유형</TableCell>
                      <TableCell>연결 Location</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {nodes.map((node) => (
                      <TableRow key={node.routeNodeId} hover>
                        <TableCell>
                          <Typography variant="subtitle2">{node.nodeCd}</Typography>
                          <Typography variant="caption" color="text.secondary">{node.nodeNm}</Typography>
                        </TableCell>
                        <TableCell>{getNodeTypeLabel(node.nodeType)}</TableCell>
                        <TableCell>{node.locationCd || '-'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </MainCard>
        </Grid>

        <Grid size={{ xs: 12, xl: 8 }}>
          <MainCard title="이동 구간">
            {edgeError && <Alert severity="error">이동 구간 조회 중 오류가 발생했습니다.</Alert>}
            {edgeLoading ? (
              <Stack alignItems="center" sx={{ py: 4 }}>
                <CircularProgress size={24} />
              </Stack>
            ) : (
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>구간</TableCell>
                      <TableCell>출발 지점</TableCell>
                      <TableCell>도착 지점</TableCell>
                      <TableCell align="right">예상 시간</TableCell>
                      <TableCell>상태</TableCell>
                      <TableCell align="right">상태 변경</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {edges.map((edge) => (
                      <TableRow key={edge.routeEdgeId} hover>
                        <TableCell>
                          <Typography variant="subtitle2">{edge.edgeCd}</Typography>
                          <Typography variant="caption" color="text.secondary">{edge.edgeNm}</Typography>
                        </TableCell>
                        <TableCell>{edge.fromNodeCd}</TableCell>
                        <TableCell>{edge.toNodeCd}</TableCell>
                        <TableCell align="right">{edge.travelTimeSec}초</TableCell>
                        <TableCell>
                          <Chip size="small" color={getEdgeColor(edge.edgeStatus)} label={getEdgeStatusLabel(edge.edgeStatus)} variant="light" />
                        </TableCell>
                        <TableCell align="right">
                          <Stack direction="row" spacing={1} justifyContent="flex-end">
                            {edgeStatuses.slice(0, 3).map((status) => (
                              <Button
                                key={status.value}
                                size="small"
                                variant={edge.edgeStatus === status.value ? 'contained' : 'outlined'}
                                disabled={Boolean(pendingEdge)}
                                onClick={() => handleChangeStatus(edge, status.value)}
                              >
                                {pendingEdge === `${edge.routeEdgeId}-${status.value}` ? <CircularProgress size={16} /> : status.label}
                              </Button>
                            ))}
                          </Stack>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </MainCard>
        </Grid>
      </Grid>
    </Stack>
  );
}
