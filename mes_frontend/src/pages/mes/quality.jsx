import { useState } from 'react';
import useSWR from 'swr';

import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Grid from '@mui/material/Grid';
import Snackbar from '@mui/material/Snackbar';
import Tab from '@mui/material/Tab';
import Tabs from '@mui/material/Tabs';
import TextField from '@mui/material/TextField';

import { PlusOutlined } from '@ant-design/icons';

import { mesQualityApi } from 'api/mes/quality';
import MesDataPage from './components/MesDataPage';

const emptyForm = {
  plantCd: '',
  inspectType: 'FINAL',
  inspectDt: '',
  itemCd: '',
  lotNo: '',
  inspectQty: 1,
  sampleQty: 1,
  passQty: 1,
  failQty: 0,
  inspectorId: 'SYSTEM',
  woId: '',
  resultId: '',
  vendorCd: '',
  judgeResult: 'PASS',
  judgeUserId: 'SYSTEM',
  processCd: '',
  inspectRmk: '',
  detail: {
    inspectStdId: '',
    sampleNo: 1,
    inspectItem: '외관',
    dataType: 'TEXT',
    measureValue: '',
    measureText: 'OK',
    lsl: '',
    usl: '',
    targetValue: '',
    judgeResult: 'PASS',
    defectCd: '',
    detailRmk: ''
  }
};

function today() {
  return new Date().toISOString().slice(0, 10);
}

function toNumberOrNull(value) {
  return value === '' || value === null || value === undefined ? null : Number(value);
}

export default function MesQuality() {
  const [tab, setTab] = useState('results');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [message, setMessage] = useState(null);
  const [activeMutate, setActiveMutate] = useState(null);

  const { data: stdResponse } = useSWR(['mes-inspect-std-options', form.plantCd, form.itemCd, form.inspectType], () =>
    mesQualityApi.inspectStds({ plantCd: form.plantCd, itemCd: form.itemCd, inspectType: form.inspectType })
  );
  const standards = stdResponse?.data ?? [];

  const openDialog = (mutate) => {
    setForm({ ...emptyForm, inspectDt: today() });
    setDialogOpen(true);
    setActiveMutate(() => mutate);
  };

  const handleValue = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const handleDetailValue = (field, value) => {
    setForm((current) => ({ ...current, detail: { ...current.detail, [field]: value } }));
  };

  const applyStandard = (inspectStdId) => {
    const standard = standards.find((item) => String(item.inspectStdId) === String(inspectStdId));
    setForm((current) => ({
      ...current,
      detail: {
        ...current.detail,
        inspectStdId,
        inspectItem: standard?.inspectItem || current.detail.inspectItem,
        dataType: standard?.dataType || current.detail.dataType,
        lsl: standard?.lsl ?? current.detail.lsl,
        usl: standard?.usl ?? current.detail.usl,
        targetValue: standard?.targetValue ?? current.detail.targetValue
      }
    }));
  };

  const handleSave = async () => {
    try {
      await mesQualityApi.createInspectResult({
        plantCd: form.plantCd,
        inspectType: form.inspectType,
        inspectDt: form.inspectDt,
        itemCd: form.itemCd,
        lotNo: form.lotNo,
        inspectQty: Number(form.inspectQty || 0),
        sampleQty: Number(form.sampleQty || 0),
        passQty: Number(form.passQty || 0),
        failQty: Number(form.failQty || 0),
        inspectorId: form.inspectorId,
        woId: toNumberOrNull(form.woId),
        resultId: toNumberOrNull(form.resultId),
        vendorCd: form.vendorCd,
        judgeResult: form.judgeResult,
        judgeUserId: form.judgeUserId,
        processCd: form.processCd,
        inspectRmk: form.inspectRmk,
        details: [
          {
            ...form.detail,
            inspectStdId: Number(form.detail.inspectStdId),
            sampleNo: Number(form.detail.sampleNo || 1),
            measureValue: toNumberOrNull(form.detail.measureValue),
            lsl: toNumberOrNull(form.detail.lsl),
            usl: toNumberOrNull(form.detail.usl),
            targetValue: toNumberOrNull(form.detail.targetValue)
          }
        ]
      });
      setMessage({ severity: 'success', text: '검사 결과가 등록되었습니다.' });
      setDialogOpen(false);
      await activeMutate?.();
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    }
  };

  return (
    <>
      <Tabs value={tab} onChange={(_, value) => setTab(value)} sx={{ mb: 2 }}>
        <Tab label="검사 결과" value="results" />
        <Tab label="검사 기준" value="standards" />
      </Tabs>
      {tab === 'results' && (
        <MesDataPage
          title="MES 품질 검사 결과"
          description="품목과 LOT 기준 검사 수량, 합격/불합격, 판정 결과를 조회하고 등록합니다."
          cardTitle="검사 결과 목록"
          swrKey="mes-inspect-results"
          fetcher={mesQualityApi.inspectResults}
          initialSearch={{ inspectNo: '', plantCd: '', itemCd: '', fromDt: '', toDt: '' }}
          filters={[
            { field: 'inspectNo', label: '검사번호' },
            { field: 'plantCd', label: '공장' },
            { field: 'itemCd', label: '품목 코드' },
            { field: 'fromDt', label: 'From', type: 'date' },
            { field: 'toDt', label: 'To', type: 'date' }
          ]}
          renderActions={({ mutate }) => (
            <Button variant="contained" startIcon={<PlusOutlined />} onClick={() => openDialog(mutate)}>
              검사 결과 등록
            </Button>
          )}
          columns={[
            { header: '검사번호', field: 'inspectNo' },
            { header: '검사일', field: 'inspectDt' },
            { header: '유형', field: 'inspectType' },
            { header: '품목', field: 'itemCd' },
            { header: 'LOT', field: 'lotNo' },
            { header: '검사수량', field: 'inspectQty', align: 'right' },
            { header: '합격', field: 'passQty', align: 'right' },
            { header: '불합격', field: 'failQty', align: 'right' },
            { header: '판정', render: (row) => <Chip label={row.judgeResult || '-'} size="small" color={row.judgeResult === 'PASS' ? 'success' : 'warning'} variant="light" /> }
          ]}
          getRowId={(row) => row.inspectId}
        />
      )}
      {tab === 'standards' && (
        <MesDataPage
          title="MES 검사 기준"
          description="품목별 검사 항목, 규격 상하한, 샘플링 기준을 조회합니다."
          cardTitle="검사 기준 목록"
          swrKey="mes-inspect-stds"
          fetcher={mesQualityApi.inspectStds}
          initialSearch={{ plantCd: '', itemCd: '', inspectType: '' }}
          filters={[
            { field: 'plantCd', label: '공장' },
            { field: 'itemCd', label: '품목 코드' },
            { field: 'inspectType', label: '검사 유형' }
          ]}
          columns={[
            { header: '품목', field: 'itemCd' },
            { header: '검사유형', field: 'inspectType' },
            { header: '순번', field: 'inspectItemSeq', align: 'right' },
            { header: '검사항목', field: 'inspectItem' },
            { header: '방법', field: 'inspectMethod' },
            { header: '규격', field: 'specValue' },
            { header: 'LSL', field: 'lsl', align: 'right' },
            { header: 'USL', field: 'usl', align: 'right' },
            { header: '단위', field: 'unit' },
            { header: '사용', field: 'useYn' }
          ]}
          getRowId={(row) => row.inspectStdId}
        />
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth maxWidth="md">
        <DialogTitle>검사 결과 등록</DialogTitle>
        <DialogContent dividers>
          <Grid container spacing={2} sx={{ pt: 0.5 }}>
            {[
              ['plantCd', '공장'],
              ['inspectType', '검사 유형'],
              ['itemCd', '품목 코드'],
              ['lotNo', 'LOT 번호'],
              ['inspectorId', '검사자'],
              ['judgeResult', '판정'],
              ['judgeUserId', '판정자'],
              ['processCd', '공정']
            ].map(([field, label]) => (
              <Grid key={field} size={{ xs: 12, md: 4 }}>
                <TextField fullWidth size="small" label={label} value={form[field]} onChange={(event) => handleValue(field, event.target.value)} />
              </Grid>
            ))}
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField fullWidth size="small" type="date" label="검사일" value={form.inspectDt} onChange={(event) => handleValue('inspectDt', event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
            </Grid>
            {['inspectQty', 'sampleQty', 'passQty', 'failQty', 'woId', 'resultId'].map((field) => (
              <Grid key={field} size={{ xs: 12, md: 4 }}>
                <TextField fullWidth size="small" type="number" label={field} value={form[field]} onChange={(event) => handleValue(field, event.target.value)} />
              </Grid>
            ))}
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField fullWidth size="small" label="검사기준 ID" value={form.detail.inspectStdId} onChange={(event) => applyStandard(event.target.value)} />
            </Grid>
            {['inspectItem', 'dataType', 'measureText', 'judgeResult', 'defectCd'].map((field) => (
              <Grid key={field} size={{ xs: 12, md: 4 }}>
                <TextField fullWidth size="small" label={field} value={form.detail[field]} onChange={(event) => handleDetailValue(field, event.target.value)} />
              </Grid>
            ))}
            {['sampleNo', 'measureValue', 'lsl', 'usl', 'targetValue'].map((field) => (
              <Grid key={field} size={{ xs: 12, md: 4 }}>
                <TextField fullWidth size="small" type="number" label={field} value={form.detail[field]} onChange={(event) => handleDetailValue(field, event.target.value)} />
              </Grid>
            ))}
            <Grid size={12}>
              <TextField fullWidth multiline minRows={3} label="비고" value={form.inspectRmk} onChange={(event) => handleValue('inspectRmk', event.target.value)} />
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
