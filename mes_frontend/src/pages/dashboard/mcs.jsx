import { Link as RouterLink } from 'react-router-dom';

import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Grid from '@mui/material/Grid';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

import { AlertOutlined, DatabaseOutlined, SwapOutlined, UnorderedListOutlined } from '@ant-design/icons';

import MainCard from 'components/MainCard';

const kpis = [
  { label: '입고 대기', value: '-', caption: 'MCS 입고 예정/대기 건수' },
  { label: '출고 대기', value: '-', caption: 'MCS 출고 지시/대기 건수' },
  { label: '이동 진행', value: '-', caption: '진행 중인 로케이션 이동 오더' },
  { label: 'PLC 이벤트', value: '-', caption: '최근 설비 이벤트 수집 현황' }
];

const workAreas = [
  { title: '입고 관리', caption: '입고 오더와 품목 입고 상태를 관리합니다.', url: '/mcs/inbounds', icon: <UnorderedListOutlined /> },
  { title: '출고 관리', caption: '출고 지시와 출고 완료 흐름을 관리합니다.', url: '/mcs/outbounds', icon: <UnorderedListOutlined /> },
  { title: '이동 관리', caption: '로케이션 간 자재 이동과 완료 반영을 관리합니다.', url: '/mcs/transfers', icon: <SwapOutlined /> },
  { title: '로케이션 재고', caption: 'Location 단위 현재 재고와 가용 재고를 확인합니다.', url: '/mcs/location-stock', icon: <DatabaseOutlined /> },
  { title: '재고 이력', caption: '입고, 출고, 이동, 조정 이력을 추적합니다.', url: '/mcs/inventory-transactions', icon: <DatabaseOutlined /> },
  { title: 'PLC 이벤트', caption: '설비 시뮬레이터와 PLC 이벤트 수집 결과를 확인합니다.', url: '/mcs/plc-events', icon: <AlertOutlined /> }
];

export default function McsDashboard() {
  return (
    <Stack spacing={3}>
      <Stack spacing={0.75}>
        <Typography variant="h2">MCS 대시보드</Typography>
        <Typography variant="body1" color="text.secondary">
          물류 이동, 입출고, 로케이션 재고, PLC 이벤트를 MCS 관점에서 분리해 확인합니다.
        </Typography>
      </Stack>

      <Grid container spacing={2.5}>
        {kpis.map((item) => (
          <Grid key={item.label} size={{ xs: 12, sm: 6, lg: 3 }}>
            <MainCard>
              <Stack spacing={0.75}>
                <Typography variant="h3">{item.value}</Typography>
                <Typography variant="subtitle1">{item.label}</Typography>
                <Typography variant="body2" color="text.secondary">
                  {item.caption}
                </Typography>
              </Stack>
            </MainCard>
          </Grid>
        ))}
      </Grid>

      <MainCard
        title="MCS 업무 화면"
        secondary={<Chip label="8081 / MCS API" size="small" color="primary" variant="light" />}
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
    </Stack>
  );
}
