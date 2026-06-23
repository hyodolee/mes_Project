import { useMemo, useState } from 'react';
import useSWR from 'swr';

import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
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
import TablePager from 'components/TablePager';
import TableRow from '@mui/material/TableRow';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';

import { ReloadOutlined, SearchOutlined } from '@ant-design/icons';

import MainCard from 'components/MainCard';
import { inventoryApi } from 'api/mcs/inventory';
import { mcsReferenceApi } from 'api/mcs/references';

function getApiData(response, fallback) {
  return response?.data ?? fallback;
}

function getTransColor(transType) {
  if (transType === 'TF_IN' || transType === 'ADJ_PLUS') return 'success';
  if (transType === 'TF_OUT' || transType === 'ADJ_MINUS') return 'warning';
  return 'primary';
}

export default function McsInventoryTransactions() {
  const [search, setSearch] = useState({ plantCd: '', fromDate: '', toDate: '', transType: '', itemCd: '', locationCd: '' });
  const [query, setQuery] = useState({ page: 1, size: 10 });

  const transParams = useMemo(() => ({ ...search, ...query }), [search, query]);
  const {
    data: transResponse,
    error: transError,
    isLoading
  } = useSWR(['mcs-inventory-transactions', transParams], () => inventoryApi.transactions(transParams));
  const { data: plantResponse } = useSWR('mcs-reference-plants', () => mcsReferenceApi.plants());
  const { data: typeResponse } = useSWR('mcs-reference-inventory-transaction-types', () => mcsReferenceApi.codes('MCS_INV_TX_TYPE'));

  const page = getApiData(transResponse, { content: [], totalElements: 0, currentPage: 1, size: 10 });
  const plants = getApiData(plantResponse, []);
  const transTypes = getApiData(typeResponse, []);

  const handleSearchValue = (field, value) => {
    setSearch((current) => ({ ...current, [field]: value }));
  };

  const handleSearch = () => {
    setQuery((current) => ({ ...current, page: 1 }));
  };

  const handleReset = () => {
    setSearch({ plantCd: '', fromDate: '', toDate: '', transType: '', itemCd: '', locationCd: '' });
    setQuery({ page: 1, size: 10 });
  };

  const getTransLabel = (history) =>
    history.transTypeNm || transTypes.find((type) => type.comCd === history.transType)?.comNm || history.transType;

  return (
    <Stack spacing={3}>
      <Box>
        <Typography variant="h3">MCS 재고 이력</Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mt: 0.75 }}>
          이동 완료와 재고 조정으로 생성된 Location 입출고 이력을 추적합니다.
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
              <InputLabel>유형</InputLabel>
              <Select label="유형" value={search.transType} onChange={(event) => handleSearchValue('transType', event.target.value)}>
                <MenuItem value="">전체</MenuItem>
                {transTypes.map((type) => (
                  <MenuItem key={type.comCd} value={type.comCd}>
                    {type.comNm}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <TextField
              fullWidth
              size="small"
              type="date"
              label="From"
              value={search.fromDate}
              onChange={(event) => handleSearchValue('fromDate', event.target.value)}
              slotProps={{ inputLabel: { shrink: true } }}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <TextField
              fullWidth
              size="small"
              type="date"
              label="To"
              value={search.toDate}
              onChange={(event) => handleSearchValue('toDate', event.target.value)}
              slotProps={{ inputLabel: { shrink: true } }}
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
              label="Location 코드"
              value={search.locationCd}
              onChange={(event) => handleSearchValue('locationCd', event.target.value)}
            />
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

      <MainCard title="재고 이력 목록" content={false}>
        {transError && <Alert severity="error">{transError.message}</Alert>}
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>일시</TableCell>
                <TableCell>공장</TableCell>
                <TableCell>Location</TableCell>
                <TableCell>품목</TableCell>
                <TableCell>LOT</TableCell>
                <TableCell>유형</TableCell>
                <TableCell align="right">수량</TableCell>
                <TableCell align="right">전</TableCell>
                <TableCell align="right">후</TableCell>
                <TableCell>참조</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {!isLoading && page.content.length === 0 && (
                <TableRow>
                  <TableCell colSpan={10} align="center">
                    조회된 재고 이력이 없습니다.
                  </TableCell>
                </TableRow>
              )}
              {page.content.map((history) => (
                <TableRow key={history.locTransId} hover>
                  <TableCell>{history.regDtm}</TableCell>
                  <TableCell>{history.plantNm || history.plantCd}</TableCell>
                  <TableCell>{history.locationCd}</TableCell>
                  <TableCell>
                    <Typography variant="subtitle2">{history.itemNm || history.itemCd}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {history.itemCd}
                    </Typography>
                  </TableCell>
                  <TableCell>{history.lotNo || '-'}</TableCell>
                  <TableCell>
                    <Chip label={getTransLabel(history)} size="small" color={getTransColor(history.transType)} variant="light" />
                  </TableCell>
                  <TableCell align="right">{history.transQty}</TableCell>
                  <TableCell align="right">{history.beforeQty}</TableCell>
                  <TableCell align="right">{history.afterQty}</TableCell>
                  <TableCell>
                    {history.refType || '-'} {history.refNo || ''}
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
    </Stack>
  );
}
