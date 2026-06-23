import { useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
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
import FormControl from '@mui/material/FormControl';
import Grid from '@mui/material/Grid';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import Select from '@mui/material/Select';
import Snackbar from '@mui/material/Snackbar';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TablePager from 'components/TablePager';
import TableRow from '@mui/material/TableRow';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';

import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  SwapOutlined,
  UnorderedListOutlined
} from '@ant-design/icons';

import MainCard from 'components/MainCard';
import { locationApi } from 'api/mcs/locations';
import { mcsReferenceApi } from 'api/mcs/references';
import { plcEventApi } from 'api/mcs/plcEvents';
import { routeApi } from 'api/mcs/routes';
import { transferApi } from 'api/mcs/transfers';

const emptyForm = {
  plantCd: '',
  transferNo: '',
  fromLocationId: '',
  toLocationId: '',
  optimizeRule: 'SHORTEST_TIME',
  transferReason: ''
};

const emptyItemForm = {
  itemCd: '',
  lotNo: '',
  transferQty: 1
};

const optimizeRules = [
  { value: 'SHORTEST_TIME', label: '최단 시간' },
  { value: 'SHORTEST_DISTANCE', label: '최단 거리' },
  { value: 'AVOID_CONGESTION', label: '혼잡 회피' }
];

const fallbackStatuses = [
  { comCd: 'REQUESTED', comNm: '이동요청' },
  { comCd: 'IN_PROGRESS', comNm: '이동중' },
  { comCd: 'COMPLETED', comNm: '완료' },
  { comCd: 'CANCELLED', comNm: '취소' },
  { comCd: 'FAILED', comNm: '실패' }
];

const routeStatusLabels = {
  PLANNED: '계획됨',
  ACTIVE: '진행중',
  COMPLETED: '완료',
  REPLANNED: '재계산됨',
  FAILED: '실패'
};

function getApiData(response, fallback) {
  return response?.data ?? fallback;
}

function toNumberOrNull(value) {
  return value === '' || value === null || value === undefined ? null : Number(value);
}

function isMesRequestedTransfer(transfer) {
  return transfer?.transferNo?.startsWith('MES-');
}

function getEventStatusColor(status) {
  if (status === 'ERROR') return 'error';
  if (status === 'WARNING') return 'warning';
  return 'info';
}

function getProcessResultColor(result) {
  if (result === 'SUCCESS') return 'success';
  if (result === 'VALIDATION_FAILED') return 'warning';
  if (result === 'FAILED') return 'error';
  return 'default';
}

function getProcessResultLabel(result) {
  if (result === 'SUCCESS') return '처리 완료';
  if (result === 'VALIDATION_FAILED') return '데이터 누락';
  if (result === 'FAILED') return '처리 실패';
  return '대기';
}

export default function McsTransfers() {
  const [urlSearchParams] = useSearchParams();
  const initialSearch = {
    plantCd: urlSearchParams.get('plantCd') || '',
    transferStatus: urlSearchParams.get('transferStatus') || '',
    transferNo: urlSearchParams.get('transferNo') || ''
  };
  const [search, setSearch] = useState(initialSearch);
  const [query, setQuery] = useState({ page: 1, size: 10 });
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingTransfer, setEditingTransfer] = useState(null);
  const [selectedTransfer, setSelectedTransfer] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [itemForm, setItemForm] = useState(emptyItemForm);
  const [message, setMessage] = useState(null);
  const [pendingAction, setPendingAction] = useState(null);

  const transferParams = useMemo(() => ({ ...search, ...query }), [search, query]);
  const {
    data: transferResponse,
    error: transferError,
    isLoading,
    mutate
  } = useSWR(['mcs-transfers', transferParams], () => transferApi.list(transferParams));
  const { data: plantResponse } = useSWR('mcs-reference-plants', () => mcsReferenceApi.plants());
  const { data: statusResponse } = useSWR('mcs-reference-transfer-statuses', () => mcsReferenceApi.codes('MCS_TF_STATUS'));
  const { data: locationResponse } = useSWR('mcs-location-options', () => locationApi.list({ page: 1, size: 1000 }));
  const { data: itemResponse } = useSWR(['mcs-reference-items', form.plantCd || search.plantCd], () =>
    mcsReferenceApi.items({ plantCd: form.plantCd || search.plantCd || undefined })
  );
  const {
    data: transferItemResponse,
    error: transferItemError,
    mutate: mutateTransferItems
  } = useSWR(selectedTransfer ? ['mcs-transfer-items', selectedTransfer.transferId] : null, () =>
    transferApi.items(selectedTransfer.transferId)
  );
  const {
    data: transferRouteResponse,
    error: transferRouteError,
    mutate: mutateTransferRoute
  } = useSWR(selectedTransfer ? ['mcs-transfer-route', selectedTransfer.transferId] : null, () =>
    routeApi.transferRoute(selectedTransfer.transferId)
  );
  const { data: plcEventResponse, error: plcEventError } = useSWR(
    selectedTransfer ? ['mcs-transfer-plc-events', selectedTransfer.transferId] : null,
    () => plcEventApi.list({ targetType: 'TRANSFER', targetId: selectedTransfer.transferId, page: 1, size: 5 })
  );

  const page = getApiData(transferResponse, { content: [], totalElements: 0, currentPage: 1, size: 10 });
  const plants = getApiData(plantResponse, []);
  const statuses = getApiData(statusResponse, []);
  const transferStatuses = useMemo(() => {
    const merged = [...statuses];
    fallbackStatuses.forEach((fallback) => {
      if (!merged.some((status) => status.comCd === fallback.comCd)) merged.push(fallback);
    });
    return merged;
  }, [statuses]);
  const locations = getApiData(locationResponse, { content: [] }).content || [];
  const items = getApiData(itemResponse, []);
  const transferItems = getApiData(transferItemResponse, []);
  const transferRoute = getApiData(transferRouteResponse, null);
  const plcEvents = getApiData(plcEventResponse, { content: [] }).content || [];
  const isBusy = Boolean(pendingAction);
  const isPending = (actionId) => pendingAction === actionId;

  const filteredLocations = useMemo(() => {
    if (!form.plantCd) return locations;
    return locations.filter((location) => location.plantCd === form.plantCd);
  }, [form.plantCd, locations]);

  const selectedCanEdit = selectedTransfer?.transferStatus === 'REQUESTED';

  const handleSearchValue = (field, value) => {
    setSearch((current) => ({ ...current, [field]: value }));
  };

  const handleSearch = () => {
    setQuery((current) => ({ ...current, page: 1 }));
  };

  const handleReset = () => {
    setSearch({ plantCd: '', transferStatus: '', transferNo: '' });
    setQuery({ page: 1, size: 10 });
  };

  const openCreateDialog = () => {
    setEditingTransfer(null);
    setForm(emptyForm);
    setDialogOpen(true);
  };

  const openEditDialog = (transfer) => {
    setEditingTransfer(transfer);
    setForm({
      plantCd: transfer.plantCd || '',
      transferNo: transfer.transferNo || '',
      fromLocationId: transfer.fromLocationId || '',
      toLocationId: transfer.toLocationId || '',
      optimizeRule: transfer.optimizeRule || transferRoute?.optimizeRule || 'SHORTEST_TIME',
      transferReason: transfer.transferReason || ''
    });
    setDialogOpen(true);
  };

  const handleFormValue = (field, value) => {
    setForm((current) => {
      const next = { ...current, [field]: value };
      if (field === 'plantCd') {
        next.fromLocationId = '';
        next.toLocationId = '';
      }
      return next;
    });
  };

  const handleSave = async () => {
    if (isBusy) return;
    setPendingAction('save-transfer');
    try {
      const payload = {
        ...form,
        fromLocationId: toNumberOrNull(form.fromLocationId),
        toLocationId: toNumberOrNull(form.toLocationId)
      };

      if (payload.fromLocationId === payload.toLocationId) {
        setMessage({ severity: 'warning', text: '출발 Location과 도착 Location은 달라야 합니다.' });
        return;
      }

      if (editingTransfer) {
        await transferApi.update(editingTransfer.transferId, { ...payload, transferStatus: editingTransfer.transferStatus });
        setMessage({ severity: 'success', text: '이동 오더가 수정되었습니다.' });
      } else {
        await transferApi.create(payload);
        setMessage({ severity: 'success', text: '이동 오더가 등록되었습니다. 품목을 추가해 주세요.' });
      }

      setDialogOpen(false);
      await mutate();
      if (editingTransfer?.transferId === selectedTransfer?.transferId) {
        await mutateTransferRoute();
      }
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    } finally {
      setPendingAction(null);
    }
  };

  const handleSelectTransfer = (transfer) => {
    setSelectedTransfer(transfer);
    setItemForm(emptyItemForm);
  };

  const handleStatus = async (transfer, status) => {
    if (isBusy) return;
    setPendingAction(`status-${transfer.transferId}-${status}`);
    try {
      await transferApi.changeStatus(transfer.transferId, status);
      setMessage({ severity: 'success', text: '이동 상태가 변경되었습니다.' });
      await mutate();
      if (selectedTransfer?.transferId === transfer.transferId) {
        setSelectedTransfer((current) => (current ? { ...current, transferStatus: status } : current));
        await mutateTransferItems();
        await mutateTransferRoute();
      }
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    } finally {
      setPendingAction(null);
    }
  };

  const handleDelete = async (transfer) => {
    if (isBusy) return;
    const confirmed = window.confirm(`${transfer.transferNo} 이동 오더를 삭제하시겠습니까?`);
    if (!confirmed) return;

    setPendingAction(`delete-${transfer.transferId}`);
    try {
      await transferApi.remove(transfer.transferId);
      setMessage({ severity: 'success', text: '이동 오더가 삭제되었습니다.' });
      if (selectedTransfer?.transferId === transfer.transferId) setSelectedTransfer(null);
      await mutate();
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    } finally {
      setPendingAction(null);
    }
  };

  const handleAddItem = async () => {
    if (!selectedTransfer || isBusy) return;

    setPendingAction('add-transfer-item');
    try {
      await transferApi.addItem(selectedTransfer.transferId, {
        ...itemForm,
        transferQty: Number(itemForm.transferQty || 0)
      });
      setMessage({ severity: 'success', text: '이동 품목이 추가되었습니다.' });
      setItemForm(emptyItemForm);
      await mutateTransferItems();
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    } finally {
      setPendingAction(null);
    }
  };

  const handleRemoveItem = async (item) => {
    if (!selectedTransfer || isBusy) return;
    const confirmed = window.confirm(`${item.itemNm || item.itemCd} 품목을 삭제하시겠습니까?`);
    if (!confirmed) return;

    setPendingAction(`remove-item-${item.transferItemId}`);
    try {
      await transferApi.removeItem(selectedTransfer.transferId, item.transferItemId);
      setMessage({ severity: 'success', text: '이동 품목이 삭제되었습니다.' });
      await mutateTransferItems();
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    } finally {
      setPendingAction(null);
    }
  };

  const getStatusLabel = (transfer) =>
    transfer.transferStatusNm ||
    transferStatuses.find((status) => status.comCd === transfer.transferStatus)?.comNm ||
    transfer.transferStatus;

  const getStatusColor = (status) => {
    if (status === 'COMPLETED') return 'success';
    if (status === 'IN_PROGRESS') return 'info';
    if (status === 'FAILED') return 'error';
    if (status === 'CANCELLED') return 'default';
    return 'primary';
  };

  const getRouteStatusColor = (status) => {
    if (status === 'COMPLETED') return 'success';
    if (status === 'ACTIVE') return 'info';
    if (status === 'FAILED') return 'error';
    if (status === 'REPLANNED') return 'warning';
    return 'primary';
  };

  const getRouteStatusLabel = (status) => routeStatusLabels[status] || status || '-';

  const getOptimizeRuleLabel = (rule) => optimizeRules.find((option) => option.value === rule)?.label || rule || '-';

  const buildRouteText = (steps) => {
    if (!steps?.length) return '-';
    return [steps[0].fromNodeCd, ...steps.map((step) => step.toNodeCd)].join(' -> ');
  };

  return (
    <Stack spacing={3}>
      <Stack
        direction={{ xs: 'column', md: 'row' }}
        spacing={2}
        sx={{ justifyContent: 'space-between', alignItems: { xs: 'flex-start', md: 'center' } }}
      >
        <Box>
          <Typography variant="h3">MCS 이동 관리</Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mt: 0.75 }}>
            Location 간 자재 이동 오더를 등록하고 품목, 진행 상태, 경로, PLC 이벤트 흐름을 관리합니다.
          </Typography>
        </Box>
        <Button variant="contained" startIcon={<PlusOutlined />} onClick={openCreateDialog}>
          이동 등록
        </Button>
      </Stack>

      <MainCard title="검색 조건">
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 3 }}>
            <FormControl fullWidth size="small">
              <InputLabel>공장</InputLabel>
              <Select label="공장" value={search.plantCd} onChange={(event) => handleSearchValue('plantCd', event.target.value)}>
                <MenuItem value="">전체</MenuItem>
                {plants.map((plant) => (
                  <MenuItem key={plant.plantCd} value={plant.plantCd}>
                    {plant.plantNm}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <FormControl fullWidth size="small">
              <InputLabel>상태</InputLabel>
              <Select
                label="상태"
                value={search.transferStatus}
                onChange={(event) => handleSearchValue('transferStatus', event.target.value)}
              >
                <MenuItem value="">전체</MenuItem>
                {transferStatuses.map((status) => (
                  <MenuItem key={status.comCd} value={status.comCd}>
                    {status.comNm}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField
              fullWidth
              size="small"
              label="이동번호"
              value={search.transferNo}
              onChange={(event) => handleSearchValue('transferNo', event.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <Stack direction="row" spacing={1} sx={{ justifyContent: 'flex-end' }}>
              <Button variant="outlined" startIcon={<ReloadOutlined />} onClick={handleReset}>
                초기화
              </Button>
              <Button variant="contained" startIcon={<SearchOutlined />} onClick={handleSearch}>
                조회
              </Button>
            </Stack>
          </Grid>
        </Grid>
      </MainCard>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, xl: 8 }}>
          <MainCard title="이동 오더 목록" content={false}>
            {transferError && <Alert severity="error">{transferError.message}</Alert>}
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>이동번호</TableCell>
                    <TableCell>공장</TableCell>
                    <TableCell>출발 Location</TableCell>
                    <TableCell>도착 Location</TableCell>
                    <TableCell>사유</TableCell>
                    <TableCell>상태</TableCell>
                    <TableCell align="right">관리</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {!isLoading && page.content.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={7} align="center">
                        조회된 이동 오더가 없습니다.
                      </TableCell>
                    </TableRow>
                  )}
                  {page.content.map((transfer) => (
                    <TableRow
                      key={transfer.transferId}
                      hover
                      selected={selectedTransfer?.transferId === transfer.transferId}
                      onClick={() => handleSelectTransfer(transfer)}
                      sx={{ cursor: 'pointer' }}
                    >
                      <TableCell>{transfer.transferNo}</TableCell>
                      <TableCell>{transfer.plantNm || transfer.plantCd}</TableCell>
                      <TableCell>{transfer.fromLocationCd}</TableCell>
                      <TableCell>{transfer.toLocationCd}</TableCell>
                      <TableCell>{transfer.transferReason || '-'}</TableCell>
                      <TableCell>
                        <Chip
                          label={getStatusLabel(transfer)}
                          size="small"
                          color={getStatusColor(transfer.transferStatus)}
                          variant="light"
                        />
                      </TableCell>
                      <TableCell align="right" onClick={(event) => event.stopPropagation()}>
                        <Stack direction="row" spacing={1} sx={{ justifyContent: 'flex-end' }}>
                          <Button size="small" startIcon={<UnorderedListOutlined />} onClick={() => handleSelectTransfer(transfer)}>
                            상세
                          </Button>
                          <Button
                            size="small"
                            startIcon={<EditOutlined />}
                            disabled={isBusy || transfer.transferStatus !== 'REQUESTED'}
                            onClick={() => openEditDialog(transfer)}
                          >
                            수정
                          </Button>
                          {isMesRequestedTransfer(transfer) &&
                            transfer.transferStatus !== 'COMPLETED' &&
                            transfer.transferStatus !== 'CANCELLED' &&
                            transfer.transferStatus !== 'FAILED' && (
                              <Chip label="PLC 처리 대기" size="small" color="warning" variant="light" />
                            )}
                          {transfer.transferStatus === 'FAILED' && (
                            <Button
                              size="small"
                              color="error"
                              startIcon={
                                isPending(`status-${transfer.transferId}-CANCELLED`) ? (
                                  <CircularProgress size={14} color="inherit" />
                                ) : undefined
                              }
                              disabled={isBusy}
                              onClick={() => handleStatus(transfer, 'CANCELLED')}
                            >
                              취소
                            </Button>
                          )}
                          {!isMesRequestedTransfer(transfer) && transfer.transferStatus === 'REQUESTED' && (
                            <Button
                              size="small"
                              startIcon={
                                isPending(`status-${transfer.transferId}-IN_PROGRESS`) ? (
                                  <CircularProgress size={14} color="inherit" />
                                ) : (
                                  <SwapOutlined />
                                )
                              }
                              disabled={isBusy}
                              onClick={() => handleStatus(transfer, 'IN_PROGRESS')}
                            >
                              시작
                            </Button>
                          )}
                          {!isMesRequestedTransfer(transfer) && transfer.transferStatus === 'IN_PROGRESS' && (
                            <Button
                              size="small"
                              color="success"
                              startIcon={
                                isPending(`status-${transfer.transferId}-COMPLETED`) ? (
                                  <CircularProgress size={14} color="inherit" />
                                ) : undefined
                              }
                              disabled={isBusy}
                              onClick={() => handleStatus(transfer, 'COMPLETED')}
                            >
                              완료
                            </Button>
                          )}
                          <Button
                            size="small"
                            color="error"
                            startIcon={
                              isPending(`delete-${transfer.transferId}`) ? (
                                <CircularProgress size={14} color="inherit" />
                              ) : (
                                <DeleteOutlined />
                              )
                            }
                            disabled={isBusy || transfer.transferStatus !== 'REQUESTED'}
                            onClick={() => handleDelete(transfer)}
                          >
                            삭제
                          </Button>
                        </Stack>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
            <TablePager
              page={page.currentPage || 1}
              count={page.totalPages ?? Math.ceil((page.totalElements || 0) / (page.size || query.size || 10))}
              onChange={(nextPage) => setQuery((current) => ({ ...current, page: nextPage }))}
            />
          </MainCard>
        </Grid>

        <Grid size={{ xs: 12, xl: 4 }}>
          <MainCard title="이동 상세">
            {!selectedTransfer && (
              <Typography variant="body2" color="text.secondary">
                왼쪽 목록에서 이동 오더를 선택하면 품목, 경로, PLC 이벤트를 확인할 수 있습니다.
              </Typography>
            )}

            {selectedTransfer && (
              <Stack spacing={2}>
                <Box>
                  <Typography variant="h5">{selectedTransfer.transferNo}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    {selectedTransfer.fromLocationCd} {'->'} {selectedTransfer.toLocationCd}
                  </Typography>
                </Box>

                <Divider />

                <Stack spacing={1}>
                  <Stack direction="row" spacing={1} alignItems="center" justifyContent="space-between">
                    <Typography variant="subtitle1">이동 경로</Typography>
                    {transferRoute?.routeStatus && (
                      <Chip
                        label={getRouteStatusLabel(transferRoute.routeStatus)}
                        size="small"
                        color={getRouteStatusColor(transferRoute.routeStatus)}
                        variant="light"
                      />
                    )}
                  </Stack>
                  {transferRouteError && <Alert severity="error">{transferRouteError.message}</Alert>}
                  {!transferRoute && !transferRouteError && (
                    <Typography variant="body2" color="text.secondary">
                      저장된 이동 경로가 없습니다.
                    </Typography>
                  )}
                  {transferRoute && (
                    <Stack spacing={1}>
                      <Typography variant="body2" color="text.secondary">
                        {buildRouteText(transferRoute.steps)}
                      </Typography>
                      <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                        <Chip size="small" label={`기준 ${getOptimizeRuleLabel(transferRoute.optimizeRule)}`} variant="outlined" />
                        <Chip size="small" label={`${transferRoute.totalDistanceM}m`} variant="outlined" />
                        <Chip size="small" label={`${transferRoute.totalTimeSec}초`} variant="outlined" />
                      </Stack>
                    </Stack>
                  )}
                </Stack>

                <Divider />

                {transferItemError && <Alert severity="error">{transferItemError.message}</Alert>}

                <Stack spacing={1}>
                  <Typography variant="subtitle1">이동 품목</Typography>
                  {transferItems.length === 0 && (
                    <Typography variant="body2" color="text.secondary">
                      등록된 이동 품목이 없습니다.
                    </Typography>
                  )}
                  {transferItems.map((item) => (
                    <Stack
                      key={item.transferItemId}
                      direction="row"
                      spacing={1}
                      sx={{ justifyContent: 'space-between', alignItems: 'center', py: 0.75 }}
                    >
                      <Box>
                        <Typography variant="subtitle2">{item.itemNm || item.itemCd}</Typography>
                        <Typography variant="caption" color="text.secondary">
                          {item.itemCd} / LOT {item.lotNo || '-'} / {item.transferQty} {item.unit || ''}
                        </Typography>
                      </Box>
                      <Button
                        size="small"
                        color="error"
                        startIcon={
                          isPending(`remove-item-${item.transferItemId}`) ? <CircularProgress size={14} color="inherit" /> : undefined
                        }
                        disabled={isBusy || !selectedCanEdit}
                        onClick={() => handleRemoveItem(item)}
                      >
                        삭제
                      </Button>
                    </Stack>
                  ))}
                </Stack>

                <Divider />

                <Stack spacing={1}>
                  <Typography variant="subtitle1">관련 PLC 이벤트</Typography>
                  {plcEventError && <Alert severity="error">{plcEventError.message}</Alert>}
                  {!plcEventError && plcEvents.length === 0 && (
                    <Typography variant="body2" color="text.secondary">
                      아직 수신된 PLC 이벤트가 없습니다.
                    </Typography>
                  )}
                  {plcEvents.map((event) => (
                    <Box key={event.eventId} sx={{ borderBottom: '1px solid', borderColor: 'divider', pb: 1 }}>
                      <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                        <Typography variant="subtitle2">{event.eventType}</Typography>
                        <Chip label={event.eventStatus} size="small" color={getEventStatusColor(event.eventStatus)} variant="light" />
                        <Chip
                          label={getProcessResultLabel(event.processResult)}
                          size="small"
                          color={getProcessResultColor(event.processResult)}
                          variant="outlined"
                        />
                      </Stack>
                      <Typography variant="caption" color="text.secondary">
                        {event.eventDtm} / {event.equipmentCd || '-'} / {event.locationCd || '-'}
                      </Typography>
                      {(event.errorCode || event.eventMessage || event.processMessage) && (
                        <Typography variant="body2" color={event.eventStatus === 'ERROR' ? 'error.main' : 'text.secondary'}>
                          {[event.errorCode, event.eventMessage, event.processMessage].filter(Boolean).join(' - ')}
                        </Typography>
                      )}
                    </Box>
                  ))}
                </Stack>

                <Divider />

                <Stack spacing={1.5}>
                  <FormControl fullWidth size="small" disabled={!selectedCanEdit}>
                    <InputLabel>품목</InputLabel>
                    <Select
                      label="품목"
                      value={itemForm.itemCd}
                      onChange={(event) => setItemForm((current) => ({ ...current, itemCd: event.target.value }))}
                    >
                      {items.map((item) => (
                        <MenuItem key={item.itemCd} value={item.itemCd}>
                          {item.itemCd} - {item.itemNm}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                  <TextField
                    fullWidth
                    size="small"
                    disabled={!selectedCanEdit}
                    label="LOT 번호"
                    value={itemForm.lotNo}
                    onChange={(event) => setItemForm((current) => ({ ...current, lotNo: event.target.value }))}
                  />
                  <TextField
                    fullWidth
                    size="small"
                    disabled={!selectedCanEdit}
                    type="number"
                    label="이동 수량"
                    value={itemForm.transferQty}
                    onChange={(event) => setItemForm((current) => ({ ...current, transferQty: event.target.value }))}
                  />
                  <Button
                    variant="contained"
                    startIcon={isPending('add-transfer-item') ? <CircularProgress size={14} color="inherit" /> : <PlusOutlined />}
                    disabled={isBusy || !selectedCanEdit}
                    onClick={handleAddItem}
                  >
                    품목 추가
                  </Button>
                </Stack>
              </Stack>
            )}
          </MainCard>
        </Grid>
      </Grid>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth maxWidth="md">
        <DialogTitle>{editingTransfer ? '이동 오더 수정' : '이동 오더 등록'}</DialogTitle>
        <DialogContent dividers>
          <Grid container spacing={2} sx={{ pt: 0.5 }}>
            <Grid size={{ xs: 12, md: 6 }}>
              <FormControl fullWidth size="small" required disabled={!!editingTransfer}>
                <InputLabel>공장</InputLabel>
                <Select label="공장" value={form.plantCd} onChange={(event) => handleFormValue('plantCd', event.target.value)}>
                  {plants.map((plant) => (
                    <MenuItem key={plant.plantCd} value={plant.plantCd}>
                      {plant.plantNm}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                fullWidth
                size="small"
                disabled={!!editingTransfer}
                label="이동번호"
                value={form.transferNo}
                onChange={(event) => handleFormValue('transferNo', event.target.value)}
                placeholder="미입력 시 자동 생성"
              />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <FormControl fullWidth size="small" required>
                <InputLabel>출발 Location</InputLabel>
                <Select
                  label="출발 Location"
                  value={form.fromLocationId}
                  onChange={(event) => handleFormValue('fromLocationId', event.target.value)}
                >
                  {filteredLocations.map((location) => (
                    <MenuItem key={location.locationId} value={location.locationId}>
                      {location.locationCd} - {location.locationNm}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <FormControl fullWidth size="small" required>
                <InputLabel>도착 Location</InputLabel>
                <Select
                  label="도착 Location"
                  value={form.toLocationId}
                  onChange={(event) => handleFormValue('toLocationId', event.target.value)}
                >
                  {filteredLocations.map((location) => (
                    <MenuItem key={location.locationId} value={location.locationId}>
                      {location.locationCd} - {location.locationNm}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <FormControl fullWidth size="small" required>
                <InputLabel>경로 계산 기준</InputLabel>
                <Select
                  label="경로 계산 기준"
                  value={form.optimizeRule}
                  onChange={(event) => handleFormValue('optimizeRule', event.target.value)}
                >
                  {optimizeRules.map((rule) => (
                    <MenuItem key={rule.value} value={rule.value}>
                      {rule.label}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid size={12}>
              <TextField
                fullWidth
                multiline
                minRows={3}
                label="이동 사유"
                value={form.transferReason}
                onChange={(event) => handleFormValue('transferReason', event.target.value)}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>취소</Button>
          <Button
            variant="contained"
            startIcon={isPending('save-transfer') ? <CircularProgress size={14} color="inherit" /> : undefined}
            disabled={isBusy}
            onClick={handleSave}
          >
            저장
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={!!message}
        autoHideDuration={3500}
        onClose={() => setMessage(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        {message && (
          <Alert severity={message.severity} variant="filled" onClose={() => setMessage(null)}>
            {message.text}
          </Alert>
        )}
      </Snackbar>
    </Stack>
  );
}
