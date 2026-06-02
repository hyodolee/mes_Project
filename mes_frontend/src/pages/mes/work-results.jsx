import { useMemo, useState } from 'react';
import useSWR from 'swr';

import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
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

import { PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons';

import MainCard from 'components/MainCard';
import { mesProductionApi } from 'api/mes/production';
import { mesWorkOrderApi } from 'api/mes/workOrders';

const emptyForm = {
  plantCd: '',
  woId: '',
  resultDt: '',
  shift: 'A',
  workerId: 'SYSTEM',
  workcenterCd: 'WC-01',
  equipmentCd: '',
  itemCd: '',
  prodQty: 1,
  goodQty: 1,
  defectQty: 0,
  startDtm: '',
  endDtm: '',
  workTime: 0,
  setupTime: 0,
  downTime: 0,
  lotNo: '',
  resultStatus: '완료',
  resultRmk: ''
};

function getApiData(response, fallback) {
  return response?.data ?? fallback;
}

function today() {
  return new Date().toISOString().slice(0, 10);
}

function currentDateTimeLocal(hourOffset = 0) {
  const date = new Date();
  date.setHours(date.getHours() + hourOffset);
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
}

export default function MesWorkResults() {
  const [search, setSearch] = useState({ plantCd: '', itemCd: '', resultFromDt: '', resultToDt: '' });
  const [query, setQuery] = useState(search);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [message, setMessage] = useState(null);

  const { data: resultResponse, error: resultError, isLoading, mutate } = useSWR(['mes-work-results', query], () => mesProductionApi.workResults(query));
  const { data: orderResponse } = useSWR('mes-work-order-options', () => mesWorkOrderApi.list({}));

  const results = getApiData(resultResponse, []);
  const orders = getApiData(orderResponse, []);

  const visibleResults = useMemo(() => {
    const start = page * rowsPerPage;
    return results.slice(start, start + rowsPerPage);
  }, [page, results, rowsPerPage]);

  const openCreateDialog = () => {
    setForm({
      ...emptyForm,
      resultDt: today(),
      startDtm: currentDateTimeLocal(-1),
      endDtm: currentDateTimeLocal()
    });
    setDialogOpen(true);
  };

  const handleSearchValue = (field, value) => setSearch((current) => ({ ...current, [field]: value }));

  const handleFormValue = (field, value) => {
    setForm((current) => {
      const next = { ...current, [field]: value };
      if (field === 'woId' && value) {
        const order = orders.find((candidate) => String(candidate.woId) === String(value));
        if (order) {
          next.plantCd = order.plantCd || next.plantCd;
          next.itemCd = order.itemCd || next.itemCd;
          next.equipmentCd = order.equipmentCd || next.equipmentCd;
          next.workcenterCd = order.workcenterCd || next.workcenterCd;
          next.workerId = order.workerId || next.workerId;
          next.lotNo = order.lotNo || next.lotNo;
          next.prodQty = order.woQty || next.prodQty;
          next.goodQty = order.woQty || next.goodQty;
        }
      }
      if (field === 'prodQty') next.goodQty = value;
      if (field === 'defectQty') next.goodQty = Math.max(Number(next.prodQty || 0) - Number(value || 0), 0);
      return next;
    });
  };

  const handleSearch = () => {
    setPage(0);
    setQuery(search);
  };

  const handleReset = () => {
    const next = { plantCd: '', itemCd: '', resultFromDt: '', resultToDt: '' };
    setSearch(next);
    setQuery(next);
    setPage(0);
  };

  const handleSave = async () => {
    try {
      await mesProductionApi.createWorkResult({
        ...form,
        woId: Number(form.woId),
        prodQty: Number(form.prodQty || 0),
        goodQty: Number(form.goodQty || 0),
        defectQty: Number(form.defectQty || 0),
        workTime: Number(form.workTime || 0),
        setupTime: Number(form.setupTime || 0),
        downTime: Number(form.downTime || 0)
      });
      setMessage({ severity: 'success', text: '생산 실적이 등록되었습니다.' });
      setDialogOpen(false);
      await mutate();
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    }
  };

  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ justifyContent: 'space-between', alignItems: { xs: 'flex-start', md: 'center' } }}>
        <Box>
          <Typography variant="h3">MES 생산 실적</Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mt: 0.75 }}>
            작업 오더 기준 생산량, 양품, 불량, 작업 시간을 조회하고 실적을 등록합니다.
          </Typography>
        </Box>
        <Button variant="contained" startIcon={<PlusOutlined />} onClick={openCreateDialog}>
          생산 실적 등록
        </Button>
      </Stack>

      <MainCard title="검색 조건">
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField fullWidth size="small" label="공장" value={search.plantCd} onChange={(event) => handleSearchValue('plantCd', event.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField fullWidth size="small" label="품목 코드" value={search.itemCd} onChange={(event) => handleSearchValue('itemCd', event.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField fullWidth size="small" type="date" label="From" value={search.resultFromDt} onChange={(event) => handleSearchValue('resultFromDt', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField fullWidth size="small" type="date" label="To" value={search.resultToDt} onChange={(event) => handleSearchValue('resultToDt', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
          </Grid>
          <Grid size={12}>
            <Stack direction="row" spacing={1} sx={{ justifyContent: 'flex-end' }}>
              <Button variant="outlined" startIcon={<ReloadOutlined />} onClick={handleReset}>초기화</Button>
              <Button variant="contained" startIcon={<SearchOutlined />} onClick={handleSearch}>조회</Button>
            </Stack>
          </Grid>
        </Grid>
      </MainCard>

      <MainCard title="생산 실적 목록" content={false}>
        {resultError && <Alert severity="error">{resultError.message}</Alert>}
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>실적번호</TableCell>
                <TableCell>공장</TableCell>
                <TableCell>실적일</TableCell>
                <TableCell>작업오더</TableCell>
                <TableCell>품목</TableCell>
                <TableCell align="right">생산</TableCell>
                <TableCell align="right">양품</TableCell>
                <TableCell align="right">불량</TableCell>
                <TableCell>설비</TableCell>
                <TableCell>상태</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {!isLoading && results.length === 0 && (
                <TableRow>
                  <TableCell colSpan={10} align="center">조회된 생산 실적이 없습니다.</TableCell>
                </TableRow>
              )}
              {visibleResults.map((result) => (
                <TableRow key={result.resultId} hover>
                  <TableCell>{result.resultNo}</TableCell>
                  <TableCell>{result.plantCd}</TableCell>
                  <TableCell>{result.resultDt}</TableCell>
                  <TableCell>{result.woId}</TableCell>
                  <TableCell>{result.itemCd}</TableCell>
                  <TableCell align="right">{result.prodQty}</TableCell>
                  <TableCell align="right">{result.goodQty}</TableCell>
                  <TableCell align="right">{result.defectQty || 0}</TableCell>
                  <TableCell>{result.equipmentCd || '-'}</TableCell>
                  <TableCell><Chip label={result.resultStatus || '-'} size="small" color="primary" variant="light" /></TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
        <TablePagination
          component="div"
          count={results.length}
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
        <DialogTitle>생산 실적 등록</DialogTitle>
        <DialogContent dividers>
          <Grid container spacing={2} sx={{ pt: 0.5 }}>
            <Grid size={{ xs: 12, md: 6 }}>
              <FormControl fullWidth size="small" required>
                <InputLabel>작업 오더</InputLabel>
                <Select label="작업 오더" value={form.woId} onChange={(event) => handleFormValue('woId', event.target.value)}>
                  {orders.slice(0, 100).map((order) => (
                    <MenuItem key={order.woId} value={order.woId}>
                      {order.woNo} - {order.itemNm || order.itemCd}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField fullWidth required size="small" label="공장" value={form.plantCd} onChange={(event) => handleFormValue('plantCd', event.target.value)} />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField fullWidth required size="small" label="품목 코드" value={form.itemCd} onChange={(event) => handleFormValue('itemCd', event.target.value)} />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField fullWidth required size="small" type="date" label="실적일" value={form.resultDt} onChange={(event) => handleFormValue('resultDt', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField fullWidth required size="small" type="number" label="생산 수량" value={form.prodQty} onChange={(event) => handleFormValue('prodQty', event.target.value)} />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField fullWidth required size="small" type="number" label="양품 수량" value={form.goodQty} onChange={(event) => handleFormValue('goodQty', event.target.value)} />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField fullWidth size="small" type="number" label="불량 수량" value={form.defectQty} onChange={(event) => handleFormValue('defectQty', event.target.value)} />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField fullWidth required size="small" label="조" value={form.shift} onChange={(event) => handleFormValue('shift', event.target.value)} />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField fullWidth required size="small" label="작업자" value={form.workerId} onChange={(event) => handleFormValue('workerId', event.target.value)} />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField fullWidth required size="small" label="작업장" value={form.workcenterCd} onChange={(event) => handleFormValue('workcenterCd', event.target.value)} />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField fullWidth size="small" label="설비" value={form.equipmentCd} onChange={(event) => handleFormValue('equipmentCd', event.target.value)} />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField fullWidth size="small" label="LOT 번호" value={form.lotNo} onChange={(event) => handleFormValue('lotNo', event.target.value)} />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField fullWidth size="small" label="상태" value={form.resultStatus} onChange={(event) => handleFormValue('resultStatus', event.target.value)} />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField fullWidth required size="small" type="datetime-local" label="시작" value={form.startDtm} onChange={(event) => handleFormValue('startDtm', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField fullWidth size="small" type="datetime-local" label="종료" value={form.endDtm} onChange={(event) => handleFormValue('endDtm', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField fullWidth size="small" type="number" label="작업 시간" value={form.workTime} onChange={(event) => handleFormValue('workTime', event.target.value)} />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField fullWidth size="small" type="number" label="준비 시간" value={form.setupTime} onChange={(event) => handleFormValue('setupTime', event.target.value)} />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField fullWidth size="small" type="number" label="비가동 시간" value={form.downTime} onChange={(event) => handleFormValue('downTime', event.target.value)} />
            </Grid>
            <Grid size={12}>
              <TextField fullWidth multiline minRows={3} label="비고" value={form.resultRmk} onChange={(event) => handleFormValue('resultRmk', event.target.value)} />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>취소</Button>
          <Button variant="contained" onClick={handleSave}>저장</Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={!!message} autoHideDuration={3500} onClose={() => setMessage(null)} anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}>
        {message && <Alert severity={message.severity} variant="filled" onClose={() => setMessage(null)}>{message.text}</Alert>}
      </Snackbar>
    </Stack>
  );
}
