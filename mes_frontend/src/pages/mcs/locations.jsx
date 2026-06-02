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
import LinearProgress from '@mui/material/LinearProgress';
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
import { locationApi } from 'api/mcs/locations';
import { mcsReferenceApi } from 'api/mcs/references';
import { zoneApi } from 'api/mcs/zones';

const emptyForm = {
  zoneId: '',
  locationCd: '',
  locationNm: '',
  maxCapacity: 0,
  locationStatus: 'EMPTY',
  useYn: 'Y'
};

function getApiData(response, fallback) {
  return response?.data ?? fallback;
}

export default function McsLocations() {
  const [search, setSearch] = useState({ plantCd: '', warehouseCd: '', zoneId: '', locationCd: '', locationNm: '', locationStatus: '' });
  const [query, setQuery] = useState({ page: 1, size: 10 });
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingLocation, setEditingLocation] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [message, setMessage] = useState(null);
  const [pendingAction, setPendingAction] = useState(null);

  const locationParams = useMemo(() => ({ ...search, ...query }), [search, query]);
  const { data: locationResponse, error: locationError, isLoading, mutate } = useSWR(['mcs-locations', locationParams], () => locationApi.list(locationParams));
  const { data: plantResponse } = useSWR('mcs-reference-plants', () => mcsReferenceApi.plants());
  const { data: warehouseResponse } = useSWR('mcs-reference-warehouses', () => mcsReferenceApi.warehouses({ useYn: 'Y' }));
  const { data: zoneResponse } = useSWR('mcs-zone-options', () => zoneApi.list({ page: 1, size: 1000 }));
  const { data: statusResponse } = useSWR('mcs-reference-location-statuses', () => mcsReferenceApi.codes('MCS_LOC_STATUS'));

  const page = getApiData(locationResponse, { content: [], totalElements: 0, currentPage: 1, size: 10 });
  const plants = getApiData(plantResponse, []);
  const warehouses = getApiData(warehouseResponse, []);
  const zones = getApiData(zoneResponse, { content: [] }).content || [];
  const statuses = getApiData(statusResponse, []);
  const isBusy = Boolean(pendingAction);
  const isPending = (actionId) => pendingAction === actionId;

  const filteredSearchWarehouses = useMemo(() => {
    if (!search.plantCd) return warehouses;
    return warehouses.filter((warehouse) => warehouse.plantCd === search.plantCd);
  }, [search.plantCd, warehouses]);

  const filteredSearchZones = useMemo(() => {
    return zones.filter((zone) => {
      if (search.plantCd && zone.plantCd !== search.plantCd) return false;
      if (search.warehouseCd && zone.warehouseCd !== search.warehouseCd) return false;
      return true;
    });
  }, [search.plantCd, search.warehouseCd, zones]);

  const handleSearchValue = (field, value) => {
    setSearch((current) => {
      const next = { ...current, [field]: value };
      if (field === 'plantCd') {
        next.warehouseCd = '';
        next.zoneId = '';
      }
      if (field === 'warehouseCd') next.zoneId = '';
      return next;
    });
  };

  const handleSearch = () => {
    setQuery((current) => ({ ...current, page: 1 }));
  };

  const handleReset = () => {
    setSearch({ plantCd: '', warehouseCd: '', zoneId: '', locationCd: '', locationNm: '', locationStatus: '' });
    setQuery({ page: 1, size: 10 });
  };

  const openCreateDialog = () => {
    setEditingLocation(null);
    setForm(emptyForm);
    setDialogOpen(true);
  };

  const openEditDialog = (location) => {
    setEditingLocation(location);
    setForm({
      zoneId: location.zoneId || '',
      locationCd: location.locationCd || '',
      locationNm: location.locationNm || '',
      maxCapacity: location.maxCapacity ?? 0,
      locationStatus: location.locationStatus || 'EMPTY',
      useYn: location.useYn || 'Y'
    });
    setDialogOpen(true);
  };

  const handleFormValue = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const handleSave = async () => {
    if (isBusy) return;
    setPendingAction('save-location');
    try {
      const payload = {
        ...form,
        zoneId: form.zoneId ? Number(form.zoneId) : null,
        maxCapacity: Number(form.maxCapacity || 0)
      };

      if (editingLocation) {
        await locationApi.update(editingLocation.locationId, payload);
        setMessage({ severity: 'success', text: 'Location 정보가 수정되었습니다.' });
      } else {
        await locationApi.create(payload);
        setMessage({ severity: 'success', text: 'Location 정보가 등록되었습니다.' });
      }

      setDialogOpen(false);
      await mutate();
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    } finally {
      setPendingAction(null);
    }
  };

  const handleDelete = async (location) => {
    if (isBusy) return;
    const confirmed = window.confirm(`${location.locationCd} Location을 삭제하시겠습니까?`);
    if (!confirmed) return;

    setPendingAction(`delete-location-${location.locationId}`);
    try {
      await locationApi.remove(location.locationId);
      setMessage({ severity: 'success', text: 'Location 정보가 삭제되었습니다.' });
      await mutate();
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    } finally {
      setPendingAction(null);
    }
  };

  const getStatusLabel = (location) => location.locationStatusNm || statuses.find((status) => status.comCd === location.locationStatus)?.comNm || location.locationStatus;

  return (
    <Stack spacing={3}>
      <Stack
        direction={{ xs: 'column', md: 'row' }}
        spacing={2}
        sx={{ justifyContent: 'space-between', alignItems: { xs: 'flex-start', md: 'center' } }}
      >
        <Box>
          <Typography variant="h3">MCS Location 관리</Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mt: 0.75 }}>
            Zone 하위의 로케이션을 등록하고 보관 용량, 사용 여부, 현재 상태를 관리합니다.
          </Typography>
        </Box>
        <Button variant="contained" startIcon={<PlusOutlined />} disabled={isBusy} onClick={openCreateDialog}>
          Location 등록
        </Button>
      </Stack>

      <MainCard title="검색 조건">
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 2.4 }}>
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
          <Grid size={{ xs: 12, md: 2.4 }}>
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
          <Grid size={{ xs: 12, md: 2.4 }}>
            <FormControl fullWidth size="small">
              <InputLabel>Zone</InputLabel>
              <Select label="Zone" value={search.zoneId} onChange={(event) => handleSearchValue('zoneId', event.target.value)}>
                <MenuItem value="">전체</MenuItem>
                {filteredSearchZones.map((zone) => (
                  <MenuItem key={zone.zoneId} value={zone.zoneId}>
                    {zone.zoneCd} - {zone.zoneNm}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <TextField
              fullWidth
              size="small"
              label="Location 코드"
              value={search.locationCd}
              onChange={(event) => handleSearchValue('locationCd', event.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <TextField
              fullWidth
              size="small"
              label="Location 명"
              value={search.locationNm}
              onChange={(event) => handleSearchValue('locationNm', event.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <FormControl fullWidth size="small">
              <InputLabel>상태</InputLabel>
              <Select label="상태" value={search.locationStatus} onChange={(event) => handleSearchValue('locationStatus', event.target.value)}>
                <MenuItem value="">전체</MenuItem>
                {statuses.map((status) => (
                  <MenuItem key={status.comCd} value={status.comCd}>
                    {status.comNm}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid size={{ xs: 12, md: 9 }}>
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

      <MainCard title="Location 목록" content={false}>
        {locationError && <Alert severity="error">{locationError.message}</Alert>}
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>공장</TableCell>
                <TableCell>창고</TableCell>
                <TableCell>Zone</TableCell>
                <TableCell>Location 코드</TableCell>
                <TableCell>Location 명</TableCell>
                <TableCell align="right">사용량</TableCell>
                <TableCell>상태</TableCell>
                <TableCell>사용</TableCell>
                <TableCell align="right">관리</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {!isLoading && page.content.length === 0 && (
                <TableRow>
                  <TableCell colSpan={9} align="center">
                    조회된 Location 정보가 없습니다.
                  </TableCell>
                </TableRow>
              )}
              {page.content.map((location) => {
                const capacity = Number(location.maxCapacity || 0);
                const usage = Number(location.currentUsage || 0);
                const usageRate = capacity > 0 ? Math.min((usage / capacity) * 100, 100) : 0;

                return (
                  <TableRow key={location.locationId} hover>
                    <TableCell>{location.plantNm || location.plantCd}</TableCell>
                    <TableCell>{location.warehouseNm || location.warehouseCd}</TableCell>
                    <TableCell>{location.zoneNm || location.zoneCd}</TableCell>
                    <TableCell>{location.locationCd}</TableCell>
                    <TableCell>{location.locationNm}</TableCell>
                    <TableCell align="right" sx={{ minWidth: 150 }}>
                      <Stack spacing={0.5}>
                        <Typography variant="body2">
                          {usage} / {capacity}
                        </Typography>
                        <LinearProgress variant="determinate" value={usageRate} />
                      </Stack>
                    </TableCell>
                    <TableCell>{getStatusLabel(location)}</TableCell>
                    <TableCell>
                      <Chip
                        label={location.useYn === 'Y' ? '사용' : '미사용'}
                        size="small"
                        color={location.useYn === 'Y' ? 'success' : 'default'}
                        variant="light"
                      />
                    </TableCell>
                    <TableCell align="right">
                      <Stack direction="row" spacing={1} sx={{ justifyContent: 'flex-end' }}>
                        <Button size="small" startIcon={<EditOutlined />} disabled={isBusy} onClick={() => openEditDialog(location)}>
                          수정
                        </Button>
                        <Button
                          size="small"
                          color="error"
                          startIcon={isPending(`delete-location-${location.locationId}`) ? <CircularProgress size={14} color="inherit" /> : <DeleteOutlined />}
                          disabled={isBusy}
                          onClick={() => handleDelete(location)}
                        >
                          삭제
                        </Button>
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
          count={page.totalElements}
          page={Math.max((page.currentPage || 1) - 1, 0)}
          rowsPerPage={page.size || query.size}
          rowsPerPageOptions={[10, 20, 50]}
          onPageChange={(_, nextPage) => setQuery((current) => ({ ...current, page: nextPage + 1 }))}
          onRowsPerPageChange={(event) => setQuery({ page: 1, size: Number(event.target.value) })}
        />
      </MainCard>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth maxWidth="md">
        <DialogTitle>{editingLocation ? 'Location 수정' : 'Location 등록'}</DialogTitle>
        <DialogContent dividers>
          <Grid container spacing={2} sx={{ pt: 0.5 }}>
            <Grid size={{ xs: 12, md: 6 }}>
              <FormControl fullWidth size="small" required disabled={!!editingLocation}>
                <InputLabel>Zone</InputLabel>
                <Select label="Zone" value={form.zoneId} onChange={(event) => handleFormValue('zoneId', event.target.value)}>
                  {zones.map((zone) => (
                    <MenuItem key={zone.zoneId} value={zone.zoneId}>
                      {zone.plantNm} / {zone.warehouseNm} / {zone.zoneCd} - {zone.zoneNm}
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
                disabled={!!editingLocation}
                label="Location 코드"
                value={form.locationCd}
                onChange={(event) => handleFormValue('locationCd', event.target.value)}
              />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                fullWidth
                size="small"
                required
                label="Location 명"
                value={form.locationNm}
                onChange={(event) => handleFormValue('locationNm', event.target.value)}
              />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                fullWidth
                size="small"
                type="number"
                label="최대 용량"
                value={form.maxCapacity}
                onChange={(event) => handleFormValue('maxCapacity', event.target.value)}
              />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <FormControl fullWidth size="small">
                <InputLabel>상태</InputLabel>
                <Select label="상태" value={form.locationStatus} onChange={(event) => handleFormValue('locationStatus', event.target.value)}>
                  {statuses.map((status) => (
                    <MenuItem key={status.comCd} value={status.comCd}>
                      {status.comNm}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
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
            startIcon={isPending('save-location') ? <CircularProgress size={14} color="inherit" /> : undefined}
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
