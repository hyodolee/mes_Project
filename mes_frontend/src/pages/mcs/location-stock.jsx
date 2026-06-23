import { useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import useSWR from 'swr';

import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Checkbox from '@mui/material/Checkbox';
import CircularProgress from '@mui/material/CircularProgress';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import FormControl from '@mui/material/FormControl';
import FormControlLabel from '@mui/material/FormControlLabel';
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

import { EditOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons';

import MainCard from 'components/MainCard';
import { inventoryApi } from 'api/mcs/inventory';
import { mcsReferenceApi } from 'api/mcs/references';
import { zoneApi } from 'api/mcs/zones';

function getApiData(response, fallback) {
  return response?.data ?? fallback;
}

const defaultSearch = { plantCd: '', warehouseCd: '', zoneId: '', locationCd: '', itemCd: '', lotNo: '', excludeZeroStock: true };

export default function McsLocationStock() {
  const [urlSearchParams] = useSearchParams();
  const initialSearch = {
    ...defaultSearch,
    plantCd: urlSearchParams.get('plantCd') || '',
    warehouseCd: urlSearchParams.get('warehouseCd') || '',
    zoneId: urlSearchParams.get('zoneId') || '',
    locationCd: urlSearchParams.get('locationCd') || '',
    itemCd: urlSearchParams.get('itemCd') || '',
    lotNo: urlSearchParams.get('lotNo') || '',
    excludeZeroStock: urlSearchParams.get('excludeZeroStock') !== 'false'
  };
  const [search, setSearch] = useState(initialSearch);
  const [query, setQuery] = useState({ page: 1, size: 10 });
  const [adjustTarget, setAdjustTarget] = useState(null);
  const [adjustForm, setAdjustForm] = useState({ adjustType: 'ADJ_PLUS', adjustQty: 1, transRmk: '' });
  const [message, setMessage] = useState(null);
  const [pendingAction, setPendingAction] = useState(null);

  const stockParams = useMemo(() => ({ ...search, ...query }), [search, query]);
  const {
    data: stockResponse,
    error: stockError,
    isLoading,
    mutate
  } = useSWR(['mcs-location-stocks', stockParams], () => inventoryApi.stocks(stockParams));
  const { data: plantResponse } = useSWR('mcs-reference-plants', () => mcsReferenceApi.plants());
  const { data: warehouseResponse } = useSWR('mcs-reference-warehouses', () => mcsReferenceApi.warehouses({ useYn: 'Y' }));
  const { data: zoneResponse } = useSWR('mcs-zone-options', () => zoneApi.list({ page: 1, size: 1000 }));

  const page = getApiData(stockResponse, { content: [], totalElements: 0, currentPage: 1, size: 10 });
  const plants = getApiData(plantResponse, []);
  const warehouses = getApiData(warehouseResponse, []);
  const zones = getApiData(zoneResponse, { content: [] }).content || [];
  const isBusy = Boolean(pendingAction);
  const isPending = (actionId) => pendingAction === actionId;

  const filteredWarehouses = useMemo(() => {
    if (!search.plantCd) return warehouses;
    return warehouses.filter((warehouse) => warehouse.plantCd === search.plantCd);
  }, [search.plantCd, warehouses]);

  const filteredZones = useMemo(() => {
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
    setSearch(defaultSearch);
    setQuery({ page: 1, size: 10 });
  };

  const openAdjustDialog = (stock) => {
    setAdjustTarget(stock);
    setAdjustForm({ adjustType: 'ADJ_PLUS', adjustQty: 1, transRmk: '' });
  };

  const handleAdjust = async () => {
    if (isBusy) return;
    setPendingAction('adjust-stock');
    try {
      await inventoryApi.adjustStock(adjustTarget.locStockId, {
        ...adjustForm,
        adjustQty: Number(adjustForm.adjustQty || 0)
      });
      setMessage({ severity: 'success', text: '재고 조정이 완료되었습니다.' });
      setAdjustTarget(null);
      await mutate();
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    } finally {
      setPendingAction(null);
    }
  };

  return (
    <Stack spacing={3}>
      <Box>
        <Typography variant="h3">MCS 로케이션 재고</Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mt: 0.75 }}>
          Location 단위 현재 재고와 가용 재고를 조회하고 시연용 재고 조정을 수행합니다.
        </Typography>
      </Box>

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
                {filteredWarehouses.map((warehouse) => (
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
                {filteredZones.map((zone) => (
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
              label="품목 코드"
              value={search.itemCd}
              onChange={(event) => handleSearchValue('itemCd', event.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField
              fullWidth
              size="small"
              label="LOT 번호"
              value={search.lotNo}
              onChange={(event) => handleSearchValue('lotNo', event.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <FormControlLabel
              control={
                <Checkbox
                  checked={search.excludeZeroStock}
                  onChange={(event) => handleSearchValue('excludeZeroStock', event.target.checked)}
                />
              }
              label="0 재고 제외"
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

      <MainCard title="로케이션 재고 목록" content={false}>
        {stockError && <Alert severity="error">{stockError.message}</Alert>}
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>공장</TableCell>
                <TableCell>창고</TableCell>
                <TableCell>Zone</TableCell>
                <TableCell>Location</TableCell>
                <TableCell>품목</TableCell>
                <TableCell>LOT</TableCell>
                <TableCell align="right">현재고</TableCell>
                <TableCell align="right">예약</TableCell>
                <TableCell align="right">가용</TableCell>
                <TableCell align="right">관리</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {!isLoading && page.content.length === 0 && (
                <TableRow>
                  <TableCell colSpan={10} align="center">
                    조회된 로케이션 재고가 없습니다.
                  </TableCell>
                </TableRow>
              )}
              {page.content.map((stock) => (
                <TableRow key={stock.locStockId} hover>
                  <TableCell>{stock.plantNm || stock.plantCd}</TableCell>
                  <TableCell>{stock.warehouseNm}</TableCell>
                  <TableCell>{stock.zoneNm}</TableCell>
                  <TableCell>{stock.locationCd}</TableCell>
                  <TableCell>
                    <Typography variant="subtitle2">{stock.itemNm || stock.itemCd}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {stock.itemCd}
                    </Typography>
                  </TableCell>
                  <TableCell>{stock.lotNo || '-'}</TableCell>
                  <TableCell align="right">{stock.stockQty}</TableCell>
                  <TableCell align="right">{stock.reservedQty}</TableCell>
                  <TableCell align="right">{stock.availableQty}</TableCell>
                  <TableCell align="right">
                    <Button size="small" startIcon={<EditOutlined />} disabled={isBusy} onClick={() => openAdjustDialog(stock)}>
                      조정
                    </Button>
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

      <Dialog open={!!adjustTarget} onClose={() => setAdjustTarget(null)} fullWidth maxWidth="sm">
        <DialogTitle>재고 조정</DialogTitle>
        <DialogContent dividers>
          {adjustTarget && (
            <Stack spacing={2}>
              <Box>
                <Typography variant="subtitle1">
                  {adjustTarget.locationCd} / {adjustTarget.itemNm || adjustTarget.itemCd}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  현재고 {adjustTarget.stockQty}, 가용 {adjustTarget.availableQty}
                </Typography>
              </Box>
              <FormControl fullWidth size="small">
                <InputLabel>조정 유형</InputLabel>
                <Select
                  label="조정 유형"
                  value={adjustForm.adjustType}
                  onChange={(event) => setAdjustForm((current) => ({ ...current, adjustType: event.target.value }))}
                >
                  <MenuItem value="ADJ_PLUS">재고 증가</MenuItem>
                  <MenuItem value="ADJ_MINUS">재고 감소</MenuItem>
                </Select>
              </FormControl>
              <TextField
                fullWidth
                size="small"
                type="number"
                label="조정 수량"
                value={adjustForm.adjustQty}
                onChange={(event) => setAdjustForm((current) => ({ ...current, adjustQty: event.target.value }))}
              />
              <TextField
                fullWidth
                multiline
                minRows={3}
                label="조정 사유"
                value={adjustForm.transRmk}
                onChange={(event) => setAdjustForm((current) => ({ ...current, transRmk: event.target.value }))}
              />
            </Stack>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAdjustTarget(null)}>취소</Button>
          <Button
            variant="contained"
            startIcon={isPending('adjust-stock') ? <CircularProgress size={14} color="inherit" /> : undefined}
            disabled={isBusy}
            onClick={handleAdjust}
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
