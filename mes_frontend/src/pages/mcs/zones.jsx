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
import { mcsReferenceApi } from 'api/mcs/references';
import { zoneApi } from 'api/mcs/zones';

const emptyForm = {
  plantCd: '',
  warehouseCd: '',
  zoneCd: '',
  zoneNm: '',
  zoneType: '',
  sortSeq: 0,
  useYn: 'Y'
};

function getApiData(response, fallback) {
  return response?.data ?? fallback;
}

export default function McsZones() {
  const [search, setSearch] = useState({ plantCd: '', warehouseCd: '', zoneCd: '', zoneNm: '' });
  const [query, setQuery] = useState({ page: 1, size: 10 });
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingZone, setEditingZone] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [message, setMessage] = useState(null);
  const [pendingAction, setPendingAction] = useState(null);

  const zoneParams = useMemo(() => ({ ...search, ...query }), [search, query]);
  const { data: zoneResponse, error: zoneError, isLoading, mutate } = useSWR(['mcs-zones', zoneParams], () => zoneApi.list(zoneParams));
  const { data: plantResponse } = useSWR('mcs-reference-plants', () => mcsReferenceApi.plants());
  const { data: warehouseResponse } = useSWR('mcs-reference-warehouses', () => mcsReferenceApi.warehouses({ useYn: 'Y' }));
  const { data: zoneTypeResponse } = useSWR('mcs-reference-zone-types', () => mcsReferenceApi.codes('MCS_ZONE_TYPE'));

  const page = getApiData(zoneResponse, { content: [], totalElements: 0, currentPage: 1, size: 10 });
  const plants = getApiData(plantResponse, []);
  const warehouses = getApiData(warehouseResponse, []);
  const zoneTypes = getApiData(zoneTypeResponse, []);
  const isBusy = Boolean(pendingAction);
  const isPending = (actionId) => pendingAction === actionId;

  const filteredWarehouses = useMemo(() => {
    if (!form.plantCd) return warehouses;
    return warehouses.filter((warehouse) => warehouse.plantCd === form.plantCd);
  }, [form.plantCd, warehouses]);

  const handleSearchValue = (field, value) => {
    setSearch((current) => ({ ...current, [field]: value }));
  };

  const handleSearch = () => {
    setQuery((current) => ({ ...current, page: 1 }));
  };

  const handleReset = () => {
    setSearch({ plantCd: '', warehouseCd: '', zoneCd: '', zoneNm: '' });
    setQuery({ page: 1, size: 10 });
  };

  const openCreateDialog = () => {
    setEditingZone(null);
    setForm(emptyForm);
    setDialogOpen(true);
  };

  const openEditDialog = (zone) => {
    setEditingZone(zone);
    setForm({
      plantCd: zone.plantCd || '',
      warehouseCd: zone.warehouseCd || '',
      zoneCd: zone.zoneCd || '',
      zoneNm: zone.zoneNm || '',
      zoneType: zone.zoneType || '',
      sortSeq: zone.sortSeq ?? 0,
      useYn: zone.useYn || 'Y'
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
    setPendingAction('save-zone');
    try {
      const payload = { ...form, sortSeq: Number(form.sortSeq || 0) };
      if (editingZone) {
        await zoneApi.update(editingZone.zoneId, payload);
        setMessage({ severity: 'success', text: 'Zone 정보가 수정되었습니다.' });
      } else {
        await zoneApi.create(payload);
        setMessage({ severity: 'success', text: 'Zone 정보가 등록되었습니다.' });
      }
      setDialogOpen(false);
      await mutate();
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    } finally {
      setPendingAction(null);
    }
  };

  const handleDelete = async (zone) => {
    if (isBusy) return;
    const confirmed = window.confirm(`${zone.zoneCd} Zone을 삭제하시겠습니까?`);
    if (!confirmed) return;

    setPendingAction(`delete-zone-${zone.zoneId}`);
    try {
      await zoneApi.remove(zone.zoneId);
      setMessage({ severity: 'success', text: 'Zone 정보가 삭제되었습니다.' });
      await mutate();
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    } finally {
      setPendingAction(null);
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
          <Typography variant="h3">MCS Zone 관리</Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mt: 0.75 }}>
            창고 내부 구역을 등록하고 로케이션 기준정보의 상위 단위로 관리합니다.
          </Typography>
        </Box>
        <Button variant="contained" startIcon={<PlusOutlined />} disabled={isBusy} onClick={openCreateDialog}>
          Zone 등록
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
                {warehouses
                  .filter((warehouse) => !search.plantCd || warehouse.plantCd === search.plantCd)
                  .map((warehouse) => (
                    <MenuItem key={warehouse.warehouseCd} value={warehouse.warehouseCd}>
                      {warehouse.warehouseNm}
                    </MenuItem>
                  ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField
              fullWidth
              size="small"
              label="Zone 코드"
              value={search.zoneCd}
              onChange={(event) => handleSearchValue('zoneCd', event.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField
              fullWidth
              size="small"
              label="Zone 명"
              value={search.zoneNm}
              onChange={(event) => handleSearchValue('zoneNm', event.target.value)}
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

      <MainCard title="Zone 목록" content={false}>
        {zoneError && <Alert severity="error">{zoneError.message}</Alert>}
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>공장</TableCell>
                <TableCell>창고</TableCell>
                <TableCell>Zone 코드</TableCell>
                <TableCell>Zone 명</TableCell>
                <TableCell>유형</TableCell>
                <TableCell align="right">정렬</TableCell>
                <TableCell>사용</TableCell>
                <TableCell align="right">관리</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {!isLoading && page.content.length === 0 && (
                <TableRow>
                  <TableCell colSpan={8} align="center">
                    조회된 Zone 정보가 없습니다.
                  </TableCell>
                </TableRow>
              )}
              {page.content.map((zone) => (
                <TableRow key={zone.zoneId} hover>
                  <TableCell>{zone.plantNm || zone.plantCd}</TableCell>
                  <TableCell>{zone.warehouseNm || zone.warehouseCd}</TableCell>
                  <TableCell>{zone.zoneCd}</TableCell>
                  <TableCell>{zone.zoneNm}</TableCell>
                  <TableCell>{zone.zoneTypeNm || zone.zoneType}</TableCell>
                  <TableCell align="right">{zone.sortSeq}</TableCell>
                  <TableCell>
                    <Chip
                      label={zone.useYn === 'Y' ? '사용' : '미사용'}
                      size="small"
                      color={zone.useYn === 'Y' ? 'success' : 'default'}
                      variant="light"
                    />
                  </TableCell>
                  <TableCell align="right">
                    <Stack direction="row" spacing={1} sx={{ justifyContent: 'flex-end' }}>
                      <Button size="small" startIcon={<EditOutlined />} disabled={isBusy} onClick={() => openEditDialog(zone)}>
                        수정
                      </Button>
                      <Button
                        size="small"
                        color="error"
                        startIcon={
                          isPending(`delete-zone-${zone.zoneId}`) ? <CircularProgress size={14} color="inherit" /> : <DeleteOutlined />
                        }
                        disabled={isBusy}
                        onClick={() => handleDelete(zone)}
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
        <DialogTitle>{editingZone ? 'Zone 수정' : 'Zone 등록'}</DialogTitle>
        <DialogContent dividers>
          <Grid container spacing={2} sx={{ pt: 0.5 }}>
            <Grid size={{ xs: 12, md: 6 }}>
              <FormControl fullWidth size="small" required disabled={!!editingZone}>
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
              <FormControl fullWidth size="small" required disabled={!!editingZone}>
                <InputLabel>창고</InputLabel>
                <Select label="창고" value={form.warehouseCd} onChange={(event) => handleFormValue('warehouseCd', event.target.value)}>
                  {filteredWarehouses.map((warehouse) => (
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
                disabled={!!editingZone}
                label="Zone 코드"
                value={form.zoneCd}
                onChange={(event) => handleFormValue('zoneCd', event.target.value)}
              />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                fullWidth
                size="small"
                required
                label="Zone 명"
                value={form.zoneNm}
                onChange={(event) => handleFormValue('zoneNm', event.target.value)}
              />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <FormControl fullWidth size="small">
                <InputLabel>Zone 유형</InputLabel>
                <Select label="Zone 유형" value={form.zoneType} onChange={(event) => handleFormValue('zoneType', event.target.value)}>
                  {zoneTypes.map((code) => (
                    <MenuItem key={code.comCd} value={code.comCd}>
                      {code.comNm}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField
                fullWidth
                size="small"
                type="number"
                label="정렬 순서"
                value={form.sortSeq}
                onChange={(event) => handleFormValue('sortSeq', event.target.value)}
              />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <FormControl fullWidth size="small">
                <InputLabel>사용 여부</InputLabel>
                <Select label="사용 여부" value={form.useYn} onChange={(event) => handleFormValue('useYn', event.target.value)}>
                  <MenuItem value="Y">사용</MenuItem>
                  <MenuItem value="N">미사용</MenuItem>
                </Select>
              </FormControl>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>취소</Button>
          <Button
            variant="contained"
            startIcon={isPending('save-zone') ? <CircularProgress size={14} color="inherit" /> : undefined}
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
