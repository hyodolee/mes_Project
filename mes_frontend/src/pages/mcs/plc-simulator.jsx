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
import Typography from '@mui/material/Typography';

import { ThunderboltOutlined, WarningOutlined } from '@ant-design/icons';

import MainCard from 'components/MainCard';
import { plcSimApi } from 'api/mes/plcSim';
import { transferApi } from 'api/mcs/transfers';

function getApiData(response, fallback) {
  return response?.data ?? fallback;
}

function scenarioColor(code) {
  if (code === 'NORMAL') return 'success';
  if (code === 'EQUIPMENT_ERROR' || code === 'INTERLOCK') return 'warning';
  return 'error';
}

export default function McsPlcSimulator() {
  const [transferId, setTransferId] = useState('');
  const [working, setWorking] = useState('');
  const [message, setMessage] = useState(null);

  const { data: transfersData, isLoading: transfersLoading } = useSWR(
    ['plc-sim-transfers'],
    () => transferApi.list({ page: 1, size: 50 }),
    { revalidateOnFocus: false }
  );

  const { data: scenariosData } = useSWR(['plc-sim-scenarios'], () => plcSimApi.scenarios(), {
    revalidateOnFocus: false
  });

  const transfers = useMemo(() => {
    const raw = getApiData(transfersData, []);
    return Array.isArray(raw) ? raw : raw?.content ?? [];
  }, [transfersData]);

  const scenarios = useMemo(() => getApiData(scenariosData, []), [scenariosData]);

  const handleFire = async (scenario) => {
    if (!transferId) {
      setMessage({ severity: 'warning', text: '먼저 이동오더를 선택해 주세요.' });
      return;
    }

    setWorking(scenario.code);
    setMessage(null);

    try {
      const res = await plcSimApi.fire(Number(transferId), scenario.code);
      const sent = res?.data?.eventsSent ?? 0;
      setMessage({
        severity: 'success',
        text: `[${scenario.label}] 이벤트 ${sent}건 발생 완료. 잠시 후 알림/PLC 이벤트 화면에 반영됩니다.`
      });
    } catch (fireError) {
      setMessage({ severity: 'error', text: fireError.message || '이벤트 발생에 실패했습니다.' });
    } finally {
      setWorking('');
    }
  };

  const handleSentryError = async () => {
    setWorking('SENTRY_ERROR');
    setMessage(null);

    try {
      await plcSimApi.sentryError(transferId);
      setMessage({
        severity: 'warning',
        text: 'Sentry 테스트 API가 에러 없이 종료되었습니다. 백엔드 설정을 확인해 주세요.'
      });
    } catch (sentryError) {
      setMessage({
        severity: 'error',
        text: `의도한 Sentry 테스트 에러가 발생했습니다. Sentry Issues에서 "PLC simulator Sentry error test"를 확인하세요. (${sentryError.message})`
      });
    } finally {
      setWorking('');
    }
  };

  return (
    <MainCard title="PLC 이벤트 시뮬레이터">
      <Stack spacing={3}>
        <Typography variant="body2" color="text.secondary">
          버튼으로 PLC 이벤트를 발생시킵니다. 이동오더를 고르고 시나리오 버튼을 누르세요. 데모/테스트 전용 화면입니다.
        </Typography>

        <FormControl sx={{ maxWidth: 420 }}>
          <InputLabel id="transfer-label">이동오더 선택</InputLabel>
          <Select
            labelId="transfer-label"
            label="이동오더 선택"
            value={transferId}
            onChange={(event) => setTransferId(event.target.value)}
            disabled={transfersLoading}
          >
            {transfers.map((t) => (
              <MenuItem key={t.transferId} value={t.transferId}>
                {t.transferNo} ({t.fromLocationCd} → {t.toLocationCd})
              </MenuItem>
            ))}
          </Select>
        </FormControl>

        {message && <Alert severity={message.severity}>{message.text}</Alert>}

        <Box>
          <Typography variant="subtitle1" sx={{ mb: 1.5 }}>
            시나리오
          </Typography>
          <Grid container spacing={2}>
            {scenarios.map((scenario) => (
              <Grid key={scenario.code} size={{ xs: 12, sm: 6, md: 4 }}>
                <Button
                  fullWidth
                  variant="outlined"
                  color={scenarioColor(scenario.code)}
                  startIcon={<ThunderboltOutlined />}
                  disabled={Boolean(working)}
                  onClick={() => handleFire(scenario)}
                  sx={{ flexDirection: 'column', alignItems: 'flex-start', py: 1.5, textAlign: 'left' }}
                >
                  <Typography variant="subtitle2">
                    {working === scenario.code ? '발생 중...' : scenario.label}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {scenario.desc}
                  </Typography>
                </Button>
              </Grid>
            ))}
          </Grid>
        </Box>

        <Box>
          <Typography variant="subtitle1" sx={{ mb: 1.5 }}>
            운영 모니터링 테스트
          </Typography>
          <Button
            variant="contained"
            color="error"
            startIcon={<WarningOutlined />}
            disabled={Boolean(working)}
            onClick={handleSentryError}
          >
            {working === 'SENTRY_ERROR' ? '에러 발생 중...' : 'Sentry 에러 발생'}
          </Button>
        </Box>
      </Stack>
    </MainCard>
  );
}
