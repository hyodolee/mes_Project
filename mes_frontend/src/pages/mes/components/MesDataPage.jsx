import { useMemo, useState } from 'react';
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
import TablePagination from '@mui/material/TablePagination';
import TableRow from '@mui/material/TableRow';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';

import { ReloadOutlined, SearchOutlined } from '@ant-design/icons';

import MainCard from 'components/MainCard';

function getApiData(response, fallback) {
  return response?.data ?? fallback;
}

export default function MesDataPage({ title, description, cardTitle = '목록', swrKey, fetcher, initialSearch, filters, columns, getRowId, renderActions }) {
  const [search, setSearch] = useState(initialSearch);
  const [query, setQuery] = useState(initialSearch);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);

  const { data, error, isLoading, mutate } = useSWR([swrKey, query], () => fetcher(query));
  const rows = getApiData(data, []);

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
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ justifyContent: 'space-between', alignItems: { xs: 'flex-start', md: 'center' } }}>
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
                  <Select label={filter.label} value={search[filter.field]} onChange={(event) => handleValue(filter.field, event.target.value)}>
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
        <TablePagination
          component="div"
          count={rows.length}
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
    </Stack>
  );
}
