import { useMemo, useState } from 'react';
import useSWR from 'swr';

import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Stack from '@mui/material/Stack';
import Tab from '@mui/material/Tab';
import Tabs from '@mui/material/Tabs';
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
import { mesMasterApi } from 'api/mes/master';

function getApiData(response, fallback) {
  return response?.data ?? fallback;
}

export default function MesMasterData() {
  const [tab, setTab] = useState('plants');
  const [plantSearch, setPlantSearch] = useState({ plantNm: '', useYn: 'Y' });
  const [itemSearch, setItemSearch] = useState({ itemNm: '', useYn: 'Y' });
  const [plantQuery, setPlantQuery] = useState(plantSearch);
  const [itemQuery, setItemQuery] = useState(itemSearch);

  const { data: plantResponse, error: plantError, isLoading: plantLoading } = useSWR(['mes-plants', plantQuery], () => mesMasterApi.plants(plantQuery));
  const { data: itemResponse, error: itemError, isLoading: itemLoading } = useSWR(['mes-items', itemQuery], () => mesMasterApi.items(itemQuery));

  const plants = getApiData(plantResponse, []);
  const items = getApiData(itemResponse, []);

  const visibleItems = useMemo(() => items.slice(0, 100), [items]);

  const resetPlants = () => {
    const next = { plantNm: '', useYn: 'Y' };
    setPlantSearch(next);
    setPlantQuery(next);
  };

  const resetItems = () => {
    const next = { itemNm: '', useYn: 'Y' };
    setItemSearch(next);
    setItemQuery(next);
  };

  return (
    <Stack spacing={3}>
      <Box>
        <Typography variant="h3">MES 기준 정보</Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mt: 0.75 }}>
          MCS가 참조하는 MES 공장과 품목 마스터를 확인합니다.
        </Typography>
      </Box>

      <MainCard>
        <Tabs value={tab} onChange={(_, value) => setTab(value)} sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
          <Tab label="공장" value="plants" />
          <Tab label="품목" value="items" />
        </Tabs>

        {tab === 'plants' && (
          <Stack spacing={2}>
            <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} sx={{ justifyContent: 'space-between' }}>
              <TextField
                size="small"
                label="공장명"
                value={plantSearch.plantNm}
                onChange={(event) => setPlantSearch((current) => ({ ...current, plantNm: event.target.value }))}
                sx={{ minWidth: 260 }}
              />
              <Stack direction="row" spacing={1} sx={{ justifyContent: 'flex-end' }}>
                <Button variant="outlined" startIcon={<ReloadOutlined />} onClick={resetPlants}>
                  초기화
                </Button>
                <Button variant="contained" startIcon={<SearchOutlined />} onClick={() => setPlantQuery(plantSearch)}>
                  조회
                </Button>
              </Stack>
            </Stack>
            {plantError && <Alert severity="error">{plantError.message}</Alert>}
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>공장 코드</TableCell>
                    <TableCell>공장명</TableCell>
                    <TableCell>회사</TableCell>
                    <TableCell>주소</TableCell>
                    <TableCell>전화</TableCell>
                    <TableCell>사용</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {!plantLoading && plants.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={6} align="center">
                        조회된 공장이 없습니다.
                      </TableCell>
                    </TableRow>
                  )}
                  {plants.map((plant) => (
                    <TableRow key={plant.plantCd} hover>
                      <TableCell>{plant.plantCd}</TableCell>
                      <TableCell>{plant.plantNm}</TableCell>
                      <TableCell>{plant.companyNm || plant.companyCd}</TableCell>
                      <TableCell>{plant.addr || '-'}</TableCell>
                      <TableCell>{plant.telNo || '-'}</TableCell>
                      <TableCell>
                        <Chip label={plant.useYn === 'Y' ? '사용' : '미사용'} size="small" color={plant.useYn === 'Y' ? 'success' : 'default'} variant="light" />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Stack>
        )}

        {tab === 'items' && (
          <Stack spacing={2}>
            <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} sx={{ justifyContent: 'space-between' }}>
              <TextField
                size="small"
                label="품목명"
                value={itemSearch.itemNm}
                onChange={(event) => setItemSearch((current) => ({ ...current, itemNm: event.target.value }))}
                sx={{ minWidth: 260 }}
              />
              <Stack direction="row" spacing={1} sx={{ justifyContent: 'flex-end' }}>
                <Button variant="outlined" startIcon={<ReloadOutlined />} onClick={resetItems}>
                  초기화
                </Button>
                <Button variant="contained" startIcon={<SearchOutlined />} onClick={() => setItemQuery(itemSearch)}>
                  조회
                </Button>
              </Stack>
            </Stack>
            {itemError && <Alert severity="error">{itemError.message}</Alert>}
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>품목 코드</TableCell>
                    <TableCell>공장</TableCell>
                    <TableCell>품목명</TableCell>
                    <TableCell>규격</TableCell>
                    <TableCell>유형</TableCell>
                    <TableCell>그룹</TableCell>
                    <TableCell>단위</TableCell>
                    <TableCell>사용</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {!itemLoading && visibleItems.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={8} align="center">
                        조회된 품목이 없습니다.
                      </TableCell>
                    </TableRow>
                  )}
                  {visibleItems.map((item) => (
                    <TableRow key={`${item.plantCd}-${item.itemCd}`} hover>
                      <TableCell>{item.itemCd}</TableCell>
                      <TableCell>{item.plantCd}</TableCell>
                      <TableCell>{item.itemNm}</TableCell>
                      <TableCell>{item.itemSpec || '-'}</TableCell>
                      <TableCell>{item.itemType || '-'}</TableCell>
                      <TableCell>{item.itemGrp || '-'}</TableCell>
                      <TableCell>{item.unit || '-'}</TableCell>
                      <TableCell>
                        <Chip label={item.useYn === 'Y' ? '사용' : '미사용'} size="small" color={item.useYn === 'Y' ? 'success' : 'default'} variant="light" />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
            {items.length > visibleItems.length && (
              <Typography variant="caption" color="text.secondary">
                화면 성능을 위해 상위 100건만 표시합니다.
              </Typography>
            )}
          </Stack>
        )}
      </MainCard>
    </Stack>
  );
}
