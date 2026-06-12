import { useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
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
import TablePagination from '@mui/material/TablePagination';
import TableRow from '@mui/material/TableRow';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';

import { ReloadOutlined, SearchOutlined } from '@ant-design/icons';

import MainCard from 'components/MainCard';
import { plcEventApi } from 'api/mcs/plcEvents';

const eventTypes = [
  'TRANSFER_STARTED',
  'TRANSFER_COMPLETED',
  'EQUIPMENT_RUNNING',
  'EQUIPMENT_ERROR',
  'ARRIVED_WRONG_LOCATION',
  'INTERLOCK_BLOCKED'
];

function getApiData(response, fallback) {
  return response?.data ?? fallback;
}

function getResultColor(result) {
  if (result === 'SUCCESS') return 'success';
  if (result === 'VALIDATION_FAILED') return 'warning';
  if (result === 'FAILED') return 'error';
  return 'default';
}

function getEventColor(status) {
  if (status === 'ERROR') return 'error';
  if (status === 'INTERLOCK') return 'warning';
  if (status === 'WARNING') return 'warning';
  return 'info';
}

function getProcessResultLabel(result) {
  if (result === 'SUCCESS') return '처리 완료';
  if (result === 'VALIDATION_FAILED') return '데이터 누락';
  if (result === 'FAILED') return '처리 실패';
  return '대기';
}

function extractMissingFields(message) {
  const match = String(message || '').match(/missingFields=([^,.\s]+(?:,[^,.\s]+)*)/);
  if (!match) return [];
  return match[1].split(',').map((field) => field.trim()).filter(Boolean);
}

export default function McsPlcEvents() {
  const [urlSearchParams] = useSearchParams();
  const [search, setSearch] = useState({
    eventId: urlSearchParams.get('eventId') || '',
    equipmentCd: urlSearchParams.get('equipmentCd') || '',
    eventType: urlSearchParams.get('eventType') || '',
    eventStatus: urlSearchParams.get('eventStatus') || '',
    processResult: urlSearchParams.get('processResult') || '',
    targetId: urlSearchParams.get('targetId') || '',
    fromDate: urlSearchParams.get('fromDate') || '',
    toDate: urlSearchParams.get('toDate') || ''
  });
  const [query, setQuery] = useState({ page: 1, size: 10 });

  const eventParams = useMemo(() => ({ ...search, targetType: 'TRANSFER', ...query }), [search, query]);
  const { data: eventResponse, error: eventError, isLoading } = useSWR(['mcs-plc-events', eventParams], () => plcEventApi.list(eventParams));

  const page = getApiData(eventResponse, { content: [], totalElements: 0, currentPage: 1, size: 10 });

  const handleSearchValue = (field, value) => {
    setSearch((current) => ({ ...current, [field]: value }));
  };

  const handleSearch = () => {
    setQuery((current) => ({ ...current, page: 1 }));
  };

  const handleReset = () => {
    setSearch({ eventId: '', equipmentCd: '', eventType: '', eventStatus: '', processResult: '', targetId: '', fromDate: '', toDate: '' });
    setQuery({ page: 1, size: 10 });
  };

  return (
    <Stack spacing={3}>
      <Box>
        <Typography variant="h3">PLC 이벤트</Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mt: 0.75 }}>
          PowerShell 시뮬레이터 또는 설비가 전송한 이벤트 처리 결과를 확인합니다.
        </Typography>
      </Box>

      <MainCard title="검색 조건">
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <TextField fullWidth size="small" label="이벤트 ID" value={search.eventId} onChange={(event) => handleSearchValue('eventId', event.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <TextField fullWidth size="small" label="설비 코드" value={search.equipmentCd} onChange={(event) => handleSearchValue('equipmentCd', event.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <FormControl fullWidth size="small">
              <InputLabel>이벤트</InputLabel>
              <Select label="이벤트" value={search.eventType} onChange={(event) => handleSearchValue('eventType', event.target.value)}>
                <MenuItem value="">전체</MenuItem>
                {eventTypes.map((type) => (
                  <MenuItem key={type} value={type}>
                    {type}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <FormControl fullWidth size="small">
              <InputLabel>이벤트 상태</InputLabel>
              <Select label="이벤트 상태" value={search.eventStatus} onChange={(event) => handleSearchValue('eventStatus', event.target.value)}>
                <MenuItem value="">전체</MenuItem>
                <MenuItem value="NORMAL">NORMAL</MenuItem>
                <MenuItem value="WARNING">WARNING</MenuItem>
                <MenuItem value="ERROR">ERROR</MenuItem>
                <MenuItem value="INTERLOCK">INTERLOCK</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <FormControl fullWidth size="small">
              <InputLabel>처리 결과</InputLabel>
              <Select label="처리 결과" value={search.processResult} onChange={(event) => handleSearchValue('processResult', event.target.value)}>
                <MenuItem value="">전체</MenuItem>
                <MenuItem value="SUCCESS">SUCCESS</MenuItem>
                <MenuItem value="VALIDATION_FAILED">VALIDATION_FAILED</MenuItem>
                <MenuItem value="FAILED">FAILED</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <TextField fullWidth size="small" label="Transfer ID" value={search.targetId} onChange={(event) => handleSearchValue('targetId', event.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
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
          <Grid size={{ xs: 12, md: 3 }}>
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

      <MainCard title="PLC 이벤트 로그" content={false}>
        {eventError && <Alert severity="error">{eventError.message}</Alert>}
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>이벤트 일시</TableCell>
                <TableCell>설비</TableCell>
                <TableCell>이벤트</TableCell>
                <TableCell>상태</TableCell>
                <TableCell>대상</TableCell>
                <TableCell>Location</TableCell>
                <TableCell>처리 결과</TableCell>
                <TableCell>누락/오류 내용</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {!isLoading && page.content.length === 0 && (
                <TableRow>
                  <TableCell colSpan={8} align="center">
                    조회된 PLC 이벤트가 없습니다.
                  </TableCell>
                </TableRow>
              )}
              {page.content.map((event) => {
                const missingFields = extractMissingFields(event.processMessage);
                const isValidationFailed = event.processResult === 'VALIDATION_FAILED';

                return (
                  <TableRow key={event.eventId} hover sx={isValidationFailed ? { bgcolor: 'warning.lighter' } : undefined}>
                    <TableCell>{event.eventDtm}</TableCell>
                    <TableCell>{event.equipmentCd || '-'}</TableCell>
                    <TableCell>
                      <Typography variant="subtitle2">{event.eventType}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        {event.errorCode || '-'}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Chip label={event.eventStatus} size="small" color={getEventColor(event.eventStatus)} variant="light" />
                    </TableCell>
                    <TableCell>
                      {event.targetType} #{event.targetId || '-'}
                    </TableCell>
                    <TableCell>{event.locationCd || '-'}</TableCell>
                    <TableCell>
                      <Chip
                        label={getProcessResultLabel(event.processResult)}
                        size="small"
                        color={getResultColor(event.processResult)}
                        variant="light"
                      />
                    </TableCell>
                    <TableCell>
                      {missingFields.length > 0 && (
                        <Stack direction="row" spacing={0.5} sx={{ mb: 0.75, flexWrap: 'wrap', rowGap: 0.5 }}>
                          {missingFields.map((field) => (
                            <Chip key={field} label={field} size="small" color="warning" variant="outlined" />
                          ))}
                        </Stack>
                      )}
                      <Typography variant="body2" sx={{ overflowWrap: 'anywhere' }}>{event.eventMessage || '-'}</Typography>
                      {event.processMessage && (
                        <Typography
                          variant="caption"
                          color={isValidationFailed ? 'warning.dark' : 'text.secondary'}
                          sx={{ display: 'block', mt: 0.5, overflowWrap: 'anywhere' }}
                        >
                          {event.processMessage}
                        </Typography>
                      )}
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
    </Stack>
  );
}
