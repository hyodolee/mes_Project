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

import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons';

import MainCard from 'components/MainCard';
import { inboundApi } from 'api/mcs/inbounds';
import { mcsReferenceApi } from 'api/mcs/references';

const emptyForm = {
  plantCd: '',
  inboundNo: '',
  vendorCd: '',
  warehouseCd: '',
  expectedDt: '',
  inboundRmk: ''
};

function getApiData(response, fallback) {
  return response?.data ?? fallback;
}

export default function McsInbounds() {
  const [search, setSearch] = useState({
    plantCd: '',
    vendorCd: '',
    warehouseCd: '',
    inboundStatus: '',
    inboundNo: '',
    fromDate: '',
    toDate: ''
  });
  const [query, setQuery] = useState({ page: 1, size: 10 });
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingInbound, setEditingInbound] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [message, setMessage] = useState(null);
  const [pendingAction, setPendingAction] = useState(null);

  const inboundParams = useMemo(() => ({ ...search, ...query }), [search, query]);
  const {
    data: inboundResponse,
    error: inboundError,
    isLoading,
    mutate
  } = useSWR(['mcs-inbounds', inboundParams], () => inboundApi.list(inboundParams));
  const { data: plantResponse } = useSWR('mcs-reference-plants', () => mcsReferenceApi.plants());
  const { data: warehouseResponse } = useSWR('mcs-reference-warehouses', () => mcsReferenceApi.warehouses({ useYn: 'Y' }));
  const { data: vendorResponse } = useSWR('mcs-reference-vendors', () => mcsReferenceApi.vendors({ useYn: 'Y' }));
  const { data: statusResponse } = useSWR('mcs-reference-inbound-statuses', () => mcsReferenceApi.codes('MCS_IB_STATUS'));

  const page = getApiData(inboundResponse, { content: [], totalElements: 0, currentPage: 1, size: 10 });
  const plants = getApiData(plantResponse, []);
  const warehouses = getApiData(warehouseResponse, []);
  const vendors = getApiData(vendorResponse, []);
  const statuses = getApiData(statusResponse, []);
  const isBusy = Boolean(pendingAction);
  const isPending = (actionId) => pendingAction === actionId;

  const filteredSearchWarehouses = useMemo(() => {
    if (!search.plantCd) return warehouses;
    return warehouses.filter((warehouse) => warehouse.plantCd === search.plantCd);
  }, [search.plantCd, warehouses]);

  const filteredFormWarehouses = useMemo(() => {
    if (!form.plantCd) return warehouses;
    return warehouses.filter((warehouse) => warehouse.plantCd === form.plantCd);
  }, [form.plantCd, warehouses]);

  const handleSearchValue = (field, value) => {
    setSearch((current) => {
      const next = { ...current, [field]: value };
      if (field === 'plantCd') next.warehouseCd = '';
      return next;
    });
  };

  const handleSearch = () => {
    setQuery((current) => ({ ...current, page: 1 }));
  };

  const handleReset = () => {
    setSearch({ plantCd: '', vendorCd: '', warehouseCd: '', inboundStatus: '', inboundNo: '', fromDate: '', toDate: '' });
    setQuery({ page: 1, size: 10 });
  };

  const openCreateDialog = () => {
    setEditingInbound(null);
    setForm(emptyForm);
    setDialogOpen(true);
  };

  const openEditDialog = (inbound) => {
    setEditingInbound(inbound);
    setForm({
      plantCd: inbound.plantCd || '',
      inboundNo: inbound.inboundNo || '',
      vendorCd: inbound.vendorCd || '',
      warehouseCd: inbound.warehouseCd || '',
      expectedDt: inbound.expectedDt || '',
      inboundRmk: inbound.inboundRmk || ''
    });
    setDialogOpen(true);
  };

  const handleFormValue = (field, value) => {
    setForm((current) => {
      const next = { ...current, [field]: value };
      if (field === 'plantCd') next.warehouseCd = '';
      return next;
    });
  };

  const handleSave = async () => {
    if (isBusy) return;
    setPendingAction('save-inbound');
    try {
      if (editingInbound) {
        await inboundApi.update(editingInbound.inboundId, form);
        setMessage({ severity: 'success', text: '입고 오더가 수정되었습니다.' });
      } else {
        await inboundApi.create(form);
        setMessage({ severity: 'success', text: '입고 오더가 등록되었습니다.' });
      }
      setDialogOpen(false);
      await mutate();
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    } finally {
      setPendingAction(null);
    }
  };

  const handleStatus = async (inbound, status) => {
    if (isBusy) return;
    setPendingAction(`status-inbound-${inbound.inboundId}-${status}`);
    try {
      await inboundApi.changeStatus(inbound.inboundId, status);
      setMessage({ severity: 'success', text: '입고 상태가 변경되었습니다.' });
      await mutate();
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    } finally {
      setPendingAction(null);
    }
  };

  const handleDelete = async (inbound) => {
    if (isBusy) return;
    const confirmed = window.confirm(`${inbound.inboundNo} 입고 오더를 삭제하시겠습니까?`);
    if (!confirmed) return;

    setPendingAction(`delete-inbound-${inbound.inboundId}`);
    try {
      await inboundApi.remove(inbound.inboundId);
      setMessage({ severity: 'success', text: '입고 오더가 삭제되었습니다.' });
      await mutate();
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    } finally {
      setPendingAction(null);
    }
  };

  const getStatusLabel = (inbound) =>
    inbound.inboundStatusNm || statuses.find((status) => status.comCd === inbound.inboundStatus)?.comNm || inbound.inboundStatus;

  return (
    <Stack spacing={3}>
      <Stack
        direction={{ xs: 'column', md: 'row' }}
        spacing={2}
        sx={{ justifyContent: 'space-between', alignItems: { xs: 'flex-start', md: 'center' } }}
      >
        <Box>
          <Typography variant="h3">MCS 입고 관리</Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mt: 0.75 }}>
            입고 오더를 등록하고 예정일, 거래처, 입고 창고, 진행 상태를 관리합니다.
          </Typography>
        </Box>
        <Button variant="contained" startIcon={<PlusOutlined />} disabled={isBusy} onClick={openCreateDialog}>
          입고 등록
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
              <InputLabel>창고</InputLabel>
              <Select label="창고" value={search.warehouseCd} onChange={(event) => handleSearchValue('warehouseCd', event.target.value)}>
                <MenuItem value="">전체</MenuItem>
                {filteredSearchWarehouses.map((warehouse) => (
                  <MenuItem key={warehouse.warehouseCd} value={warehouse.warehouseCd}>
                    {warehouse.warehouseNm}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <FormControl fullWidth size="small">
              <InputLabel>거래처</InputLabel>
              <Select label="거래처" value={search.vendorCd} onChange={(event) => handleSearchValue('vendorCd', event.target.value)}>
                <MenuItem value="">전체</MenuItem>
                {vendors.map((vendor) => (
                  <MenuItem key={vendor.vendorCd} value={vendor.vendorCd}>
                    {vendor.vendorNm}
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
                value={search.inboundStatus}
                onChange={(event) => handleSearchValue('inboundStatus', event.target.value)}
              >
                <MenuItem value="">전체</MenuItem>
                {statuses.map((status) => (
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
              label="입고번호"
              value={search.inboundNo}
              onChange={(event) => handleSearchValue('inboundNo', event.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField
              fullWidth
              size="small"
              type="date"
              label="예정일 From"
              value={search.fromDate}
              onChange={(event) => handleSearchValue('fromDate', event.target.value)}
              slotProps={{ inputLabel: { shrink: true } }}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField
              fullWidth
              size="small"
              type="date"
              label="예정일 To"
              value={search.toDate}
              onChange={(event) => handleSearchValue('toDate', event.target.value)}
              slotProps={{ inputLabel: { shrink: true } }}
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

      <MainCard title="입고 목록" content={false}>
        {inboundError && <Alert severity="error">{inboundError.message}</Alert>}
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>입고번호</TableCell>
                <TableCell>공장</TableCell>
                <TableCell>창고</TableCell>
                <TableCell>거래처</TableCell>
                <TableCell>예정일</TableCell>
                <TableCell>실제입고일</TableCell>
                <TableCell>상태</TableCell>
                <TableCell align="right">관리</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {!isLoading && page.content.length === 0 && (
                <TableRow>
                  <TableCell colSpan={8} align="center">
                    조회된 입고 오더가 없습니다.
                  </TableCell>
                </TableRow>
              )}
              {page.content.map((inbound) => (
                <TableRow key={inbound.inboundId} hover>
                  <TableCell>{inbound.inboundNo}</TableCell>
                  <TableCell>{inbound.plantNm || inbound.plantCd}</TableCell>
                  <TableCell>{inbound.warehouseNm || inbound.warehouseCd}</TableCell>
                  <TableCell>{inbound.vendorNm || inbound.vendorCd}</TableCell>
                  <TableCell>{inbound.expectedDt}</TableCell>
                  <TableCell>{inbound.actualDt || '-'}</TableCell>
                  <TableCell>
                    <Chip
                      label={getStatusLabel(inbound)}
                      size="small"
                      color={inbound.inboundStatus === 'COMPLETED' ? 'success' : 'primary'}
                      variant="light"
                    />
                  </TableCell>
                  <TableCell align="right">
                    <Stack direction="row" spacing={1} sx={{ justifyContent: 'flex-end' }}>
                      <Button
                        size="small"
                        startIcon={<EditOutlined />}
                        disabled={isBusy || inbound.inboundStatus !== 'PLANNED'}
                        onClick={() => openEditDialog(inbound)}
                      >
                        수정
                      </Button>
                      {inbound.inboundStatus !== 'COMPLETED' && (
                        <Button
                          size="small"
                          startIcon={
                            isPending(`status-inbound-${inbound.inboundId}-COMPLETED`) ? (
                              <CircularProgress size={14} color="inherit" />
                            ) : undefined
                          }
                          disabled={isBusy}
                          onClick={() => handleStatus(inbound, 'COMPLETED')}
                        >
                          완료
                        </Button>
                      )}
                      <Button
                        size="small"
                        color="error"
                        startIcon={
                          isPending(`delete-inbound-${inbound.inboundId}`) ? (
                            <CircularProgress size={14} color="inherit" />
                          ) : (
                            <DeleteOutlined />
                          )
                        }
                        disabled={isBusy || inbound.inboundStatus !== 'PLANNED'}
                        onClick={() => handleDelete(inbound)}
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

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth maxWidth="md">
        <DialogTitle>{editingInbound ? '입고 수정' : '입고 등록'}</DialogTitle>
        <DialogContent dividers>
          <Grid container spacing={2} sx={{ pt: 0.5 }}>
            <Grid size={{ xs: 12, md: 6 }}>
              <FormControl fullWidth size="small" required disabled={!!editingInbound}>
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
                disabled={!!editingInbound}
                label="입고번호"
                value={form.inboundNo}
                onChange={(event) => handleFormValue('inboundNo', event.target.value)}
                placeholder="미입력 시 자동 생성"
              />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <FormControl fullWidth size="small">
                <InputLabel>거래처</InputLabel>
                <Select label="거래처" value={form.vendorCd} onChange={(event) => handleFormValue('vendorCd', event.target.value)}>
                  <MenuItem value="">선택 안 함</MenuItem>
                  {vendors.map((vendor) => (
                    <MenuItem key={vendor.vendorCd} value={vendor.vendorCd}>
                      {vendor.vendorNm}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <FormControl fullWidth size="small" required>
                <InputLabel>입고 창고</InputLabel>
                <Select label="입고 창고" value={form.warehouseCd} onChange={(event) => handleFormValue('warehouseCd', event.target.value)}>
                  {filteredFormWarehouses.map((warehouse) => (
                    <MenuItem key={warehouse.warehouseCd} value={warehouse.warehouseCd}>
                      {warehouse.warehouseNm}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                fullWidth
                size="small"
                required
                type="date"
                label="입고 예정일"
                value={form.expectedDt}
                onChange={(event) => handleFormValue('expectedDt', event.target.value)}
                slotProps={{ inputLabel: { shrink: true } }}
              />
            </Grid>
            <Grid size={12}>
              <TextField
                fullWidth
                multiline
                minRows={3}
                label="비고"
                value={form.inboundRmk}
                onChange={(event) => handleFormValue('inboundRmk', event.target.value)}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>취소</Button>
          <Button
            variant="contained"
            startIcon={isPending('save-inbound') ? <CircularProgress size={14} color="inherit" /> : undefined}
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
