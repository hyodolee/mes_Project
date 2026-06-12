import { useState } from 'react';

import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Grid from '@mui/material/Grid';
import Snackbar from '@mui/material/Snackbar';
import TextField from '@mui/material/TextField';

import { PlusOutlined } from '@ant-design/icons';

import { mesProductionApi } from 'api/mes/production';
import MesDataPage from './components/MesDataPage';

const emptyForm = {
  plantCd: '',
  resultId: '',
  procResultId: '',
  defectDt: '',
  defectType: 'PROCESS',
  defectCd: '',
  defectNm: '',
  defectQty: 1,
  defectCause: '',
  defectAction: '',
  disposition: 'HOLD',
  dispositionQty: 0,
  dispositionDt: '',
  itemCd: '',
  lotNo: '',
  workerId: 'SYSTEM',
  equipmentCd: '',
  processCd: '',
  defectRmk: ''
};

function today() {
  return new Date().toISOString().slice(0, 10);
}

function toNumberOrNull(value) {
  return value === '' || value === null || value === undefined ? null : Number(value);
}

export default function MesDefects() {
  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [message, setMessage] = useState(null);
  const [activeMutate, setActiveMutate] = useState(null);

  const openDialog = (mutate) => {
    setForm({ ...emptyForm, defectDt: today(), dispositionDt: today() });
    setDialogOpen(true);
    setActiveMutate(() => mutate);
  };

  const handleValue = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const handleSave = async () => {
    try {
      await mesProductionApi.createDefectHistory({
        ...form,
        resultId: Number(form.resultId),
        procResultId: toNumberOrNull(form.procResultId),
        defectQty: Number(form.defectQty || 0),
        dispositionQty: Number(form.dispositionQty || 0)
      });
      setMessage({ severity: 'success', text: '불량 이력이 등록되었습니다.' });
      setDialogOpen(false);
      await activeMutate?.();
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    }
  };

  return (
    <>
      <MesDataPage
        title="MES 불량 이력"
        description="생산실적과 연결된 불량 유형, 수량, 원인, 조치 내역을 조회하고 등록합니다."
        cardTitle="불량 이력 목록"
        swrKey="mes-defect-histories"
        fetcher={mesProductionApi.defectHistories}
        initialSearch={{ plantCd: '', itemCd: '', fromDt: '', toDt: '', defectId: '' }}
        filters={[
          { field: 'plantCd', label: '공장' },
          { field: 'itemCd', label: '품목 코드' },
          { field: 'fromDt', label: 'From', type: 'date' },
          { field: 'toDt', label: 'To', type: 'date' }
        ]}
        renderActions={({ mutate }) => (
          <Button variant="contained" startIcon={<PlusOutlined />} onClick={() => openDialog(mutate)}>
            불량 등록
          </Button>
        )}
        columns={[
          { header: '일자', field: 'defectDt' },
          { header: '공장', field: 'plantCd' },
          { header: '실적ID', field: 'resultId' },
          { header: '유형', field: 'defectType' },
          { header: '코드', field: 'defectCd' },
          { header: '불량명', field: 'defectNm' },
          { header: '수량', field: 'defectQty', align: 'right' },
          { header: '원인', field: 'defectCause' },
          { header: '조치', field: 'defectAction' },
          { header: '처분', field: 'disposition' }
        ]}
        getRowId={(row) => row.defectId}
      />

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth maxWidth="md">
        <DialogTitle>불량 이력 등록</DialogTitle>
        <DialogContent dividers>
          <Grid container spacing={2} sx={{ pt: 0.5 }}>
            {[
              ['plantCd', '공장'],
              ['resultId', '실적 ID'],
              ['defectType', '불량 유형'],
              ['defectCd', '불량 코드'],
              ['defectNm', '불량명'],
              ['itemCd', '품목 코드'],
              ['lotNo', 'LOT 번호'],
              ['workerId', '작업자'],
              ['equipmentCd', '설비'],
              ['processCd', '공정'],
              ['disposition', '처분']
            ].map(([field, label]) => (
              <Grid key={field} size={{ xs: 12, md: 4 }}>
                <TextField fullWidth size="small" label={label} value={form[field]} onChange={(event) => handleValue(field, event.target.value)} />
              </Grid>
            ))}
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField fullWidth size="small" type="date" label="불량일" value={form.defectDt} onChange={(event) => handleValue('defectDt', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField fullWidth size="small" type="number" label="불량 수량" value={form.defectQty} onChange={(event) => handleValue('defectQty', event.target.value)} />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField fullWidth size="small" type="number" label="처분 수량" value={form.dispositionQty} onChange={(event) => handleValue('dispositionQty', event.target.value)} />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField fullWidth size="small" type="date" label="처분일" value={form.dispositionDt} onChange={(event) => handleValue('dispositionDt', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField fullWidth multiline minRows={3} label="원인" value={form.defectCause} onChange={(event) => handleValue('defectCause', event.target.value)} />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField fullWidth multiline minRows={3} label="조치" value={form.defectAction} onChange={(event) => handleValue('defectAction', event.target.value)} />
            </Grid>
            <Grid size={12}>
              <TextField fullWidth multiline minRows={3} label="비고" value={form.defectRmk} onChange={(event) => handleValue('defectRmk', event.target.value)} />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>취소</Button>
          <Button variant="contained" onClick={handleSave}>저장</Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={!!message} autoHideDuration={3500} onClose={() => setMessage(null)} anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}>
        {message && <Alert severity={message.severity} variant="filled" onClose={() => setMessage(null)}>{message.text}</Alert>}
      </Snackbar>
    </>
  );
}
