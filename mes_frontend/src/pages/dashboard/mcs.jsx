import { Link as RouterLink } from 'react-router-dom';

import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Grid from '@mui/material/Grid';
import LinearProgress from '@mui/material/LinearProgress';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

import { AlertOutlined, DatabaseOutlined, SwapOutlined, UnorderedListOutlined } from '@ant-design/icons';

import MainCard from 'components/MainCard';
import {
  DetailDrawer,
  OperationFlowPanel,
  PlcSignalTrendPanel,
  SignalStatusPanel,
  TransferProductionTrendPanel,
  TransferStatusPanel,
  useOperationsDashboard
} from 'sections/operations/operationsDashboard';

const workAreas = [
  { title: '입고 관리', caption: '입고 오더와 품목 입고 상태를 관리합니다.', url: '/mcs/inbounds', icon: <UnorderedListOutlined /> },
  { title: '출고 관리', caption: '출고 지시와 출고 완료 흐름을 관리합니다.', url: '/mcs/outbounds', icon: <UnorderedListOutlined /> },
  { title: '이동 관리', caption: '로케이션 간 자재 이동과 완료 반영을 관리합니다.', url: '/mcs/transfers', icon: <SwapOutlined /> },
  { title: '로케이션 재고', caption: 'Location 단위 현재 재고와 가용 재고를 확인합니다.', url: '/mcs/location-stock', icon: <DatabaseOutlined /> },
  { title: '재고 이력', caption: '입고, 출고, 이동, 조정 이력을 추적합니다.', url: '/mcs/inventory-transactions', icon: <DatabaseOutlined /> },
  { title: 'PLC 이벤트', caption: '설비 시뮬레이터와 PLC 이벤트 수집 결과를 확인합니다.', url: '/mcs/plc-events', icon: <AlertOutlined /> }
];

export default function McsDashboard() {
  const { dashboard, isLoading, errors, detail, setDetail, openTransfers, openSignals, openWorkOrders } = useOperationsDashboard();
  const hasTrend = dashboard.trend.labels.length > 0;

  return (
    <Stack spacing={3}>
      <Stack spacing={0.75}>
        <Typography variant="h2">MCS 대시보드</Typography>
        <Typography variant="body1" color="text.secondary">
          물류 이동, 입출고, 로케이션 재고, PLC 이벤트를 MCS 관점에서 확인합니다.
        </Typography>
      </Stack>

      {isLoading && <LinearProgress />}
      {errors.map((error, index) => (
        <Alert key={index} severity="error">
          {error.message}
        </Alert>
      ))}

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, lg: 6 }}>
          <TransferStatusPanel dashboard={dashboard} onOpenTransfers={openTransfers} />
        </Grid>
        <Grid size={{ xs: 12, lg: 6 }}>
          <SignalStatusPanel dashboard={dashboard} onOpenSignals={openSignals} />
        </Grid>
      </Grid>

      {hasTrend && (
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, lg: 6 }}>
            <PlcSignalTrendPanel dashboard={dashboard} onOpenSignals={openSignals} />
          </Grid>
          <Grid size={{ xs: 12, lg: 6 }}>
            <TransferProductionTrendPanel dashboard={dashboard} onOpenTransfers={openTransfers} />
          </Grid>
        </Grid>
      )}

      <OperationFlowPanel
        dashboard={dashboard}
        onOpenWorkOrders={openWorkOrders}
        onOpenTransfers={openTransfers}
        onOpenSignals={openSignals}
      />

      <MainCard title="MCS 업무 화면" secondary={<Chip label="MES 8080 통합 API" size="small" color="primary" variant="light" />}>
        <Grid container spacing={2}>
          {workAreas.map((area) => (
            <Grid key={area.title} size={{ xs: 12, md: 6, xl: 4 }}>
              <Stack spacing={1.5} sx={{ height: '100%' }}>
                <Stack direction="row" spacing={1} alignItems="center">
                  {area.icon}
                  <Typography variant="subtitle1">{area.title}</Typography>
                </Stack>
                <Typography variant="body2" color="text.secondary" sx={{ flexGrow: 1 }}>
                  {area.caption}
                </Typography>
                <Button component={RouterLink} to={area.url} variant="outlined" size="small">
                  화면 열기
                </Button>
              </Stack>
            </Grid>
          ))}
        </Grid>
      </MainCard>

      <DetailDrawer detail={detail} onClose={() => setDetail(null)} />
    </Stack>
  );
}
