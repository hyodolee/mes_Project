import { Link as RouterLink } from 'react-router-dom';

import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Grid from '@mui/material/Grid';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

import { AppstoreOutlined, RobotOutlined } from '@ant-design/icons';

import MainCard from 'components/MainCard';

const systems = [
  {
    title: 'MES 대시보드',
    caption: '생산 계획, 작업 오더, 생산 실적, 품질과 설비 흐름을 확인합니다.',
    status: 'MES',
    url: '/mes/dashboard'
  },
  {
    title: 'MCS 대시보드',
    caption: '입고, 출고, 로케이션 재고, 이동 오더, PLC 이벤트 흐름을 확인합니다.',
    status: 'MCS',
    url: '/mcs/dashboard'
  },
  {
    title: 'AI 운영 분석',
    caption: '운영 현황 요약, 이상 징후, 알림 후보, 원인 관계를 정리합니다.',
    status: 'AI',
    url: '/ai/operations'
  }
];

export default function DashboardDefault() {
  return (
    <Stack spacing={3}>
      <Stack spacing={0.75}>
        <Typography variant="h2">운영 포털</Typography>
        <Typography variant="body1" color="text.secondary">
          MES와 MCS 기능을 하나의 백엔드로 통합하고, 필요한 업무 화면으로 바로 이동할 수 있는 포털 화면입니다.
        </Typography>
      </Stack>

      <Grid container spacing={2.5}>
        {systems.map((system) => (
          <Grid key={system.title} size={{ xs: 12, md: 4 }}>
            <MainCard>
              <Stack spacing={2} sx={{ minHeight: 180 }}>
                <Stack direction="row" alignItems="center" justifyContent="space-between" spacing={1}>
                  <Typography variant="h4">{system.title}</Typography>
                  <Chip label={system.status} size="small" color={system.status === 'AI' ? 'secondary' : 'primary'} variant="light" />
                </Stack>
                <Typography variant="body2" color="text.secondary" sx={{ flexGrow: 1 }}>
                  {system.caption}
                </Typography>
                <Button component={RouterLink} to={system.url} variant="contained" startIcon={<AppstoreOutlined />}>
                  이동
                </Button>
              </Stack>
            </MainCard>
          </Grid>
        ))}
      </Grid>

      <MainCard title="구성 기준">
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 4 }}>
            <Typography variant="subtitle1">MES</Typography>
            <Typography variant="body2" color="text.secondary">
              생산 실행, 품질, 설비, 기준 정보를 담당합니다.
            </Typography>
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <Typography variant="subtitle1">MCS</Typography>
            <Typography variant="body2" color="text.secondary">
              물류 이동, 로케이션 재고, 입출고, PLC 이벤트를 담당합니다.
            </Typography>
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <Stack direction="row" spacing={1} alignItems="center">
              <RobotOutlined />
              <Typography variant="subtitle1">AI</Typography>
            </Stack>
            <Typography variant="body2" color="text.secondary">
              두 업무 영역의 운영 데이터를 읽어 분석과 알림을 제공합니다.
            </Typography>
          </Grid>
        </Grid>
      </MainCard>
    </Stack>
  );
}
