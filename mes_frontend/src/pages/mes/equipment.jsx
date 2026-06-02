import { useState } from 'react';

import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Grid from '@mui/material/Grid';
import Snackbar from '@mui/material/Snackbar';
import Stack from '@mui/material/Stack';
import Tab from '@mui/material/Tab';
import Tabs from '@mui/material/Tabs';
import TextField from '@mui/material/TextField';

import { PlusOutlined } from '@ant-design/icons';

import { mesEquipmentApi } from 'api/mes/equipment';
import MesDataPage from './components/MesDataPage';

const commonFilters = [
  { field: 'plantCd', label: '공장' },
  { field: 'equipmentCd', label: '설비 코드' },
  { field: 'fromDt', label: 'From', type: 'date' },
  { field: 'toDt', label: 'To', type: 'date' }
];

const emptyOper = {
  plantCd: '',
  equipmentCd: '',
  operDt: '',
  shift: 'A',
  operStatus: 'RUN',
  startDtm: '',
  endDtm: '',
  operTime: 0,
  woId: '',
  itemCd: '',
  prodQty: 0,
  workerId: 'SYSTEM',
  operRmk: '',
  regUserId: 'SYSTEM'
};

const emptyDown = {
  plantCd: '',
  equipmentCd: '',
  downtimeDt: '',
  downtimeType: 'STOP',
  downtimeCd: '',
  downtimeReason: '',
  startDtm: '',
  endDtm: '',
  downtimeMin: 0,
  woId: '',
  actionContent: '',
  actionUserId: 'SYSTEM',
  actionDtm: '',
  reporterId: 'SYSTEM',
  downtimeRmk: '',
  regUserId: 'SYSTEM'
};

const emptyMaint = {
  plantCd: '',
  equipmentCd: '',
  maintType: 'REPAIR',
  maintDt: '',
  startDtm: '',
  endDtm: '',
  maintTime: 0,
  maintWorkerId: 'SYSTEM',
  maintTeam: '',
  maintContent: '',
  symptom: '',
  cause: '',
  action: '',
  partReplaced: '',
  partCost: 0,
  laborCost: 0,
  otherCost: 0,
  maintResult: 'COMPLETED',
  nextMaintDt: '',
  maintRmk: '',
  regUserId: 'SYSTEM'
};

function today() {
  return new Date().toISOString().slice(0, 10);
}

function nowLocal(offsetHours = 0) {
  const date = new Date();
  date.setHours(date.getHours() + offsetHours);
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
}

function numberFields(payload, fields) {
  const next = { ...payload };
  fields.forEach((field) => {
    if (next[field] === '') {
      next[field] = null;
    } else if (next[field] !== null && next[field] !== undefined) {
      next[field] = Number(next[field]);
    }
  });
  return next;
}

export default function MesEquipment() {
  const [tab, setTab] = useState('oper');
  const [dialog, setDialog] = useState(null);
  const [form, setForm] = useState(emptyOper);
  const [message, setMessage] = useState(null);
  const [activeMutate, setActiveMutate] = useState(null);

  const openDialog = (type, mutate) => {
    const base = {
      oper: { ...emptyOper, operDt: today(), startDtm: nowLocal(-1), endDtm: nowLocal() },
      downtime: { ...emptyDown, downtimeDt: today(), startDtm: nowLocal(-1), endDtm: nowLocal(), actionDtm: nowLocal() },
      maint: { ...emptyMaint, maintDt: today(), startDtm: nowLocal(-1), endDtm: nowLocal() }
    }[type];

    setDialog(type);
    setForm(base);
    setActiveMutate(() => mutate);
  };

  const handleValue = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const handleSave = async () => {
    try {
      if (dialog === 'oper') {
        await mesEquipmentApi.createOperStatus(numberFields(form, ['operTime', 'woId', 'prodQty']));
      }
      if (dialog === 'downtime') {
        await mesEquipmentApi.createDowntime(numberFields(form, ['downtimeMin', 'woId']));
      }
      if (dialog === 'maint') {
        await mesEquipmentApi.createMaintHistory(numberFields(form, ['maintTime', 'partCost', 'laborCost', 'otherCost']));
      }
      setMessage({ severity: 'success', text: '설비 기록이 등록되었습니다.' });
      setDialog(null);
      await activeMutate?.();
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    }
  };

  const actionButton = (type, label) => ({ mutate }) => (
    <Button variant="contained" startIcon={<PlusOutlined />} onClick={() => openDialog(type, mutate)}>
      {label}
    </Button>
  );

  return (
    <>
      <Tabs value={tab} onChange={(_, value) => setTab(value)} sx={{ mb: 2 }}>
        <Tab label="가동 현황" value="oper" />
        <Tab label="비가동" value="downtime" />
        <Tab label="보전 이력" value="maint" />
      </Tabs>
      {tab === 'oper' && (
        <MesDataPage
          title="MES 설비 가동 현황"
          description="설비별 가동 상태, 작업 오더, 생산량과 작업 시간을 조회하고 기록합니다."
          cardTitle="설비 가동 목록"
          swrKey="mes-equipment-oper"
          fetcher={mesEquipmentApi.operStatuses}
          initialSearch={{ plantCd: '', equipmentCd: '', fromDt: '', toDt: '' }}
          filters={commonFilters}
          renderActions={actionButton('oper', '가동 기록')}
          columns={[
            { header: '일자', field: 'operDt' },
            { header: '설비', field: 'equipmentCd' },
            { header: '조', field: 'shift' },
            { header: '상태', render: (row) => <Chip label={row.operStatus} size="small" color={row.operStatus === 'RUN' ? 'success' : 'warning'} variant="light" /> },
            { header: '작업오더', field: 'woId' },
            { header: '품목', field: 'itemCd' },
            { header: '생산량', field: 'prodQty', align: 'right' },
            { header: '가동시간', field: 'operTime', align: 'right' },
            { header: '작업자', field: 'workerId' }
          ]}
          getRowId={(row) => row.operId}
        />
      )}
      {tab === 'downtime' && (
        <MesDataPage
          title="MES 설비 비가동"
          description="설비 정지 유형, 원인, 조치 내용을 조회하고 기록합니다."
          cardTitle="비가동 목록"
          swrKey="mes-equipment-downtime"
          fetcher={mesEquipmentApi.downtimes}
          initialSearch={{ plantCd: '', equipmentCd: '', fromDt: '', toDt: '' }}
          filters={commonFilters}
          renderActions={actionButton('downtime', '비가동 기록')}
          columns={[
            { header: '일자', field: 'downtimeDt' },
            { header: '설비', field: 'equipmentCd' },
            { header: '유형', field: 'downtimeType' },
            { header: '코드', field: 'downtimeCd' },
            { header: '원인', field: 'downtimeReason' },
            { header: '분', field: 'downtimeMin', align: 'right' },
            { header: '작업오더', field: 'woId' },
            { header: '조치', field: 'actionContent' }
          ]}
          getRowId={(row) => row.downtimeId}
        />
      )}
      {tab === 'maint' && (
        <MesDataPage
          title="MES 설비 보전 이력"
          description="설비 점검과 수리 이력, 보전 시간과 비용을 조회하고 기록합니다."
          cardTitle="보전 이력 목록"
          swrKey="mes-equipment-maint"
          fetcher={mesEquipmentApi.maintHistories}
          initialSearch={{ plantCd: '', equipmentCd: '', fromDt: '', toDt: '' }}
          filters={commonFilters}
          renderActions={actionButton('maint', '보전 기록')}
          columns={[
            { header: '보전번호', field: 'maintNo' },
            { header: '일자', field: 'maintDt' },
            { header: '설비', field: 'equipmentCd' },
            { header: '유형', field: 'maintType' },
            { header: '시간', field: 'maintTime', align: 'right' },
            { header: '담당자', field: 'maintWorkerId' },
            { header: '결과', field: 'maintResult' },
            { header: '비용', field: 'maintCost', align: 'right' }
          ]}
          getRowId={(row) => row.maintId}
        />
      )}

      <Dialog open={!!dialog} onClose={() => setDialog(null)} fullWidth maxWidth="md">
        <DialogTitle>{dialog === 'oper' ? '가동 기록' : dialog === 'downtime' ? '비가동 기록' : '보전 기록'}</DialogTitle>
        <DialogContent dividers>
          <Grid container spacing={2} sx={{ pt: 0.5 }}>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField fullWidth required size="small" label="공장" value={form.plantCd || ''} onChange={(event) => handleValue('plantCd', event.target.value)} />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField fullWidth required size="small" label="설비 코드" value={form.equipmentCd || ''} onChange={(event) => handleValue('equipmentCd', event.target.value)} />
            </Grid>

            {dialog === 'oper' && (
              <>
                <Grid size={{ xs: 12, md: 4 }}>
                  <TextField fullWidth size="small" type="date" label="가동일" value={form.operDt} onChange={(event) => handleValue('operDt', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
                </Grid>
                <Grid size={{ xs: 12, md: 4 }}>
                  <TextField fullWidth size="small" label="조" value={form.shift} onChange={(event) => handleValue('shift', event.target.value)} />
                </Grid>
                <Grid size={{ xs: 12, md: 4 }}>
                  <TextField fullWidth size="small" label="상태" value={form.operStatus} onChange={(event) => handleValue('operStatus', event.target.value)} />
                </Grid>
                <Grid size={{ xs: 12, md: 6 }}>
                  <TextField fullWidth size="small" type="datetime-local" label="시작" value={form.startDtm} onChange={(event) => handleValue('startDtm', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
                </Grid>
                <Grid size={{ xs: 12, md: 6 }}>
                  <TextField fullWidth size="small" type="datetime-local" label="종료" value={form.endDtm} onChange={(event) => handleValue('endDtm', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
                </Grid>
                <Grid size={{ xs: 12, md: 4 }}>
                  <TextField fullWidth size="small" type="number" label="가동시간" value={form.operTime} onChange={(event) => handleValue('operTime', event.target.value)} />
                </Grid>
                <Grid size={{ xs: 12, md: 4 }}>
                  <TextField fullWidth size="small" label="품목" value={form.itemCd} onChange={(event) => handleValue('itemCd', event.target.value)} />
                </Grid>
                <Grid size={{ xs: 12, md: 4 }}>
                  <TextField fullWidth size="small" type="number" label="생산량" value={form.prodQty} onChange={(event) => handleValue('prodQty', event.target.value)} />
                </Grid>
              </>
            )}

            {dialog === 'downtime' && (
              <>
                <Grid size={{ xs: 12, md: 4 }}>
                  <TextField fullWidth size="small" type="date" label="비가동일" value={form.downtimeDt} onChange={(event) => handleValue('downtimeDt', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
                </Grid>
                <Grid size={{ xs: 12, md: 4 }}>
                  <TextField fullWidth size="small" label="유형" value={form.downtimeType} onChange={(event) => handleValue('downtimeType', event.target.value)} />
                </Grid>
                <Grid size={{ xs: 12, md: 4 }}>
                  <TextField fullWidth size="small" label="코드" value={form.downtimeCd} onChange={(event) => handleValue('downtimeCd', event.target.value)} />
                </Grid>
                <Grid size={{ xs: 12, md: 6 }}>
                  <TextField fullWidth size="small" type="datetime-local" label="시작" value={form.startDtm} onChange={(event) => handleValue('startDtm', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
                </Grid>
                <Grid size={{ xs: 12, md: 6 }}>
                  <TextField fullWidth size="small" type="datetime-local" label="종료" value={form.endDtm} onChange={(event) => handleValue('endDtm', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
                </Grid>
                <Grid size={{ xs: 12, md: 4 }}>
                  <TextField fullWidth size="small" type="number" label="비가동 분" value={form.downtimeMin} onChange={(event) => handleValue('downtimeMin', event.target.value)} />
                </Grid>
                <Grid size={{ xs: 12, md: 8 }}>
                  <TextField fullWidth size="small" label="원인" value={form.downtimeReason} onChange={(event) => handleValue('downtimeReason', event.target.value)} />
                </Grid>
                <Grid size={12}>
                  <TextField fullWidth multiline minRows={3} label="조치 내용" value={form.actionContent} onChange={(event) => handleValue('actionContent', event.target.value)} />
                </Grid>
              </>
            )}

            {dialog === 'maint' && (
              <>
                <Grid size={{ xs: 12, md: 4 }}>
                  <TextField fullWidth size="small" type="date" label="보전일" value={form.maintDt} onChange={(event) => handleValue('maintDt', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
                </Grid>
                <Grid size={{ xs: 12, md: 4 }}>
                  <TextField fullWidth size="small" label="유형" value={form.maintType} onChange={(event) => handleValue('maintType', event.target.value)} />
                </Grid>
                <Grid size={{ xs: 12, md: 4 }}>
                  <TextField fullWidth size="small" label="담당자" value={form.maintWorkerId} onChange={(event) => handleValue('maintWorkerId', event.target.value)} />
                </Grid>
                <Grid size={{ xs: 12, md: 6 }}>
                  <TextField fullWidth size="small" type="datetime-local" label="시작" value={form.startDtm} onChange={(event) => handleValue('startDtm', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
                </Grid>
                <Grid size={{ xs: 12, md: 6 }}>
                  <TextField fullWidth size="small" type="datetime-local" label="종료" value={form.endDtm} onChange={(event) => handleValue('endDtm', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
                </Grid>
                <Grid size={{ xs: 12, md: 4 }}>
                  <TextField fullWidth size="small" type="number" label="보전 시간" value={form.maintTime} onChange={(event) => handleValue('maintTime', event.target.value)} />
                </Grid>
                <Grid size={{ xs: 12, md: 4 }}>
                  <TextField fullWidth size="small" type="number" label="부품비" value={form.partCost} onChange={(event) => handleValue('partCost', event.target.value)} />
                </Grid>
                <Grid size={{ xs: 12, md: 4 }}>
                  <TextField fullWidth size="small" type="number" label="인건비" value={form.laborCost} onChange={(event) => handleValue('laborCost', event.target.value)} />
                </Grid>
                <Grid size={12}>
                  <TextField fullWidth multiline minRows={3} label="보전 내용" value={form.maintContent} onChange={(event) => handleValue('maintContent', event.target.value)} />
                </Grid>
                <Grid size={12}>
                  <TextField fullWidth multiline minRows={3} label="조치" value={form.action} onChange={(event) => handleValue('action', event.target.value)} />
                </Grid>
              </>
            )}
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialog(null)}>취소</Button>
          <Button variant="contained" onClick={handleSave}>저장</Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={!!message} autoHideDuration={3500} onClose={() => setMessage(null)} anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}>
        {message && <Alert severity={message.severity} variant="filled" onClose={() => setMessage(null)}>{message.text}</Alert>}
      </Snackbar>
    </>
  );
}
