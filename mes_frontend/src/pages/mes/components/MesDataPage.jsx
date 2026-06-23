import { useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import useSWR from 'swr';

import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
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
import TableRow from '@mui/material/TableRow';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';

import { ReloadOutlined, SearchOutlined } from '@ant-design/icons';

import MainCard from 'components/MainCard';
import TablePager from 'components/TablePager';

function getApiData(response, fallback) {
  return response?.data ?? fallback;
}

function makeInitialSearch(initialSearch, searchParams) {
  const nextSearch = { ...initialSearch };

  Object.keys(nextSearch).forEach((field) => {
    const value = searchParams.get(field);
    if (value !== null) {
      nextSearch[field] = value;
    }
  });

  return nextSearch;
}

function matchesSearch(row, query) {
  return Object.entries(query).every(([field, value]) => {
    if (value === '' || value === null || value === undefined) {
      return true;
    }

    if (!Object.prototype.hasOwnProperty.call(row, field)) {
      return true;
    }

    return String(row[field]) === String(value);
  });
}

export default function MesDataPage({
  title,
  description,
  cardTitle = '목록',
  swrKey,
  fetcher,
  initialSearch,
  filters,
  columns,
  getRowId,
  renderActions
}) {
  const [urlSearchParams] = useSearchParams();
  const initialSearchFromUrl = useMemo(() => makeInitialSearch(initialSearch, urlSearchParams), [initialSearch, urlSearchParams]);
  const [search, setSearch] = useState(initialSearchFromUrl);
  const [query, setQuery] = useState(initialSearchFromUrl);
  const [page, setPage] = useState(0);
  const [rowsPerPage] = useState(10);

  const { data, error, isLoading, mutate } = useSWR([swrKey, query], () => fetcher(query));
  const rawRows = getApiData(data, []);
  const rows = useMemo(() => rawRows.filter((row) => matchesSearch(row, query)), [rawRows, query]);

  const visibleRows = useMemo(() => {
    const start = page * rowsPerPage;
    return rows.slice(start, start + rowsPerPage);
  }, [page, rows, rowsPerPage]);

  const handleValue = (field, value) => {
    setSearch((current) => ({ ...current, [field]: value }));
  };

  const handleSearch = () => {
    setPage(0);
    setQuery(search);
  };

  const handleReset = () => {
    setSearch(initialSearch);
    setQuery(initialSearch);
    setPage(0);
  };

  return (
    <Stack spacing={3}>
      <Stack
        direction={{ xs: 'column', md: 'row' }}
        spacing={2}
        sx={{ justifyContent: 'space-between', alignItems: { xs: 'flex-start', md: 'center' } }}
      >
        <Box>
          <Typography variant="h3">{title}</Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mt: 0.75 }}>
            {description}
          </Typography>
        </Box>
        {renderActions?.({ mutate, query })}
      </Stack>

      <MainCard title="검색 조건">
        <Grid container spacing={2}>
          {filters.map((filter) => (
            <Grid key={filter.field} size={{ xs: 12, md: filter.md || 2.4 }}>
              {filter.type === 'select' ? (
                <FormControl fullWidth size="small">
                  <InputLabel>{filter.label}</InputLabel>
                  <Select
                    label={filter.label}
                    value={search[filter.field]}
                    onChange={(event) => handleValue(filter.field, event.target.value)}
                  >
                    <MenuItem value="">전체</MenuItem>
                    {(filter.options || []).map((option) => (
                      <MenuItem key={option.value} value={option.value}>
                        {option.label}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              ) : (
                <TextField
                  fullWidth
                  size="small"
                  type={filter.type === 'date' ? 'date' : 'text'}
                  label={filter.label}
                  value={search[filter.field]}
                  onChange={(event) => handleValue(filter.field, event.target.value)}
                  slotProps={filter.type === 'date' ? { inputLabel: { shrink: true } } : undefined}
                />
              )}
            </Grid>
          ))}
          <Grid size={{ xs: 12, md: 12 }}>
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

      <MainCard title={cardTitle} content={false}>
        {error && <Alert severity="error">{error.message}</Alert>}
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                {columns.map((column) => (
                  <TableCell key={column.field || column.header} align={column.align}>
                    {column.header}
                  </TableCell>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {!isLoading && rows.length === 0 && (
                <TableRow>
                  <TableCell colSpan={columns.length} align="center">
                    조회된 데이터가 없습니다.
                  </TableCell>
                </TableRow>
              )}
              {visibleRows.map((row, index) => (
                <TableRow key={getRowId ? getRowId(row) : index} hover>
                  {columns.map((column) => (
                    <TableCell key={column.field || column.header} align={column.align}>
                      {column.render ? column.render(row) : row[column.field] || '-'}
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
        <TablePager page={page + 1} count={Math.ceil(rows.length / rowsPerPage)} onChange={(nextPage) => setPage(nextPage - 1)} />
      </MainCard>
    </Stack>
  );
}
