import { Link as RouterLink } from 'react-router-dom';

import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Grid from '@mui/material/Grid';
import LinearProgress from '@mui/material/LinearProgress';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

import { BarChartOutlined, BuildOutlined, DatabaseOutlined, ExperimentOutlined, ProfileOutlined } from '@ant-design/icons';

import MainCard from 'components/MainCard';
import {
  DetailDrawer,
  EquipmentPanel,
  QualityPriorityTable,
  QualityResultPanel,
  StockEquipmentPriorityTable,
  StockPanel,
  WorkOrderStatusPanel,
  useOperationsDashboard
} from 'sections/operations/operationsDashboard';

const workAreas = [
  { title: '작업 오더', caption: '생산 작업 지시와 진행 상태를 조회합니다.', url: '/mes/work-orders', icon: <ProfileOutlined /> },
  { title: '생산 계획', caption: '생산 계획과 예정 물량을 확인합니다.', url: '/mes/prod-plans', icon: <BarChartOutlined /> },
  { title: '생산 실적', caption: '생산 완료 실적과 작업 결과를 확인합니다.', url: '/mes/work-results', icon: <BarChartOutlined /> },
  { title: 'MES 재고', caption: 'MES 기준의 품목 재고를 확인합니다.', url: '/mes/inventory', icon: <DatabaseOutlined /> },
  { title: '품질 검사', caption: '검사 결과와 품질 판정 흐름을 확인합니다.', url: '/mes/quality', icon: <ExperimentOutlined /> },
  { title: '설비 현황', caption: '생산 설비 상태와 기준 정보를 확인합니다.', url: '/mes/equipment', icon: <BuildOutlined /> }
];

export default function MesDashboard() {
  const {
    dashboard,
    isLoading,
    errors,
    defects,
    stocks,
    equipmentStatuses,
    detail,
    setDetail,
    openDefects,
    openInspections,
    openStocks,
    openEquipment,
    openWorkOrders
  } = useOperationsDashboard();

  return (
    <Stack spacing={3}>
      <Stack spacing={0.75}>
        <Typography variant="h2">MES 대시보드</Typography>
        <Typography variant="body1" color="text.secondary">
          생산 계획, 작업 오더, 실적, 품질, 설비 정보를 MES 관점에서 분리해 확인합니다.
        </Typography>
      </Stack>

      {isLoading && <LinearProgress />}
      {errors.map((error, index) => (
        <Alert key={index} severity="error">{error.message}</Alert>
      ))}

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, xl: 7 }}>
          <QualityResultPanel dashboard={dashboard} onOpenDefects={openDefects} onOpenInspections={openInspections} />
        </Grid>
        <Grid size={{ xs: 12, xl: 5 }}>
          <StockPanel dashboard={dashboard} onOpen={openStocks} />
        </Grid>
      </Grid>

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, lg: 6 }}>
          <WorkOrderStatusPanel dashboard={dashboard} onOpenWorkOrders={openWorkOrders} />
        </Grid>
        <Grid size={{ xs: 12, lg: 6 }}>
          <EquipmentPanel dashboard={dashboard} onOpen={openEquipment} />
        </Grid>
      </Grid>

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, lg: 6 }}>
          <QualityPriorityTable defects={defects} failedInspections={dashboard.failedInspections} />
        </Grid>
        <Grid size={{ xs: 12, lg: 6 }}>
          <StockEquipmentPriorityTable stocks={stocks} equipmentStatuses={equipmentStatuses} />
        </Grid>
      </Grid>

      <MainCard
        title="MES 업무 화면"
        secondary={<Chip label="8080 / MES API" size="small" color="primary" variant="light" />}
      >
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
