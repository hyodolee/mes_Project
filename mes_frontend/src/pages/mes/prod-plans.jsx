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
import TablePager from 'components/TablePager';
import TableRow from '@mui/material/TableRow';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';

import { PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons';

import MainCard from 'components/MainCard';
import { mesMasterApi } from 'api/mes/master';
import { mesPlanningApi } from 'api/mes/planning';

const emptyForm = {
  plantCd: '',
  planNo: '',
  planDt: '',
  planType: 'NORMAL',
  itemCd: '',
  planQty: 1,
  planStartDt: '',
  planEndDt: '',
  priority: 3,
  orderNo: '',
  customerCd: '',
  customerNm: '',
  deliveryDt: '',
  planStatus: 'PLANNED',
  planRmk: ''
};

function getApiData(response, fallback) {
  return response?.data ?? fallback;
}

function statusColor(status) {
  if (status === 'COMPLETED' || status === '완료') return 'success';
  if (status === 'CONFIRMED' || status === '확정') return 'info';
  if (status === 'CANCELLED' || status === '취소') return 'default';
  return 'primary';
}

function today() {
  return new Date().toISOString().slice(0, 10);
}

export default function MesProdPlans() {
  const [search, setSearch] = useState({ plantCd: '', itemCd: '', planStatus: '', planFromDt: '', planToDt: '' });
  const [query, setQuery] = useState(search);
  const [page, setPage] = useState(0);
  const [rowsPerPage] = useState(10);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [message, setMessage] = useState(null);

  const {
    data: planResponse,
    error: planError,
    isLoading,
    mutate
  } = useSWR(['mes-prod-plans', query], () => mesPlanningApi.prodPlans(query));
  const { data: plantResponse } = useSWR('mes-plants', () => mesMasterApi.plants({ useYn: 'Y' }));
  const { data: itemResponse } = useSWR('mes-items', () => mesMasterApi.items({ useYn: 'Y', itemNm: '' }));

  const plans = getApiData(planResponse, []);
  const plants = getApiData(plantResponse, []);
  const items = getApiData(itemResponse, []);

  const visiblePlans = useMemo(() => {
    const start = page * rowsPerPage;
    return plans.slice(start, start + rowsPerPage);
  }, [page, plans, rowsPerPage]);

  const openCreateDialog = () => {
    const baseDate = today();
    setForm({
      ...emptyForm,
      planNo: `PP${baseDate.replaceAll('-', '')}${String(Date.now()).slice(-4)}`,
      planDt: baseDate,
      planStartDt: baseDate,
      planEndDt: baseDate
    });
    setDialogOpen(true);
  };

  const handleSearchValue = (field, value) => setSearch((current) => ({ ...current, [field]: value }));
  const handleFormValue = (field, value) => setForm((current) => ({ ...current, [field]: value }));

  const handleSearch = () => {
    setPage(0);
    setQuery(search);
  };

  const handleReset = () => {
    const next = { plantCd: '', itemCd: '', planStatus: '', planFromDt: '', planToDt: '' };
    setSearch(next);
    setQuery(next);
    setPage(0);
  };

  const handleSave = async () => {
    try {
      await mesPlanningApi.createProdPlan({
        ...form,
        planQty: Number(form.planQty || 0),
        priority: Number(form.priority || 0)
      });
      setMessage({ severity: 'success', text: '생산 계획이 등록되었습니다.' });
      setDialogOpen(false);
      await mutate();
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    }
  };

  return (
    <Stack spacing={3}>
      <Stack
        direction={{ xs: 'column', md: 'row' }}
        spacing={2}
        sx={{ justifyContent: 'space-between', alignItems: { xs: 'flex-start', md: 'center' } }}
      >
        <Box>
          <Typography variant="h3">MES 생산 계획</Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mt: 0.75 }}>
            생산 계획의 기간, 품목, 상태를 조회하고 신규 계획을 등록합니다.
          </Typography>
        </Box>
        <Button variant="contained" startIcon={<PlusOutlined />} onClick={openCreateDialog}>
          생산 계획 등록
        </Button>
      </Stack>

      <MainCard title="검색 조건">
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <TextField
              fullWidth
              size="small"
              label="공장"
              value={search.plantCd}
              onChange={(event) => handleSearchValue('plantCd', event.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <TextField
              fullWidth
              size="small"
              label="품목 코드"
              value={search.itemCd}
              onChange={(event) => handleSearchValue('itemCd', event.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <TextField
              fullWidth
              size="small"
              label="상태"
              value={search.planStatus}
              onChange={(event) => handleSearchValue('planStatus', event.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <TextField
              fullWidth
              size="small"
              type="date"
              label="From"
              value={search.planFromDt}
              onChange={(event) => handleSearchValue('planFromDt', event.target.value)}
              slotProps={{ inputLabel: { shrink: true } }}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <TextField
              fullWidth
              size="small"
              type="date"
              label="To"
              value={search.planToDt}
              onChange={(event) => handleSearchValue('planToDt', event.target.value)}
              slotProps={{ inputLabel: { shrink: true } }}
            />
          </Grid>
          <Grid size={12}>
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

      <MainCard title="생산 계획 목록" content={false}>
        {planError && <Alert severity="error">{planError.message}</Alert>}
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>계획번호</TableCell>
                <TableCell>공장</TableCell>
                <TableCell>계획일</TableCell>
                <TableCell>품목</TableCell>
                <TableCell align="right">계획수량</TableCell>
                <TableCell align="right">실적수량</TableCell>
                <TableCell>기간</TableCell>
                <TableCell>납기</TableCell>
                <TableCell>상태</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {!isLoading && plans.length === 0 && (
                <TableRow>
                  <TableCell colSpan={9} align="center">
                    조회된 생산 계획이 없습니다.
                  </TableCell>
                </TableRow>
              )}
              {visiblePlans.map((plan) => (
                <TableRow key={plan.planId} hover>
                  <TableCell>{plan.planNo}</TableCell>
                  <TableCell>{plan.plantNm || plan.plantCd}</TableCell>
                  <TableCell>{plan.planDt}</TableCell>
                  <TableCell>
                    {plan.itemNm || plan.itemCd} ({plan.itemCd})
                  </TableCell>
                  <TableCell align="right">{plan.planQty}</TableCell>
                  <TableCell align="right">{plan.resultQty || 0}</TableCell>
                  <TableCell>
                    {plan.planStartDt || '-'} ~ {plan.planEndDt || '-'}
                  </TableCell>
                  <TableCell>{plan.deliveryDt || '-'}</TableCell>
                  <TableCell>
                    <Chip label={plan.planStatus} size="small" color={statusColor(plan.planStatus)} variant="light" />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
        <TablePager page={page + 1} count={Math.ceil(plans.length / rowsPerPage)} onChange={(nextPage) => setPage(nextPage - 1)} />
      </MainCard>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth maxWidth="md">
        <DialogTitle>생산 계획 등록</DialogTitle>
        <DialogContent dividers>
          <Grid container spacing={2} sx={{ pt: 0.5 }}>
            <Grid size={{ xs: 12, md: 6 }}>
              <FormControl fullWidth size="small" required>
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
                required
                size="small"
                label="계획번호"
                value={form.planNo}
                onChange={(event) => handleFormValue('planNo', event.target.value)}
              />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <FormControl fullWidth size="small" required>
                <InputLabel>품목</InputLabel>
                <Select label="품목" value={form.itemCd} onChange={(event) => handleFormValue('itemCd', event.target.value)}>
                  {items.slice(0, 100).map((item) => (
                    <MenuItem key={`${item.plantCd}-${item.itemCd}`} value={item.itemCd}>
                      {item.itemCd} - {item.itemNm}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                fullWidth
                required
                size="small"
                type="number"
                label="계획수량"
                value={form.planQty}
                onChange={(event) => handleFormValue('planQty', event.target.value)}
              />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                fullWidth
                required
                size="small"
                type="date"
                label="계획일"
                value={form.planDt}
                onChange={(event) => handleFormValue('planDt', event.target.value)}
                slotProps={{ inputLabel: { shrink: true } }}
              />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                fullWidth
                required
                size="small"
                label="계획유형"
                value={form.planType}
                onChange={(event) => handleFormValue('planType', event.target.value)}
              />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                fullWidth
                required
                size="small"
                type="date"
                label="시작일"
                value={form.planStartDt}
                onChange={(event) => handleFormValue('planStartDt', event.target.value)}
                slotProps={{ inputLabel: { shrink: true } }}
              />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                fullWidth
                required
                size="small"
                type="date"
                label="종료일"
                value={form.planEndDt}
                onChange={(event) => handleFormValue('planEndDt', event.target.value)}
                slotProps={{ inputLabel: { shrink: true } }}
              />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField
                fullWidth
                size="small"
                type="number"
                label="우선순위"
                value={form.priority}
                onChange={(event) => handleFormValue('priority', event.target.value)}
              />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField
                fullWidth
                size="small"
                label="수주번호"
                value={form.orderNo}
                onChange={(event) => handleFormValue('orderNo', event.target.value)}
              />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField
                fullWidth
                size="small"
                type="date"
                label="납기"
                value={form.deliveryDt}
                onChange={(event) => handleFormValue('deliveryDt', event.target.value)}
                slotProps={{ inputLabel: { shrink: true } }}
              />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                fullWidth
                size="small"
                label="고객코드"
                value={form.customerCd}
                onChange={(event) => handleFormValue('customerCd', event.target.value)}
              />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                fullWidth
                size="small"
                label="고객명"
                value={form.customerNm}
                onChange={(event) => handleFormValue('customerNm', event.target.value)}
              />
            </Grid>
            <Grid size={12}>
              <TextField
                fullWidth
                multiline
                minRows={3}
                label="비고"
                value={form.planRmk}
                onChange={(event) => handleFormValue('planRmk', event.target.value)}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>취소</Button>
          <Button variant="contained" onClick={handleSave}>
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
