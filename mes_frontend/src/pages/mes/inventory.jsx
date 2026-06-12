import { useState } from 'react';

import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
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

import { mesInventoryApi } from 'api/mes/inventory';
import MesDataPage from './components/MesDataPage';

const emptyTrans = {
  plantCd: '',
  transType: '입고',
  transReason: '시연 등록',
  itemCd: '',
  lotNo: '',
  transQty: 1,
  unit: 'EA',
  fromWarehouseCd: '',
  toWarehouseCd: '',
  fromLocationCd: '',
  toLocationCd: '',
  refType: 'MANUAL',
  refNo: '',
  refId: '',
  vendorCd: '',
  transUserId: 'SYSTEM',
  transRmk: '',
  regUserId: 'SYSTEM'
};

export default function MesInventory() {
  const [tab, setTab] = useState('stocks');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState(emptyTrans);
  const [message, setMessage] = useState(null);
  const [activeMutate, setActiveMutate] = useState(null);

  const openDialog = (mutate) => {
    setForm(emptyTrans);
    setDialogOpen(true);
    setActiveMutate(() => mutate);
  };

  const handleValue = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const handleSave = async () => {
    try {
      await mesInventoryApi.createTransaction({
        ...form,
        transQty: Number(form.transQty || 0),
        refId: form.refId ? Number(form.refId) : null
      });
      setMessage({ severity: 'success', text: '입출고가 등록되었습니다.' });
      setDialogOpen(false);
      await activeMutate?.();
    } catch (error) {
      setMessage({ severity: 'error', text: error.message });
    }
  };

  return (
    <>
      <Tabs value={tab} onChange={(_, value) => setTab(value)} sx={{ mb: 2 }}>
        <Tab label="현재고" value="stocks" />
        <Tab label="입출고 이력" value="transactions" />
      </Tabs>
      {tab === 'stocks' && (
        <MesDataPage
          title="MES 현재고"
          description="창고 단위 MES 재고를 조회하고 MCS 로케이션 재고와 비교할 기준 데이터를 확인합니다."
          cardTitle="현재고 목록"
          swrKey="mes-stocks"
          fetcher={mesInventoryApi.stocks}
          initialSearch={{ plantCd: '', warehouseCd: '', locationCd: '', itemCd: '', lotNo: '', stockStatus: '', stockId: '' }}
          filters={[
            { field: 'plantCd', label: '공장' },
            { field: 'warehouseCd', label: '창고' },
            { field: 'locationCd', label: 'Location' },
            { field: 'itemCd', label: '품목 코드' },
            { field: 'lotNo', label: 'LOT 번호' },
            { field: 'stockStatus', label: '상태' }
          ]}
          renderActions={({ mutate }) => (
            <Button variant="contained" startIcon={<PlusOutlined />} onClick={() => openDialog(mutate)}>
              입출고 등록
            </Button>
          )}
          columns={[
            { header: '공장', field: 'plantCd' },
            { header: '창고', field: 'warehouseCd' },
            { header: 'Location', field: 'locationCd' },
            { header: '품목', render: (row) => `${row.itemNm || row.itemCd} (${row.itemCd})` },
            { header: 'LOT', field: 'lotNo' },
            { header: '현재고', field: 'stockQty', align: 'right' },
            { header: '예약', field: 'reservedQty', align: 'right' },
            { header: '가용', field: 'availableQty', align: 'right' },
            { header: '단위', field: 'unit' },
            { header: '상태', field: 'stockStatus' }
          ]}
          getRowId={(row) => row.stockId}
        />
      )}
      {tab === 'transactions' && (
        <MesDataPage
          title="MES 입출고 이력"
          description="MES 재고 입출고 이력을 조회해 MCS 이동 완료 후 반영 내역을 추적합니다."
          cardTitle="입출고 이력 목록"
          swrKey="mes-transactions"
          fetcher={mesInventoryApi.transactions}
          initialSearch={{ plantCd: '', itemCd: '', fromDt: '', toDt: '' }}
          filters={[
            { field: 'plantCd', label: '공장' },
            { field: 'itemCd', label: '품목 코드' },
            { field: 'fromDt', label: 'From', type: 'date' },
            { field: 'toDt', label: 'To', type: 'date' }
          ]}
          renderActions={({ mutate }) => (
            <Button variant="contained" startIcon={<PlusOutlined />} onClick={() => openDialog(mutate)}>
              입출고 등록
            </Button>
          )}
          columns={[
            { header: '전표번호', field: 'transNo' },
            { header: '일자', field: 'transDt' },
            { header: '유형', field: 'transType' },
            { header: '사유', field: 'transReason' },
            { header: '품목', field: 'itemCd' },
            { header: 'LOT', field: 'lotNo' },
            { header: '수량', field: 'transQty', align: 'right' },
            { header: 'From', field: 'fromWarehouseCd' },
            { header: 'To', field: 'toWarehouseCd' },
            { header: '참조', field: 'refNo' }
          ]}
          getRowId={(row) => row.transId}
        />
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth maxWidth="md">
        <DialogTitle>MES 입출고 등록</DialogTitle>
        <DialogContent dividers>
          <Grid container spacing={2} sx={{ pt: 0.5 }}>
            {[
              ['plantCd', '공장'],
              ['transType', '유형'],
              ['transReason', '사유'],
              ['itemCd', '품목 코드'],
              ['lotNo', 'LOT 번호'],
              ['unit', '단위'],
              ['fromWarehouseCd', '출발 창고'],
              ['toWarehouseCd', '도착 창고'],
              ['fromLocationCd', '출발 Location'],
              ['toLocationCd', '도착 Location'],
              ['refNo', '참조번호'],
              ['vendorCd', '거래처'],
              ['transUserId', '처리자']
            ].map(([field, label]) => (
              <Grid key={field} size={{ xs: 12, md: 4 }}>
                <TextField fullWidth size="small" label={label} value={form[field]} onChange={(event) => handleValue(field, event.target.value)} />
              </Grid>
            ))}
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField fullWidth size="small" type="number" label="수량" value={form.transQty} onChange={(event) => handleValue('transQty', event.target.value)} />
            </Grid>
            <Grid size={12}>
              <TextField fullWidth multiline minRows={3} label="비고" value={form.transRmk} onChange={(event) => handleValue('transRmk', event.target.value)} />
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
