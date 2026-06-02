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
import TablePagination from '@mui/material/TablePagination';
import TableRow from '@mui/material/TableRow';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';

import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons';

import MainCard from 'components/MainCard';
import { outboundApi } from 'api/mcs/outbounds';
import { mcsReferenceApi } from 'api/mcs/references';

const nowForInput = () => new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 16);

const emptyForm = {
  plantCd: '',
  outboundNo: '',
  customerCd: '',
  warehouseCd: '',
  requestDt: nowForInput(),
  destination: '',
  outboundRmk: ''
};

function getApiData(response, fallback) {
  return response?.data ?? fallback;
}

export default function McsOutbounds() {
  const [search, setSearch] = useState({ plantCd: '', warehouseCd: '', outboundStatus: '', outboundNo: '', fromDate: '', toDate: '' });
  const [query, setQuery] = useState({ page: 1, size: 10 });
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingOutbound, setEditingOutbound] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [message, setMessage] = useState(null);
  const [pendingAction, setPendingAction] = useState(null);

  const outboundParams = useMemo(() => ({ ...search, ...query }), [search, query]);
  const { data: outboundResponse, error: outboundError, isLoading, mutate } = useSWR(['mcs-outbounds', outboundParams], () => outboundApi.list(outboundParams));
  const { data: plantResponse } = useSWR('mcs-reference-plants', () => mcsReferenceApi.plants());
  const { data: warehouseResponse } = useSWR('mcs-reference-warehouses', () => mcsReferenceApi.warehouses({ useYn: 'Y' }));
  const { data: vendorResponse } = useSWR('mcs-reference-vendors', () => mcsReferenceApi.vendors({ useYn: 'Y' }));
  const { data: statusResponse } = useSWR('mcs-reference-outbound-statuses', () => mcsReferenceApi.codes('MCS_OB_STATUS'));

  const page = getApiData(outboundResponse, { content: [], totalElements: 0, currentPage: 1, size: 10 });
  const plants = getApiData(plantResponse, []);
  const warehouses = getApiData(warehouseResponse, []);
  const customers = getApiData(vendorResponse, []);
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

  const handleSearch = () => setQuery((current) => ({ ...current, page: 1 }));

  const handleReset = () => {
    setSearch({ plantCd: '', warehouseCd: '', outboundStatus: '', outboundNo: '', fromDate: '', toDate: '' });
    setQuery({ page: 1, size: 10 });
  };

  const openCreateDialog = () => {
    setEditingOutbound(null);
    setForm({ ...emptyForm, requestDt: nowForInput() });
    setDialogOpen(true);
  };

  const openEditDialog = (outbound) => {
    setEditingOutbound(outbound);
    setForm({
      plantCd: outbound.plantCd || '',
      outboundNo: outbound.outboundNo || '',
      customerCd: outbound.customerCd || '',
      warehouseCd: outbound.warehouseCd || '',
      requestDt: outbound.requestDt ? outbound.requestDt.slice(0, 16) : nowForInput(),
      destination: outbound.destination || '',
      outboundRmk: outbound.outboundRmk || ''
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

  const toPayload = () => ({
    ...form,
    requestDt: form.requestDt ? `${form.requestDt}:00` : null
  });

  const handleSave = async () => {
    if (isBusy) return;
    setPendingAction('save-outbound');
    try {
      if (editingOutbound) {
        await outboundApi.update(editingOutbound.outboundId, toPayload());
        setMessage({ severity: 'success', text: '출고 오더가 수정되었습니다.' });
      } else {
        await outboundApi.create(toPayload());
        setMessage({ severity: 'success', text: '출고 오더가 등록되었습니다.' });
      }
      setDialogOpen(false);
      await mutate();
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    } finally {
      setPendingAction(null);
    }
  };

  const handleStatus = async (outbound, status) => {
    if (isBusy) return;
    setPendingAction(`status-outbound-${outbound.outboundId}-${status}`);
    try {
      await outboundApi.changeStatus(outbound.outboundId, status);
      setMessage({ severity: 'success', text: '출고 상태가 변경되었습니다.' });
      await mutate();
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    } finally {
      setPendingAction(null);
    }
  };

  const handleDelete = async (outbound) => {
    if (isBusy) return;
    const confirmed = window.confirm(`${outbound.outboundNo} 출고 오더를 삭제하시겠습니까?`);
    if (!confirmed) return;

    setPendingAction(`delete-outbound-${outbound.outboundId}`);
    try {
      await outboundApi.remove(outbound.outboundId);
      setMessage({ severity: 'success', text: '출고 오더가 삭제되었습니다.' });
      await mutate();
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    } finally {
      setPendingAction(null);
    }
  };

  const getStatusLabel = (outbound) => outbound.outboundStatusNm || statuses.find((status) => status.comCd === outbound.outboundStatus)?.comNm || outbound.outboundStatus;

  return (
    <Stack spacing={3}>
      <Stack
        direction={{ xs: 'column', md: 'row' }}
        spacing={2}
        sx={{ justifyContent: 'space-between', alignItems: { xs: 'flex-start', md: 'center' } }}
      >
        <Box>
          <Typography variant="h3">MCS 출고 관리</Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mt: 0.75 }}>
            출고 오더를 등록하고 요청일, 출고 창고, 목적지, 진행 상태를 관리합니다.
          </Typography>
        </Box>
        <Button variant="contained" startIcon={<PlusOutlined />} disabled={isBusy} onClick={openCreateDialog}>
          출고 등록
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
              <InputLabel>상태</InputLabel>
              <Select label="상태" value={search.outboundStatus} onChange={(event) => handleSearchValue('outboundStatus', event.target.value)}>
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
            <TextField fullWidth size="small" label="출고번호" value={search.outboundNo} onChange={(event) => handleSearchValue('outboundNo', event.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField
              fullWidth
              size="small"
              type="date"
              label="요청일 From"
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
              label="요청일 To"
              value={search.toDate}
              onChange={(event) => handleSearchValue('toDate', event.target.value)}
              slotProps={{ inputLabel: { shrink: true } }}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 6 }}>
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

      <MainCard title="출고 목록" content={false}>
        {outboundError && <Alert severity="error">{outboundError.message}</Alert>}
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>출고번호</TableCell>
                <TableCell>공장</TableCell>
                <TableCell>창고</TableCell>
                <TableCell>출고처</TableCell>
                <TableCell>목적지</TableCell>
                <TableCell>요청일</TableCell>
                <TableCell>출고일</TableCell>
                <TableCell>상태</TableCell>
                <TableCell align="right">관리</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {!isLoading && page.content.length === 0 && (
                <TableRow>
                  <TableCell colSpan={9} align="center">
                    조회된 출고 오더가 없습니다.
                  </TableCell>
                </TableRow>
              )}
              {page.content.map((outbound) => (
                <TableRow key={outbound.outboundId} hover>
                  <TableCell>{outbound.outboundNo}</TableCell>
                  <TableCell>{outbound.plantNm || outbound.plantCd}</TableCell>
                  <TableCell>{outbound.warehouseNm || outbound.warehouseCd}</TableCell>
                  <TableCell>{outbound.customerCd || '-'}</TableCell>
                  <TableCell>{outbound.destination || '-'}</TableCell>
                  <TableCell>{outbound.requestDt || '-'}</TableCell>
                  <TableCell>{outbound.shippedDt || '-'}</TableCell>
                  <TableCell>
                    <Chip label={getStatusLabel(outbound)} size="small" color={outbound.outboundStatus === 'SHIPPED' ? 'success' : 'primary'} variant="light" />
                  </TableCell>
                  <TableCell align="right">
                    <Stack direction="row" spacing={1} sx={{ justifyContent: 'flex-end' }}>
                      <Button size="small" startIcon={<EditOutlined />} disabled={isBusy || outbound.outboundStatus !== 'REQUESTED'} onClick={() => openEditDialog(outbound)}>
                        수정
                      </Button>
                      {outbound.outboundStatus !== 'SHIPPED' && (
                        <Button
                          size="small"
                          startIcon={isPending(`status-outbound-${outbound.outboundId}-SHIPPED`) ? <CircularProgress size={14} color="inherit" /> : undefined}
                          disabled={isBusy}
                          onClick={() => handleStatus(outbound, 'SHIPPED')}
                        >
                          출고
                        </Button>
                      )}
                      <Button
                        size="small"
                        color="error"
                        startIcon={isPending(`delete-outbound-${outbound.outboundId}`) ? <CircularProgress size={14} color="inherit" /> : <DeleteOutlined />}
                        disabled={isBusy || outbound.outboundStatus !== 'REQUESTED'}
                        onClick={() => handleDelete(outbound)}
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
        <TablePagination
          component="div"
          count={page.totalElements}
          page={Math.max((page.currentPage || 1) - 1, 0)}
          rowsPerPage={page.size || query.size}
          rowsPerPageOptions={[10, 20, 50]}
          onPageChange={(_, nextPage) => setQuery((current) => ({ ...current, page: nextPage + 1 }))}
          onRowsPerPageChange={(event) => setQuery({ page: 1, size: Number(event.target.value) })}
        />
      </MainCard>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth maxWidth="md">
        <DialogTitle>{editingOutbound ? '출고 수정' : '출고 등록'}</DialogTitle>
        <DialogContent dividers>
          <Grid container spacing={2} sx={{ pt: 0.5 }}>
            <Grid size={{ xs: 12, md: 6 }}>
              <FormControl fullWidth size="small" required disabled={!!editingOutbound}>
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
                disabled={!!editingOutbound}
                label="출고번호"
                value={form.outboundNo}
                onChange={(event) => handleFormValue('outboundNo', event.target.value)}
                placeholder="미입력 시 자동 생성"
              />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <FormControl fullWidth size="small" required>
                <InputLabel>출고 창고</InputLabel>
                <Select label="출고 창고" value={form.warehouseCd} onChange={(event) => handleFormValue('warehouseCd', event.target.value)}>
                  {filteredFormWarehouses.map((warehouse) => (
                    <MenuItem key={warehouse.warehouseCd} value={warehouse.warehouseCd}>
                      {warehouse.warehouseNm}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <FormControl fullWidth size="small">
                <InputLabel>출고처</InputLabel>
                <Select label="출고처" value={form.customerCd} onChange={(event) => handleFormValue('customerCd', event.target.value)}>
                  <MenuItem value="">선택 안 함</MenuItem>
                  {customers.map((customer) => (
                    <MenuItem key={customer.vendorCd} value={customer.vendorCd}>
                      {customer.vendorNm}
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
                type="datetime-local"
                label="요청일"
                value={form.requestDt}
                onChange={(event) => handleFormValue('requestDt', event.target.value)}
                slotProps={{ inputLabel: { shrink: true } }}
              />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField fullWidth size="small" label="목적지" value={form.destination} onChange={(event) => handleFormValue('destination', event.target.value)} />
            </Grid>
            <Grid size={12}>
              <TextField
                fullWidth
                multiline
                minRows={3}
                label="비고"
                value={form.outboundRmk}
                onChange={(event) => handleFormValue('outboundRmk', event.target.value)}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>취소</Button>
          <Button
            variant="contained"
            startIcon={isPending('save-outbound') ? <CircularProgress size={14} color="inherit" /> : undefined}
            disabled={isBusy}
            onClick={handleSave}
          >
            저장
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={!!message} autoHideDuration={3500} onClose={() => setMessage(null)} anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}>
        {message && (
          <Alert severity={message.severity} variant="filled" onClose={() => setMessage(null)}>
            {message.text}
          </Alert>
        )}
      </Snackbar>
    </Stack>
  );
}
