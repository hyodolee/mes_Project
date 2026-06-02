import { useMemo, useState } from 'react';
import useSWR from 'swr';

import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import CircularProgress from '@mui/material/CircularProgress';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Divider from '@mui/material/Divider';
import Drawer from '@mui/material/Drawer';
import FormControl from '@mui/material/FormControl';
import Grid from '@mui/material/Grid';
import IconButton from '@mui/material/IconButton';
import InputLabel from '@mui/material/InputLabel';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import MenuItem from '@mui/material/MenuItem';
import Select from '@mui/material/Select';
import Snackbar from '@mui/material/Snackbar';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TablePagination from '@mui/material/TablePagination';
import TableRow from '@mui/material/TableRow';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';

import { BulbOutlined, CloseOutlined, PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons';

import MainCard from 'components/MainCard';
import { mesEquipmentApi } from 'api/mes/equipment';
import { mesMasterApi } from 'api/mes/master';
import { mesPlanningApi } from 'api/mes/planning';
import { mesWorkOrderApi } from 'api/mes/workOrders';

const emptyForm = {
  plantCd: '',
  planId: '',
  woDt: '',
  itemCd: '',
  woQty: 1,
  workcenterCd: '',
  equipmentCd: '',
  workerId: '',
  planStartDtm: '',
  planEndDtm: '',
  priority: 3,
  lotNo: '',
  orderNo: '',
  deliveryDt: '',
  woRmk: ''
};

const emptyMaterialForm = {
  itemCd: '',
  transferQty: 1,
  optimizeRule: 'AVOID_CONGESTION',
  requestReason: ''
};

function getApiData(response, fallback) {
  return response?.data ?? fallback;
}

function getStatusColor(status) {
  if (status === '완료' || status === 'COMPLETED') return 'success';
  if (status === '진행' || status === 'IN_PROGRESS') return 'info';
  if (status === '취소' || status === 'CANCELLED') return 'default';
  return 'primary';
}

function getMaterialStatusColor(status) {
  if (status === 'COMPLETED') return 'success';
  if (status === 'IN_PROGRESS') return 'info';
  if (status === 'REQUESTED') return 'warning';
  if (status === 'FAILED') return 'error';
  if (status === 'CANCELLED') return 'default';
  return 'default';
}

function getMaterialStatusView(materialStatus) {
  if (!materialStatus?.requested) {
    return {
      label: 'MCS 자재 이동 요청 전',
      detail: '자재 요청 후 MCS가 LOT, 출발/도착 Location, 이동 경로를 자동 배정합니다.',
      color: 'default',
      canStart: false,
      startTitle: '먼저 MCS 자재 요청을 생성하세요.'
    };
  }

  const status = materialStatus.transferStatus;
  if (status === 'REQUESTED') {
    return {
      label: 'MCS 이동 요청됨',
      detail: 'PLC 이동 시작 이벤트를 기다리고 있습니다.',
      color: 'warning',
      canStart: false,
      startTitle: 'PLC 이동 완료 후 작업을 시작할 수 있습니다.'
    };
  }
  if (status === 'IN_PROGRESS') {
    return {
      label: 'PLC 이동 중',
      detail: 'MCS가 자재 이동을 진행 중입니다.',
      color: 'info',
      canStart: false,
      startTitle: 'MCS 자재 이동 완료 후 작업을 시작할 수 있습니다.'
    };
  }
  if (status === 'COMPLETED') {
    return {
      label: 'MCS 이동 완료 - 작업 시작 가능',
      detail: '자재 이동이 완료되어 MES 작업을 시작할 수 있습니다.',
      color: 'success',
      canStart: true,
      startTitle: '작업 시작'
    };
  }
  if (status === 'FAILED') {
    return {
      label: 'MCS 이동 실패 - 취소 후 재요청 필요',
      detail: 'MCS에서 실패 이동오더를 취소한 뒤 MES에서 자재 요청을 다시 생성하세요.',
      color: 'error',
      canStart: false,
      startTitle: 'MCS 자재 이동 실패 상태입니다. 취소 후 재요청이 필요합니다.'
    };
  }
  if (status === 'CANCELLED') {
    return {
      label: 'MCS 이동 취소됨',
      detail: '새 자재 요청을 생성할 수 있습니다.',
      color: 'default',
      canStart: false,
      startTitle: '새 MCS 자재 요청이 필요합니다.'
    };
  }

  return {
    label: materialStatus.transferStatusNm || materialStatus.message || 'MCS 상태 확인 필요',
    detail: materialStatus.message || 'MCS 자재 이동 상태를 확인하세요.',
    color: getMaterialStatusColor(status),
    canStart: false,
    startTitle: 'MCS 자재 이동 완료 후 작업을 시작할 수 있습니다.'
  };
}

function today() {
  return new Date().toISOString().slice(0, 10);
}

function currentDateTimeLocal(hourOffset = 0) {
  const date = new Date();
  date.setHours(date.getHours() + hourOffset);
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
}

function canStart(status) {
  return status === '대기' || status === 'PLANNED';
}

function canFinish(status) {
  return status === '진행' || status === 'IN_PROGRESS';
}

export default function MesWorkOrders() {
  const [search, setSearch] = useState({ plantCd: '', itemCd: '', woStatus: '', woFromDt: '', woToDt: '' });
  const [query, setQuery] = useState(search);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [materialDialogOpen, setMaterialDialogOpen] = useState(false);
  const [materialOrder, setMaterialOrder] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [materialForm, setMaterialForm] = useState(emptyMaterialForm);
  const [message, setMessage] = useState(null);
  const [pendingAction, setPendingAction] = useState(null);
  const [analysisDrawerOpen, setAnalysisDrawerOpen] = useState(false);
  const [analysisOrder, setAnalysisOrder] = useState(null);
  const [analysis, setAnalysis] = useState(null);
  const [analysisLoading, setAnalysisLoading] = useState(false);
  const [analysisError, setAnalysisError] = useState('');

  const { data: workOrderResponse, error: workOrderError, isLoading, mutate } = useSWR(['mes-work-orders', query], () => mesWorkOrderApi.list(query));
  const { data: plantResponse } = useSWR('mes-plants', () => mesMasterApi.plants({ useYn: 'Y' }));
  const { data: itemResponse } = useSWR('mes-items', () => mesMasterApi.items({ useYn: 'Y', itemNm: '' }));
  const { data: planResponse } = useSWR('mes-prod-plan-options', () => mesPlanningApi.prodPlans({}));
  const { data: workcenterResponse } = useSWR(['mes-workcenter-options', form.plantCd], () =>
    mesEquipmentApi.workcenters({ plantCd: form.plantCd || undefined })
  );
  const { data: equipmentResponse } = useSWR(['mes-equipment-options', form.plantCd, form.workcenterCd], () =>
    mesEquipmentApi.options({ plantCd: form.plantCd || undefined, workcenterCd: form.workcenterCd || undefined })
  );
  const { data: workerResponse } = useSWR(['mes-worker-options', form.plantCd], () =>
    mesEquipmentApi.workers({ plantCd: form.plantCd || undefined })
  );
  const workOrders = getApiData(workOrderResponse, []);
  const plants = getApiData(plantResponse, []);
  const items = getApiData(itemResponse, []);
  const plans = getApiData(planResponse, []);
  const workcenters = getApiData(workcenterResponse, []);
  const equipmentOptions = getApiData(equipmentResponse, []);
  const workerOptions = getApiData(workerResponse, []);
  const isBusy = Boolean(pendingAction);

  const visibleWorkOrders = useMemo(() => {
    const start = page * rowsPerPage;
    return workOrders.slice(start, start + rowsPerPage);
  }, [page, rowsPerPage, workOrders]);
  const visibleWorkOrderIds = useMemo(() => visibleWorkOrders.map((order) => order.woId), [visibleWorkOrders]);
  const { data: materialStatusResponse, mutate: mutateMaterialStatuses } = useSWR(
    visibleWorkOrderIds.length ? ['mes-work-order-material-statuses', visibleWorkOrderIds] : null,
    async () => {
      const responses = await Promise.all(
        visibleWorkOrderIds.map((woId) => mesWorkOrderApi.materialTransferStatus(woId))
      );
      return responses.map((response) => getApiData(response, null)).filter(Boolean);
    }
  );
  const materialStatusMap = useMemo(() => {
    const entries = materialStatusResponse || [];
    return entries.reduce((acc, status) => {
      acc[status.woId] = status;
      return acc;
    }, {});
  }, [materialStatusResponse]);

  const isWorkOrderFormValid =
    Boolean(form.plantCd) &&
    Boolean(form.itemCd) &&
    Boolean(form.woDt) &&
    Number(form.woQty || 0) > 0 &&
    Boolean(form.workcenterCd?.trim()) &&
    Boolean(form.planStartDtm) &&
    Boolean(form.planEndDtm);

  const openCreateDialog = () => {
    setForm({
      ...emptyForm,
      woDt: today(),
      planStartDtm: currentDateTimeLocal(),
      planEndDtm: currentDateTimeLocal(8)
    });
    setDialogOpen(true);
  };

  const openMaterialDialog = (order) => {
    setMaterialOrder(order);
    setMaterialForm({
      ...emptyMaterialForm,
      itemCd: order.itemCd || '',
      transferQty: order.woQty || 1,
      requestReason: `${order.woNo} 작업 투입 자재 요청`
    });
    setMaterialDialogOpen(true);
  };

  const handleSearchValue = (field, value) => setSearch((current) => ({ ...current, [field]: value }));
  const handleFormValue = (field, value) => {
    setForm((current) => {
      const next = { ...current, [field]: value };
      if (field === 'planId' && value) {
        const plan = plans.find((candidate) => String(candidate.planId) === String(value));
        if (plan) {
          next.plantCd = plan.plantCd || next.plantCd;
          next.itemCd = plan.itemCd || next.itemCd;
          next.woQty = plan.planQty || next.woQty;
          next.orderNo = plan.orderNo || next.orderNo;
          next.deliveryDt = plan.deliveryDt || next.deliveryDt;
        }
      }
      if (field === 'plantCd') {
        next.workcenterCd = '';
        next.equipmentCd = '';
        next.workerId = '';
      }
      if (field === 'workcenterCd') {
        next.equipmentCd = '';
      }
      return next;
    });
  };
  const handleMaterialValue = (field, value) =>
    setMaterialForm((current) => ({ ...current, [field]: value }));

  const handleSearch = () => {
    setPage(0);
    setQuery(search);
  };

  const handleReset = () => {
    const next = { plantCd: '', itemCd: '', woStatus: '', woFromDt: '', woToDt: '' };
    setSearch(next);
    setQuery(next);
    setPage(0);
  };

  const handleSave = async () => {
    if (!isWorkOrderFormValid) {
      setMessage({ severity: 'warning', text: '공장, 품목, 작업장, 지시수량, 작업일, 계획 일시는 필수입니다.' });
      return;
    }

    try {
      await mesWorkOrderApi.create({
        ...form,
        planId: form.planId ? Number(form.planId) : null,
        woQty: Number(form.woQty || 0),
        priority: Number(form.priority || 0),
        equipmentCd: form.equipmentCd || null,
        workerId: form.workerId || null,
        lotNo: null,
        orderNo: form.orderNo || null,
        deliveryDt: form.deliveryDt || null
      });
      setMessage({ severity: 'success', text: '작업 오더가 등록되었습니다.' });
      setDialogOpen(false);
      await mutate();
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    }
  };

  const handleStatus = async (order, nextStatus) => {
    const actionKey = `status-${order.woId}-${nextStatus}`;
    setPendingAction(actionKey);
    try {
      await mesWorkOrderApi.changeStatus(order.woId, nextStatus);
      setMessage({ severity: 'success', text: '작업 오더 상태가 변경되었습니다.' });
      await mutate();
      await mutateMaterialStatuses();
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    } finally {
      setPendingAction(null);
    }
  };

  const handleMaterialTransfer = async () => {
    if (!materialOrder || isBusy) return;

    setPendingAction('material-transfer');
    try {
      const response = await mesWorkOrderApi.requestMaterialTransfer(materialOrder.woId, {
        itemCd: materialForm.itemCd,
        optimizeRule: materialForm.optimizeRule,
        requestReason: materialForm.requestReason,
        transferQty: Number(materialForm.transferQty || 0)
      });
      const result = getApiData(response, {});
      setMessage({ severity: 'success', text: `MCS가 자재 이동오더를 자동 배정했습니다. ${result.transferNo || ''}` });
      setMaterialDialogOpen(false);
      await mutateMaterialStatuses();
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    } finally {
      setPendingAction(null);
    }
  };

  const handleAiAnalysis = async (order) => {
    setAnalysisDrawerOpen(true);
    setAnalysisOrder(order);
    setAnalysis(null);
    setAnalysisError('');
    setAnalysisLoading(true);
    try {
      const response = await mesWorkOrderApi.aiAnalysis(order.woId);
      setAnalysis(getApiData(response, null));
    } catch (error) {
      setAnalysisError(error.message);
    } finally {
      setAnalysisLoading(false);
    }
  };

  const renderAnalysisList = (title, items, emptyText) => (
    <Box>
      <Typography variant="subtitle1" sx={{ mb: 0.75 }}>{title}</Typography>
      {items?.length ? (
        <List dense disablePadding>
          {items.map((item, index) => (
            <ListItem key={`${title}-${index}`} sx={{ px: 0, py: 0.35, alignItems: 'flex-start' }}>
              <ListItemText
                primary={item}
                primaryTypographyProps={{ variant: 'body2', color: 'text.secondary' }}
              />
            </ListItem>
          ))}
        </List>
      ) : (
        <Typography variant="body2" color="text.secondary">{emptyText}</Typography>
      )}
    </Box>
  );

  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ justifyContent: 'space-between', alignItems: { xs: 'flex-start', md: 'center' } }}>
        <Box>
          <Typography variant="h3">MES 작업 오더</Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mt: 0.75 }}>
            생산 작업 지시의 진행 상태를 조회하고 신규 작업 오더를 등록합니다.
          </Typography>
        </Box>
        <Button variant="contained" startIcon={<PlusOutlined />} onClick={openCreateDialog}>
          작업 오더 등록
        </Button>
      </Stack>

      <MainCard title="검색 조건">
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <FormControl fullWidth size="small">
              <InputLabel>공장</InputLabel>
              <Select label="공장" value={search.plantCd} onChange={(event) => handleSearchValue('plantCd', event.target.value)}>
                <MenuItem value="">전체</MenuItem>
                {plants.map((plant) => <MenuItem key={plant.plantCd} value={plant.plantCd}>{plant.plantNm}</MenuItem>)}
              </Select>
            </FormControl>
          </Grid>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <FormControl fullWidth size="small">
              <InputLabel>품목</InputLabel>
              <Select label="품목" value={search.itemCd} onChange={(event) => handleSearchValue('itemCd', event.target.value)}>
                <MenuItem value="">전체</MenuItem>
                {items.slice(0, 100).map((item) => <MenuItem key={`${item.plantCd}-${item.itemCd}`} value={item.itemCd}>{item.itemCd} - {item.itemNm}</MenuItem>)}
              </Select>
            </FormControl>
          </Grid>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <FormControl fullWidth size="small">
              <InputLabel>상태</InputLabel>
              <Select label="상태" value={search.woStatus} onChange={(event) => handleSearchValue('woStatus', event.target.value)}>
                <MenuItem value="">전체</MenuItem>
                <MenuItem value="대기">대기</MenuItem>
                <MenuItem value="진행">진행</MenuItem>
                <MenuItem value="완료">완료</MenuItem>
                <MenuItem value="취소">취소</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <TextField fullWidth size="small" type="date" label="From" value={search.woFromDt} onChange={(event) => handleSearchValue('woFromDt', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
          </Grid>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <TextField fullWidth size="small" type="date" label="To" value={search.woToDt} onChange={(event) => handleSearchValue('woToDt', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
          </Grid>
          <Grid size={12}>
            <Stack direction="row" spacing={1} sx={{ justifyContent: 'flex-end' }}>
              <Button variant="outlined" startIcon={<ReloadOutlined />} onClick={handleReset}>초기화</Button>
              <Button variant="contained" startIcon={<SearchOutlined />} onClick={handleSearch}>조회</Button>
            </Stack>
          </Grid>
        </Grid>
      </MainCard>

      <MainCard title="작업 오더 목록" content={false}>
        {workOrderError && <Alert severity="error">{workOrderError.message}</Alert>}
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>작업번호</TableCell>
                <TableCell>공장</TableCell>
                <TableCell>작업일</TableCell>
                <TableCell>품목</TableCell>
                <TableCell align="right">지시수량</TableCell>
                <TableCell align="right">양품</TableCell>
                <TableCell align="right">불량</TableCell>
                <TableCell>설비</TableCell>
                <TableCell>상태</TableCell>
                <TableCell>MCS 자재 이동</TableCell>
                <TableCell>LOT</TableCell>
                <TableCell align="right">관리</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {!isLoading && workOrders.length === 0 && (
                <TableRow>
                  <TableCell colSpan={12} align="center">조회된 작업 오더가 없습니다.</TableCell>
                </TableRow>
              )}
              {visibleWorkOrders.map((order) => {
                const materialStatus = materialStatusMap[order.woId];
                const materialRequested = Boolean(materialStatus?.requested);
                const materialView = getMaterialStatusView(materialStatus);
                const startDisabled = isBusy || !materialView.canStart;

                return (
                  <TableRow key={order.woId} hover>
                    <TableCell>{order.woNo}</TableCell>
                    <TableCell>{order.plantNm || order.plantCd}</TableCell>
                    <TableCell>{order.woDt}</TableCell>
                    <TableCell>
                      <Typography variant="subtitle2">{order.itemNm || order.itemCd}</Typography>
                      <Typography variant="caption" color="text.secondary">{order.itemCd}</Typography>
                    </TableCell>
                    <TableCell align="right">{order.woQty}</TableCell>
                    <TableCell align="right">{order.goodQty || 0}</TableCell>
                    <TableCell align="right">{order.defectQty || 0}</TableCell>
                    <TableCell>{order.equipmentCd || '-'}</TableCell>
                    <TableCell><Chip label={order.woStatus} size="small" color={getStatusColor(order.woStatus)} variant="light" /></TableCell>
                    <TableCell>
                      <Stack spacing={0.5}>
                        <Chip
                          label={materialView.label}
                          size="small"
                          color={materialView.color}
                          variant={materialRequested ? 'light' : 'outlined'}
                          sx={{ width: 'fit-content' }}
                        />
                        <Typography variant="caption" color={materialView.color === 'error' ? 'error.main' : 'text.secondary'}>
                          {materialView.detail}
                        </Typography>
                        {materialStatus?.transferNo && (
                          <Typography variant="caption" color="text.secondary">
                            {materialStatus.transferNo}
                          </Typography>
                        )}
                      </Stack>
                    </TableCell>
                    <TableCell>{order.lotNo || '-'}</TableCell>
                    <TableCell align="right">
                      <Stack direction="row" spacing={1} sx={{ justifyContent: 'flex-end' }}>
                        <Button
                          size="small"
                          variant="outlined"
                          startIcon={<BulbOutlined />}
                          onClick={() => handleAiAnalysis(order)}
                        >
                          AI 분석
                        </Button>
                        <Button
                          size="small"
                          variant="outlined"
                          disabled={isBusy || materialRequested}
                          onClick={() => openMaterialDialog(order)}
                        >
                          자재 요청
                        </Button>
                        {canStart(order.woStatus) && (
                          <Button
                            size="small"
                            title={materialView.startTitle}
                            disabled={startDisabled}
                            startIcon={pendingAction === `status-${order.woId}-진행` ? <CircularProgress size={14} color="inherit" /> : undefined}
                            onClick={() => handleStatus(order, '진행')}
                          >
                            시작
                          </Button>
                        )}
                        {canFinish(order.woStatus) && (
                          <Button
                            size="small"
                            color="success"
                            disabled={isBusy}
                            startIcon={pendingAction === `status-${order.woId}-완료` ? <CircularProgress size={14} color="inherit" /> : undefined}
                            onClick={() => handleStatus(order, '완료')}
                          >
                            완료
                          </Button>
                        )}
                        {(canStart(order.woStatus) || canFinish(order.woStatus)) && (
                          <Button
                            size="small"
                            color="error"
                            disabled={isBusy}
                            startIcon={pendingAction === `status-${order.woId}-취소` ? <CircularProgress size={14} color="inherit" /> : undefined}
                            onClick={() => handleStatus(order, '취소')}
                          >
                            취소
                          </Button>
                        )}
                      </Stack>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </TableContainer>
        <TablePagination
          component="div"
          count={workOrders.length}
          page={page}
          rowsPerPage={rowsPerPage}
          rowsPerPageOptions={[10, 20, 50]}
          onPageChange={(_, nextPage) => setPage(nextPage)}
          onRowsPerPageChange={(event) => {
            setRowsPerPage(Number(event.target.value));
            setPage(0);
          }}
        />
      </MainCard>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth maxWidth="md">
        <DialogTitle>작업 오더 등록</DialogTitle>
        <DialogContent dividers>
          <Grid container spacing={2} sx={{ pt: 0.5 }}>
            <Grid size={{ xs: 12, md: 6 }}>
              <FormControl fullWidth size="small">
                <InputLabel>생산 계획</InputLabel>
                <Select label="생산 계획" value={form.planId} onChange={(event) => handleFormValue('planId', event.target.value)}>
                  <MenuItem value="">계획 미연결</MenuItem>
                  {plans.slice(0, 100).map((plan) => <MenuItem key={plan.planId} value={plan.planId}>{plan.planNo} - {plan.itemNm || plan.itemCd}</MenuItem>)}
                </Select>
              </FormControl>
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <FormControl fullWidth size="small" required>
                <InputLabel>공장</InputLabel>
                <Select label="공장" value={form.plantCd} onChange={(event) => handleFormValue('plantCd', event.target.value)}>
                  {plants.map((plant) => <MenuItem key={plant.plantCd} value={plant.plantCd}>{plant.plantNm}</MenuItem>)}
                </Select>
              </FormControl>
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <FormControl fullWidth size="small" required>
                <InputLabel>품목</InputLabel>
                <Select label="품목" value={form.itemCd} onChange={(event) => handleFormValue('itemCd', event.target.value)}>
                  {items.slice(0, 100).map((item) => <MenuItem key={`${item.plantCd}-${item.itemCd}`} value={item.itemCd}>{item.itemCd} - {item.itemNm}</MenuItem>)}
                </Select>
              </FormControl>
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField fullWidth required size="small" type="number" label="지시수량" value={form.woQty} onChange={(event) => handleFormValue('woQty', event.target.value)} />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField fullWidth required size="small" type="date" label="작업일" value={form.woDt} onChange={(event) => handleFormValue('woDt', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField fullWidth size="small" type="number" label="우선순위" value={form.priority} onChange={(event) => handleFormValue('priority', event.target.value)} />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField fullWidth required size="small" type="datetime-local" label="계획 시작" value={form.planStartDtm} onChange={(event) => handleFormValue('planStartDtm', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField fullWidth required size="small" type="datetime-local" label="계획 종료" value={form.planEndDtm} onChange={(event) => handleFormValue('planEndDtm', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <FormControl fullWidth size="small" required>
                <InputLabel>작업장</InputLabel>
                <Select label="작업장" value={form.workcenterCd} onChange={(event) => handleFormValue('workcenterCd', event.target.value)}>
                  {workcenters.map((workcenter) => (
                    <MenuItem key={workcenter.workcenterCd} value={workcenter.workcenterCd}>
                      {workcenter.workcenterCd} - {workcenter.workcenterNm}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <FormControl fullWidth size="small" disabled={!form.workcenterCd}>
                <InputLabel>설비</InputLabel>
                <Select label="설비" value={form.equipmentCd} onChange={(event) => handleFormValue('equipmentCd', event.target.value)}>
                  <MenuItem value="">미배정</MenuItem>
                  {equipmentOptions.map((equipment) => (
                    <MenuItem key={equipment.equipmentCd} value={equipment.equipmentCd}>
                      {equipment.equipmentCd} - {equipment.equipmentNm}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <FormControl fullWidth size="small">
                <InputLabel>작업자</InputLabel>
                <Select label="작업자" value={form.workerId} onChange={(event) => handleFormValue('workerId', event.target.value)}>
                  <MenuItem value="">미배정</MenuItem>
                  {workerOptions.map((worker) => (
                    <MenuItem key={worker.workerId} value={worker.workerId}>
                      {worker.workerId} - {worker.workerNm}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField
                fullWidth
                disabled
                size="small"
                label="생산 LOT 번호"
                value="저장 시 자동 생성"
                helperText="작업일과 품목 기준으로 시스템이 생성합니다."
              />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField fullWidth size="small" label="수주번호" value={form.orderNo} onChange={(event) => handleFormValue('orderNo', event.target.value)} />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField fullWidth size="small" type="date" label="납기" value={form.deliveryDt} onChange={(event) => handleFormValue('deliveryDt', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
            </Grid>
            <Grid size={12}>
              <TextField fullWidth multiline minRows={3} label="비고" value={form.woRmk} onChange={(event) => handleFormValue('woRmk', event.target.value)} />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>취소</Button>
          <Button variant="contained" disabled={!isWorkOrderFormValid} onClick={handleSave}>저장</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={materialDialogOpen} onClose={() => setMaterialDialogOpen(false)} fullWidth maxWidth="md">
        <DialogTitle>MCS 자재 요청</DialogTitle>
        <DialogContent dividers>
          <Stack spacing={2}>
            {materialOrder && (
              <Alert severity="info" variant="outlined">
                {materialOrder.woNo} 작업에 필요한 자재를 MCS에 요청합니다.
              </Alert>
            )}
            <Grid container spacing={2}>
              <Grid size={{ xs: 12, md: 6 }}>
                <FormControl fullWidth size="small" required>
                  <InputLabel>요청 품목</InputLabel>
                  <Select label="요청 품목" value={materialForm.itemCd} onChange={(event) => handleMaterialValue('itemCd', event.target.value)}>
                    {items.filter((item) => !materialOrder?.plantCd || item.plantCd === materialOrder.plantCd).slice(0, 100).map((item) => (
                      <MenuItem key={`${item.plantCd}-${item.itemCd}`} value={item.itemCd}>
                        {item.itemCd} - {item.itemNm}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
              <Grid size={{ xs: 12, md: 6 }}>
                <TextField fullWidth required size="small" type="number" label="요청 수량" value={materialForm.transferQty} onChange={(event) => handleMaterialValue('transferQty', event.target.value)} />
              </Grid>
              <Grid size={{ xs: 12 }}>
                <TextField fullWidth size="small" label="요청 사유" value={materialForm.requestReason} onChange={(event) => handleMaterialValue('requestReason', event.target.value)} />
              </Grid>
              <Grid size={{ xs: 12 }}>
                <Alert severity="info" variant="outlined">
                  출발 로케이션, LOT, 도착 로케이션, 이동 경로는 MCS가 가용 재고와 경로 상태를 기준으로 자동 배정합니다.
                </Alert>
              </Grid>
            </Grid>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setMaterialDialogOpen(false)}>취소</Button>
          <Button
            variant="contained"
            startIcon={pendingAction === 'material-transfer' ? <CircularProgress size={14} color="inherit" /> : undefined}
            disabled={isBusy || !materialForm.itemCd || Number(materialForm.transferQty || 0) <= 0}
            onClick={handleMaterialTransfer}
          >
            MCS 자재 요청
          </Button>
        </DialogActions>
      </Dialog>

      <Drawer
        anchor="right"
        open={analysisDrawerOpen}
        onClose={() => setAnalysisDrawerOpen(false)}
        PaperProps={{ sx: { width: { xs: '100%', sm: 460 }, maxWidth: '100%' } }}
      >
        <Box sx={{ p: 2.5 }}>
          <Stack direction="row" spacing={1.5} sx={{ alignItems: 'flex-start', justifyContent: 'space-between' }}>
            <Box>
              <Typography variant="h4">AI 작업오더 분석</Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                {analysisOrder?.woNo || '-'} / {analysisOrder?.itemNm || analysisOrder?.itemCd || '-'}
              </Typography>
            </Box>
            <IconButton size="small" onClick={() => setAnalysisDrawerOpen(false)}>
              <CloseOutlined />
            </IconButton>
          </Stack>

          <Divider sx={{ my: 2 }} />

          {analysisLoading && (
            <Stack spacing={1.5} sx={{ alignItems: 'center', py: 7 }}>
              <CircularProgress />
              <Typography variant="body2" color="text.secondary">MES/MCS/PLC 데이터를 모아 AI 분석 중입니다.</Typography>
            </Stack>
          )}

          {!analysisLoading && analysisError && (
            <Alert severity="error">{analysisError}</Alert>
          )}

          {!analysisLoading && !analysisError && analysis && (
            <Stack spacing={2.25}>
              {!analysis.aiGenerated && (
                <Alert severity="info" variant="outlined">
                  OpenAI API Key가 없거나 호출에 실패해 규칙 기반 미리보기 분석을 표시합니다.
                </Alert>
              )}

              <Box>
                <Typography variant="subtitle1" sx={{ mb: 0.75 }}>상태 요약</Typography>
                <Alert severity={analysis.evidence?.mcsTransfer?.transferStatus === 'FAILED' ? 'error' : 'info'}>
                  {analysis.summary}
                </Alert>
              </Box>

              {renderAnalysisList('확인된 사실', analysis.facts, '확인된 사실이 없습니다.')}

              <Box>
                <Typography variant="subtitle1" sx={{ mb: 0.75 }}>추정 원인</Typography>
                <Typography variant="body2" color="text.secondary">{analysis.inference || '-'}</Typography>
              </Box>

              <Box>
                <Typography variant="subtitle1" sx={{ mb: 0.75 }}>운영 영향</Typography>
                <Typography variant="body2" color="text.secondary">{analysis.impact || '-'}</Typography>
              </Box>

              {renderAnalysisList('권장 조치', analysis.recommendedActions, '권장 조치가 없습니다.')}

              <Divider />

              <Box>
                <Typography variant="subtitle1" sx={{ mb: 1 }}>근거 데이터</Typography>
                <Stack spacing={1}>
                  <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }}>
                    <Chip label={`MES: ${analysis.evidence?.workOrder?.woStatus || '-'}`} size="small" />
                    <Chip
                      label={`MCS: ${analysis.evidence?.mcsTransfer?.transferStatus || '요청 전'}`}
                      size="small"
                      color={getMaterialStatusColor(analysis.evidence?.mcsTransfer?.transferStatus)}
                      variant="light"
                    />
                    <Chip label={`Model: ${analysis.model || '-'}`} size="small" variant="outlined" />
                  </Stack>
                  {analysis.evidence?.mcsTransfer && (
                    <Typography variant="caption" color="text.secondary">
                      이동오더 {analysis.evidence.mcsTransfer.transferNo} / {analysis.evidence.mcsTransfer.fromLocationCd || '-'} → {analysis.evidence.mcsTransfer.toLocationCd || '-'}
                    </Typography>
                  )}
                  {analysis.evidence?.plcEvents?.length > 0 && (
                    <Typography variant="caption" color="text.secondary">
                      최근 PLC 이벤트 {analysis.evidence.plcEvents[0].eventType} / {analysis.evidence.plcEvents[0].eventMessage || '-'}
                    </Typography>
                  )}
                </Stack>
              </Box>
            </Stack>
          )}
        </Box>
      </Drawer>

      <Snackbar open={!!message} autoHideDuration={3500} onClose={() => setMessage(null)} anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}>
        {message && <Alert severity={message.severity} variant="filled" onClose={() => setMessage(null)}>{message.text}</Alert>}
      </Snackbar>
    </Stack>
  );
}
