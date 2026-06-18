import { useMemo, useState } from 'react';
import useSWR from 'swr';

import Box from '@mui/material/Box';
import Chip from '@mui/material/Chip';
import LinearProgress from '@mui/material/LinearProgress';
import Pagination from '@mui/material/Pagination';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';

import MainCard from 'components/MainCard';
import { bomApi } from 'api/mes/bom';

const PAGE_SIZE = 10;

function getApiData(response, fallback) {
  return response?.data ?? fallback;
}

function num(value) {
  if (value === null || value === undefined) return '-';
  return Number(value).toLocaleString('ko-KR');
}

export default function MesBom() {
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(1);

  const { data, error, isLoading } = useSWR(
    ['mes-boms', keyword, page],
    () => bomApi.list({ keyword: keyword.trim(), page, size: PAGE_SIZE, useYn: 'Y' }),
    { revalidateOnFocus: false, keepPreviousData: true }
  );

  const pageData = useMemo(() => getApiData(data, { content: [], totalPages: 0, totalElements: 0 }), [data]);
  const rows = pageData.content ?? [];
  const totalPages = pageData.totalPages ?? 0;

  const handleSearch = (value) => {
    setKeyword(value);
    setPage(1);
  };

  return (
    <MainCard title="BOM (자재명세서)">
      <Stack spacing={2}>
        <Typography variant="body2" color="text.secondary">
          제품을 만드는 데 필요한 자재 구성과 소요량을 정의합니다. 모품목(완제품/반제품) → 자품목(투입 자재) 관계입니다.
        </Typography>

        <TextField
          size="small"
          label="품목코드 / 품목명 검색"
          value={keyword}
          onChange={(event) => handleSearch(event.target.value)}
          sx={{ maxWidth: 320 }}
        />

        {isLoading && <LinearProgress />}
        {error && (
          <Typography color="error" variant="body2">
            BOM 목록을 불러오지 못했습니다. {error.message}
          </Typography>
        )}

        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>모품목</TableCell>
                <TableCell>자품목 (투입 자재)</TableCell>
                <TableCell align="right">소요량</TableCell>
                <TableCell align="right">손실율(%)</TableCell>
                <TableCell align="center">레벨</TableCell>
                <TableCell>투입공정</TableCell>
                <TableCell align="center">사용</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.length === 0 && !isLoading && (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    등록된 BOM이 없습니다.
                  </TableCell>
                </TableRow>
              )}
              {rows.map((row) => (
                <TableRow key={row.bomId} hover>
                  <TableCell>
                    <Typography variant="body2">{row.parentItemNm || row.parentItemCd}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {row.parentItemCd}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2">{row.childItemNm || row.childItemCd}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {row.childItemCd}
                    </Typography>
                  </TableCell>
                  <TableCell align="right">{num(row.bomQty)}</TableCell>
                  <TableCell align="right">{num(row.lossRate)}</TableCell>
                  <TableCell align="center">{row.bomLevel ?? '-'}</TableCell>
                  <TableCell>{row.processCd || '-'}</TableCell>
                  <TableCell align="center">
                    <Chip size="small" label={row.useYn === 'Y' ? '사용' : '미사용'} color={row.useYn === 'Y' ? 'success' : 'default'} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>

        {totalPages > 1 && (
          <Box display="flex" justifyContent="center">
            <Pagination count={totalPages} page={page} onChange={(event, value) => setPage(value)} color="primary" />
          </Box>
        )}
      </Stack>
    </MainCard>
  );
}
