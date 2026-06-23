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
import TablePager from 'components/TablePager';
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

const eventTypeLabels = {
  TRANSFER_STARTED: '이동 시작',
  TRANSFER_COMPLETED: '이동 완료',
  EQUIPMENT_RUNNING: '설비 가동',
  EQUIPMENT_ERROR: '설비 오류 발생',
  ARRIVED_WRONG_LOCATION: '다른 위치에서 감지',
  INTERLOCK_BLOCKED: '목적지 점유로 이동 차단'
};

const eventStatusOptions = [
  { value: 'NORMAL', label: '정상' },
  { value: 'WARNING', label: '주의' },
  { value: 'ERROR', label: '확인 필요' },
  { value: 'INTERLOCK', label: '이동 차단' }
];

const errorCodeLabels = {
  DESTINATION_BLOCKED: '목적지 점유',
  MOTOR_OVERLOAD: '모터 과부하',
  SENSOR_LOCATION_MISMATCH: '위치 불일치'
};

const fieldLabels = {
  equipmentCd: '설비',
  eventType: '이벤트 종류',
  targetId: '이동오더',
  locationCd: '현재 위치',
  toLocationCd: '목적지',
  lotNo: 'LOT 번호',
  errorCode: '오류 코드',
  message: '오류 내용'
};

function getApiData(response, fallback) {
  return response?.data ?? fallback;
}

function getEventColor(status, processResult) {
  if (processResult === 'VALIDATION_FAILED') return 'warning';
  if (processResult === 'FAILED') return 'error';
  if (status === 'ERROR') return 'error';
  if (status === 'INTERLOCK') return 'warning';
  if (status === 'WARNING') return 'warning';
  if (status === 'NORMAL') return 'success';
  return 'info';
}

function getStatusLabel(event) {
  if (event.processResult === 'VALIDATION_FAILED') return '정보 누락';
  if (event.processResult === 'FAILED') return '처리 실패';
  if (event.eventStatus === 'ERROR') return '확인 필요';
  if (event.eventStatus === 'INTERLOCK') return '이동 차단';
  if (event.eventStatus === 'WARNING') return '주의';
  return '정상';
}

function getSituationLabel(event) {
  if (event.processResult === 'VALIDATION_FAILED') {
    return `${eventTypeLabels[event.eventType] || event.eventType || 'PLC 이벤트'} 정보 누락`;
  }
  if (event.errorCode && errorCodeLabels[event.errorCode]) {
    if (event.eventType === 'INTERLOCK_BLOCKED') return '목적지 점유로 이동 차단';
    if (event.eventType === 'ARRIVED_WRONG_LOCATION') return '다른 위치에서 감지';
    return errorCodeLabels[event.errorCode];
  }
  return eventTypeLabels[event.eventType] || event.eventType || '-';
}

function extractMissingFields(message) {
  const match = String(message || '').match(/missingFields=([^,.\s]+(?:,[^,.\s]+)*)/);
  if (!match) return [];
  return match[1]
    .split(',')
    .map((field) => field.trim())
    .filter(Boolean);
}

function getDetailMessage(event, missingFields) {
  if (missingFields.length > 0) {
    return `필수 정보가 부족합니다: ${missingFields.map((field) => fieldLabels[field] || field).join(', ')}`;
  }
  if (event.eventMessage) return event.eventMessage;
  if (event.processResult === 'FAILED' && event.processMessage && event.processMessage !== 'processed') {
    return event.processMessage;
  }
  if (event.errorCode && errorCodeLabels[event.errorCode]) {
    return `${errorCodeLabels[event.errorCode]} 상태를 확인하세요.`;
  }
  return '-';
}

function getRelatedWorkText(event) {
  if (event.targetType !== 'TRANSFER' || !event.targetId) return '';
  return `이동오더 ${event.targetId}`;
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
  const {
    data: eventResponse,
    error: eventError,
    isLoading
  } = useSWR(['mcs-plc-events', eventParams], () => plcEventApi.list(eventParams));

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
          설비에서 발생한 이동 시작, 이동 완료, 차단, 오류 상황을 확인합니다.
        </Typography>
      </Box>

      <MainCard title="검색 조건">
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <TextField
              fullWidth
              size="small"
              label="이벤트 ID"
              value={search.eventId}
              onChange={(event) => handleSearchValue('eventId', event.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <TextField
              fullWidth
              size="small"
              label="설비"
              value={search.equipmentCd}
              onChange={(event) => handleSearchValue('equipmentCd', event.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <FormControl fullWidth size="small">
              <InputLabel>상황</InputLabel>
              <Select label="상황" value={search.eventType} onChange={(event) => handleSearchValue('eventType', event.target.value)}>
                <MenuItem value="">전체</MenuItem>
                {eventTypes.map((type) => (
                  <MenuItem key={type} value={type}>
                    {eventTypeLabels[type] || type}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <FormControl fullWidth size="small">
              <InputLabel>상태</InputLabel>
              <Select label="상태" value={search.eventStatus} onChange={(event) => handleSearchValue('eventStatus', event.target.value)}>
                <MenuItem value="">전체</MenuItem>
                {eventStatusOptions.map((option) => (
                  <MenuItem key={option.value} value={option.value}>
                    {option.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid size={{ xs: 12, md: 2.4 }}>
            <TextField
              fullWidth
              size="small"
              label="이동오더"
              value={search.targetId}
              onChange={(event) => handleSearchValue('targetId', event.target.value)}
            />
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
                <TableCell>일시</TableCell>
                <TableCell>설비</TableCell>
                <TableCell>상황</TableCell>
                <TableCell>상태</TableCell>
                <TableCell>위치</TableCell>
                <TableCell>상세 내용</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {!isLoading && page.content.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} align="center">
                    조회된 PLC 이벤트가 없습니다.
                  </TableCell>
                </TableRow>
              )}
              {page.content.map((event) => {
                const missingFields = extractMissingFields(event.processMessage);
                const isValidationFailed = event.processResult === 'VALIDATION_FAILED';
                const relatedWork = getRelatedWorkText(event);

                return (
                  <TableRow key={event.eventId} hover sx={isValidationFailed ? { bgcolor: 'warning.lighter' } : undefined}>
                    <TableCell>{event.eventDtm}</TableCell>
                    <TableCell>{event.equipmentCd || '-'}</TableCell>
                    <TableCell>
                      <Stack spacing={0.25}>
                        <Typography variant="subtitle2">{getSituationLabel(event)}</Typography>
                        {event.errorCode && (
                          <Typography variant="caption" color="text.secondary">
                            {errorCodeLabels[event.errorCode] || event.errorCode}
                          </Typography>
                        )}
                      </Stack>
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={getStatusLabel(event)}
                        size="small"
                        color={getEventColor(event.eventStatus, event.processResult)}
                        variant="light"
                      />
                    </TableCell>
                    <TableCell>{event.locationCd || '-'}</TableCell>
                    <TableCell>
                      {missingFields.length > 0 && (
                        <Stack direction="row" spacing={0.5} sx={{ mb: 0.75, flexWrap: 'wrap', rowGap: 0.5 }}>
                          {missingFields.map((field) => (
                            <Chip key={field} label={fieldLabels[field] || field} size="small" color="warning" variant="outlined" />
                          ))}
                        </Stack>
                      )}
                      <Typography variant="body2" sx={{ overflowWrap: 'anywhere' }}>
                        {getDetailMessage(event, missingFields)}
                      </Typography>
                      {relatedWork && (
                        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
                          관련 작업: {relatedWork}
                        </Typography>
                      )}
                    </TableCell>
                  </TableRow>
                );
              })}
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
