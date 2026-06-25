import { useMemo, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import useSWR from 'swr';

import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Divider from '@mui/material/Divider';
import Drawer from '@mui/material/Drawer';
import Grid from '@mui/material/Grid';
import IconButton from '@mui/material/IconButton';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import { BarChart, LineChart, PieChart } from '@mui/x-charts';

import { CloseOutlined } from '@ant-design/icons';

import MainCard from 'components/MainCard';
import { mesEquipmentApi } from 'api/mes/equipment';
import { mesInventoryApi } from 'api/mes/inventory';
import { mesProductionApi } from 'api/mes/production';
import { mesQualityApi } from 'api/mes/quality';
import { mesWorkOrderApi } from 'api/mes/workOrders';
import { plcEventApi } from 'api/mcs/plcEvents';
import { transferApi } from 'api/mcs/transfers';

const dashboardSWRConfig = {
  revalidateOnFocus: false,
  revalidateOnReconnect: false
};

export const workOrderStatusKeys = ['대기', '진행', '완료', '취소'];
export const transferStatusKeys = ['REQUESTED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED'];
export const signalStatusKeys = ['NORMAL', 'ERROR', 'INTERLOCK', 'VALIDATION_FAILED'];

export const workOrderStatusLabels = {
  대기: '대기',
  진행: '진행',
  완료: '완료',
  취소: '취소'
};

export const transferStatusLabels = {
  REQUESTED: '요청',
  IN_PROGRESS: '이동 중',
  COMPLETED: '완료',
  FAILED: '실패',
  CANCELLED: '취소'
};

export const signalStatusLabels = {
  NORMAL: '정상',
  ERROR: '오류',
  INTERLOCK: '인터록',
  VALIDATION_FAILED: '데이터 누락'
};

export function getApiData(response, fallback) {
  return response?.data ?? fallback;
}

function asList(value) {
  if (Array.isArray(value)) return value;
  if (Array.isArray(value?.content)) return value.content;
  return [];
}

export function toNumber(value) {
  const number = Number(value);
  return Number.isFinite(number) ? number : 0;
}

function countBy(list, keyGetter) {
  return list.reduce((acc, item) => {
    const key = keyGetter(item) || '기타';
    acc[key] = (acc[key] || 0) + 1;
    return acc;
  }, {});
}

function sumBy(list, valueGetter) {
  return list.reduce((sum, item) => sum + toNumber(valueGetter(item)), 0);
}

function toPieData(items) {
  return items.map((item, index) => ({
    id: index,
    label: item.label,
    value: item.value || 0,
    color: item.color
  }));
}

function buildUrl(path, params) {
  const query = new URLSearchParams();
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') {
      query.set(key, String(value));
    }
  });
  const queryString = query.toString();
  return queryString ? `${path}?${queryString}` : path;
}

export function getStockStatus(stock) {
  const status = String(stock.stockStatus || '').toUpperCase();
  if (toNumber(stock.availableQty) <= 0) return '부족';
  if (['HOLD', 'BLOCKED', 'INSUFFICIENT', '부족'].includes(status)) return '사용 제한';
  return '정상';
}

export function getEquipmentStatus(status) {
  const value = String(status.operStatus || '').toUpperCase();
  if (['RUN', 'RUNNING', '가동', '정상'].includes(value)) return '가동';
  if (['STOP', 'DOWN', 'ERROR', '비가동', '정지'].includes(value)) return '비가동';
  return '확인 필요';
}

function getSignalStatus(event) {
  if (event.processResult === 'VALIDATION_FAILED') return 'VALIDATION_FAILED';
  if (event.eventStatus === 'ERROR') return 'ERROR';
  if (event.eventStatus === 'INTERLOCK') return 'INTERLOCK';
  return 'NORMAL';
}

// 'YYYY-MM-DD HH:mm:ss' / ISO 모두에서 날짜 부분만 안전하게 추출한다.
function toDateKey(value) {
  if (!value) return null;
  const s = String(value).trim();
  return s.length >= 10 ? s.slice(0, 10) : null;
}

// 더미 데이터마다 날짜 필드명이 다를 수 있어 후보 키 중 먼저 값이 있는 것을 쓴다.
function pickDate(obj, keys) {
  for (const key of keys) {
    if (obj && obj[key]) return obj[key];
  }
  return null;
}

const TRANSFER_DATE_KEYS = ['updDtm', 'completedDtm', 'regDtm', 'createdDtm', 'transferDtm'];
const WORKORDER_DATE_KEYS = ['actualEndDtm', 'actualStartDtm', 'planEndDtm', 'planStartDtm', 'regDtm'];

/**
 * 일자별 운영 추이를 만든다. 절대 날짜(오늘) 대신 데이터에 실제로 존재하는 날짜를 모아
 * 최근 8일치를 축으로 사용하므로, 더미 데이터의 기준 날짜와 무관하게 항상 실제 데이터를 보여준다.
 */
function buildOperationTrend(plcEvents, transfers, workOrders) {
  const dateSet = new Set();
  plcEvents.forEach((e) => {
    const k = toDateKey(e.eventDtm);
    if (k) dateSet.add(k);
  });
  transfers.forEach((t) => {
    const k = toDateKey(pickDate(t, TRANSFER_DATE_KEYS));
    if (k) dateSet.add(k);
  });
  workOrders.forEach((w) => {
    const k = toDateKey(pickDate(w, WORKORDER_DATE_KEYS));
    if (k) dateSet.add(k);
  });

  const days = [...dateSet].sort().slice(-8);
  const empty = { labels: [], plcNormal: [], plcIssue: [], plcMissing: [], transferDone: [], transferFailed: [], workCompleted: [] };
  if (days.length === 0) return empty;

  const idx = new Map(days.map((d, i) => [d, i]));
  const zeros = () => days.map(() => 0);
  const plcNormal = zeros();
  const plcIssue = zeros();
  const plcMissing = zeros();
  const transferDone = zeros();
  const transferFailed = zeros();
  const workCompleted = zeros();

  plcEvents.forEach((e) => {
    const i = idx.get(toDateKey(e.eventDtm));
    if (i === undefined) return;
    const status = getSignalStatus(e);
    if (status === 'VALIDATION_FAILED') plcMissing[i] += 1;
    else if (status === 'ERROR' || status === 'INTERLOCK') plcIssue[i] += 1;
    else plcNormal[i] += 1;
  });
  transfers.forEach((t) => {
    const i = idx.get(toDateKey(pickDate(t, TRANSFER_DATE_KEYS)));
    if (i === undefined) return;
    if (t.transferStatus === 'FAILED') transferFailed[i] += 1;
    else if (t.transferStatus === 'COMPLETED') transferDone[i] += 1;
  });
  workOrders.forEach((w) => {
    const i = idx.get(toDateKey(pickDate(w, WORKORDER_DATE_KEYS)));
    if (i === undefined) return;
    if (w.woStatus === '완료') workCompleted[i] += 1;
  });

  return {
    labels: days.map((d) => d.slice(5)),
    plcNormal,
    plcIssue,
    plcMissing,
    transferDone,
    transferFailed,
    workCompleted
  };
}

/**
 * 운영 대시보드 공용 데이터 훅.
 * MES/MCS/AI 화면이 공통으로 쓰는 원본 데이터 조회, 집계(dashboard), 상세 Drawer 상태와
 * 상태별 열기 핸들러를 한곳에서 제공한다.
 */
export function useOperationsDashboard() {
  const [detail, setDetail] = useState(null);

  const { data: workOrderResponse, error: workOrderError, isLoading: workOrderLoading } = useSWR(
    ['ops-dashboard-work-orders'],
    () => mesWorkOrderApi.list({ page: 1, size: 200 }),
    dashboardSWRConfig
  );
  const { data: transferResponse, error: transferError, isLoading: transferLoading } = useSWR(
    ['ops-dashboard-transfers'],
    () => transferApi.list({ page: 1, size: 200 }),
    dashboardSWRConfig
  );
  const { data: plcEventResponse, error: plcEventError, isLoading: plcEventLoading } = useSWR(
    ['ops-dashboard-plc-events'],
    () => plcEventApi.list({ page: 1, size: 200 }),
    dashboardSWRConfig
  );
  const { data: defectResponse, error: defectError, isLoading: defectLoading } = useSWR(
    ['ops-dashboard-defects'],
    () => mesProductionApi.defectHistories({}),
    dashboardSWRConfig
  );
  const { data: inspectResponse, error: inspectError, isLoading: inspectLoading } = useSWR(
    ['ops-dashboard-inspections'],
    () => mesQualityApi.inspectResults({}),
    dashboardSWRConfig
  );
  const { data: stockResponse, error: stockError, isLoading: stockLoading } = useSWR(
    ['ops-dashboard-stocks'],
    () => mesInventoryApi.stocks({ size: 200 }),
    dashboardSWRConfig
  );
  const { data: equipmentResponse, error: equipmentError, isLoading: equipmentLoading } = useSWR(
    ['ops-dashboard-equipment-status'],
    () => mesEquipmentApi.operStatuses({}),
    dashboardSWRConfig
  );

  const workOrders = asList(getApiData(workOrderResponse, []));
  const transfers = asList(getApiData(transferResponse, []));
  const plcEvents = asList(getApiData(plcEventResponse, []));
  const defects = asList(getApiData(defectResponse, []));
  const inspections = asList(getApiData(inspectResponse, []));
  const stocks = asList(getApiData(stockResponse, []));
  const equipmentStatuses = asList(getApiData(equipmentResponse, []));

  const dashboard = useMemo(() => {
    const defectCount = defects.length;
    const defectQty = sumBy(defects, (defect) => defect.defectQty);
    const failedInspections = inspections.filter((inspect) =>
      toNumber(inspect.failQty) > 0 || ['FAIL', 'NG', '불합격'].includes(String(inspect.judgeResult || '').toUpperCase())
    );
    const failedInspectionCount = failedInspections.length;
    const failedInspectionQty = sumBy(failedInspections, (inspect) => inspect.failQty);
    const passedInspectionCount = Math.max(inspections.length - failedInspectionCount, 0);

    const stockGroups = countBy(stocks, getStockStatus);
    const lowStockCount = stockGroups.부족 || 0;
    const restrictedStockCount = stockGroups['사용 제한'] || 0;
    const totalStockQty = sumBy(stocks, (stock) => stock.stockQty);
    const availableStockQty = sumBy(stocks, (stock) => stock.availableQty);

    const equipmentGroups = countBy(equipmentStatuses, getEquipmentStatus);
    const runningEquipmentCount = equipmentGroups.가동 || 0;
    const downEquipmentCount = equipmentGroups.비가동 || 0;
    const unknownEquipmentCount = equipmentGroups['확인 필요'] || 0;
    const equipmentIssueCount = downEquipmentCount + unknownEquipmentCount;

    const transferCounts = countBy(transfers, (transfer) => transfer.transferStatus);
    const failedTransferCount = transferCounts.FAILED || 0;
    const movingTransferCount = (transferCounts.REQUESTED || 0) + (transferCounts.IN_PROGRESS || 0);

    const signalCounts = countBy(plcEvents, getSignalStatus);
    const dataMissingCount = signalCounts.VALIDATION_FAILED || 0;
    const plcIssueCount = (signalCounts.ERROR || 0) + (signalCounts.INTERLOCK || 0) + dataMissingCount;

    const workOrderCounts = countBy(workOrders, (order) => order.woStatus);
    const waitingWorkOrderCount = workOrderCounts.대기 || 0;
    const inProgressWorkOrderCount = workOrderCounts.진행 || 0;

    const qualityRiskCount = defectCount + failedInspectionCount;
    const operationRiskCount = failedTransferCount + dataMissingCount + lowStockCount + equipmentIssueCount + qualityRiskCount;
    const mainCause = operationRiskCount > 0
      ? `현재 주의가 필요한 항목은 ${operationRiskCount}건입니다. 품질, 재고, 설비 그래프에서 우선순위를 확인하세요.`
      : '현재 조회된 데이터 기준으로 큰 운영 차단 요인은 없습니다.';

    return {
      defectCount,
      defectQty,
      failedInspections,
      failedInspectionCount,
      failedInspectionQty,
      passedInspectionCount,
      lowStockCount,
      restrictedStockCount,
      totalStockQty,
      availableStockQty,
      runningEquipmentCount,
      downEquipmentCount,
      unknownEquipmentCount,
      equipmentIssueCount,
      failedTransferCount,
      movingTransferCount,
      dataMissingCount,
      plcIssueCount,
      waitingWorkOrderCount,
      inProgressWorkOrderCount,
      mainCause,
      qualityPieData: toPieData([
        { label: '검사 통과', value: passedInspectionCount, color: '#52c41a' },
        { label: '검사 부적합', value: failedInspectionCount, color: '#faad14' },
        { label: '불량 이력', value: defectCount, color: '#ff4d4f' }
      ]),
      equipmentPieData: toPieData([
        { label: '가동', value: runningEquipmentCount, color: '#52c41a' },
        { label: '비가동', value: downEquipmentCount, color: '#ff4d4f' },
        { label: '확인 필요', value: unknownEquipmentCount, color: '#faad14' }
      ]),
      stockStatusData: toPieData([
        { label: '정상', value: stockGroups.정상 || 0, color: '#52c41a' },
        { label: '부족', value: lowStockCount, color: '#faad14' },
        { label: '사용 제한', value: restrictedStockCount, color: '#ff4d4f' }
      ]),
      workOrderCounts,
      transferCounts,
      signalCounts,
      trend: buildOperationTrend(plcEvents, transfers, workOrders)
    };
  }, [workOrders, transfers, plcEvents, defects, inspections, stocks, equipmentStatuses]);

  const isLoading = workOrderLoading || transferLoading || plcEventLoading || defectLoading || inspectLoading || stockLoading || equipmentLoading;
  const errors = [workOrderError, transferError, plcEventError, defectError, inspectError, stockError, equipmentError].filter(Boolean);

  const openDefects = () => {
    const rows = defects
      .toSorted((a, b) => toNumber(b.defectQty) - toNumber(a.defectQty))
      .map((defect) => ({
        ...defect,
        id: defect.defectId,
        detailUrl: buildUrl('/mes/defects', {
          defectId: defect.defectId,
          itemCd: defect.itemCd,
          fromDt: defect.defectDt,
          toDt: defect.defectDt
        }),
        detailLabel: '불량 확인'
      }));

    setDetail({
      title: '불량 이력',
      description: '생산 중 발생한 불량 이력을 확인합니다.',
      guide: defects.length > 0
        ? '불량 수량이 많은 항목부터 생산 실적, 설비 상태, 작업 조건을 같이 확인하세요.'
        : '현재 조회된 불량 이력이 없습니다.',
      guideSeverity: defects.length > 0 ? 'warning' : 'success',
      actionUrl: '/mes/defects',
      actionLabel: '불량 이력 화면으로 이동',
      maxRows: 12,
      totalCount: defects.length,
      summaryItems: [
        { label: '전체', value: `${defects.length}건`, color: 'default' },
        { label: '불량 수량', value: `${dashboard.defectQty}건`, color: dashboard.defectQty > 0 ? 'warning' : 'success' }
      ],
      columns: [
        { key: 'defectDt', label: '일자' },
        { key: 'defectNm', label: '불량명' },
        { key: 'defectQty', label: '수량', align: 'right' },
        { key: 'defectCause', label: '원인' },
        { key: 'defectAction', label: '조치' }
      ],
      rows
    });
  };

  const openInspections = () => {
    const rows = dashboard.failedInspections
      .toSorted((a, b) => toNumber(b.failQty) - toNumber(a.failQty))
      .map((inspect) => ({
        ...inspect,
        id: inspect.inspectId,
        detailUrl: buildUrl('/mes/quality', {
          inspectNo: inspect.inspectNo,
          itemCd: inspect.itemCd,
          fromDt: inspect.inspectDt,
          toDt: inspect.inspectDt
        }),
        detailLabel: '검사 확인'
      }));

    setDetail({
      title: '검사 부적합',
      description: '검사 결과 중 부적합 수량이 있는 항목입니다.',
      guide: dashboard.failedInspectionCount > 0
        ? '부적합 검사는 불량 이력 등록 여부와 LOT, 품목, 공정 정보를 함께 확인하세요.'
        : '현재 조회된 검사 부적합이 없습니다.',
      guideSeverity: dashboard.failedInspectionCount > 0 ? 'warning' : 'success',
      actionUrl: '/mes/quality',
      actionLabel: '검사 결과 화면으로 이동',
      secondaryActionUrl: '/mes/defects',
      secondaryActionLabel: '불량 이력 확인',
      columns: [
        { key: 'inspectNo', label: '검사번호' },
        { key: 'itemCd', label: '품목' },
        { key: 'inspectQty', label: '검사수량', align: 'right' },
        { key: 'failQty', label: '부적합', align: 'right' },
        { key: 'judgeResult', label: '판정' }
      ],
      maxRows: 12,
      totalCount: dashboard.failedInspections.length,
      summaryItems: [
        { label: '부적합 건수', value: `${dashboard.failedInspectionCount}건`, color: dashboard.failedInspectionCount > 0 ? 'warning' : 'success' },
        { label: '부적합 수량', value: `${dashboard.failedInspectionQty}건`, color: dashboard.failedInspectionQty > 0 ? 'warning' : 'success' }
      ],
      rows
    });
  };

  const openStocks = () => {
    const rows = stocks
      .filter((stock) => getStockStatus(stock) !== '정상')
      .map((stock) => ({
        ...stock,
        id: stock.stockId || `${stock.itemCd}-${stock.locationCd || stock.warehouseCd || ''}`,
        stockHealth: getStockStatus(stock),
        detailUrl: buildUrl('/mes/inventory', {
          itemCd: stock.itemCd,
          warehouseCd: stock.warehouseCd,
          locationCd: stock.locationCd
        }),
        detailLabel: '재고 확인'
      }));

    setDetail({
      title: '재고 확인 필요',
      description: '가용재고가 없거나 사용 제한 상태인 품목입니다.',
      guide: rows.length > 0
        ? '생산 시작 전 MES 재고와 MCS 로케이션 재고가 같은 흐름으로 맞는지 확인하세요.'
        : '현재 조회된 재고 부족 또는 사용 제한 품목이 없습니다.',
      guideSeverity: rows.length > 0 ? 'warning' : 'success',
      actionUrl: '/mes/inventory',
      actionLabel: 'MES 재고 화면으로 이동',
      secondaryActionUrl: '/mcs/location-stock',
      secondaryActionLabel: 'MCS 로케이션 재고 확인',
      columns: [
        { key: 'itemCd', label: '품목' },
        { key: 'locationCd', label: '위치' },
        { key: 'stockQty', label: '현재고', align: 'right' },
        { key: 'availableQty', label: '가용', align: 'right' },
        { key: 'stockHealth', label: '상태' }
      ],
      rows
    });
  };

  const openEquipment = () => {
    const rows = equipmentStatuses
      .filter((status) => getEquipmentStatus(status) !== '가동')
      .map((status) => ({
        ...status,
        id: status.operId || status.equipmentCd,
        health: getEquipmentStatus(status),
        detailUrl: buildUrl('/mes/equipment', {
          equipmentCd: status.equipmentCd
        }),
        detailLabel: '설비 확인'
      }));

    setDetail({
      title: '설비 상태 확인',
      description: '비가동 또는 확인 필요 상태의 설비입니다.',
      guide: rows.length > 0
        ? '설비 상태가 생산 지연이나 MCS 이동 실패와 연결되어 있는지 PLC 이벤트 화면과 함께 확인하세요.'
        : '현재 조회된 설비 상태 이상은 없습니다.',
      guideSeverity: rows.length > 0 ? 'warning' : 'success',
      actionUrl: '/mes/equipment',
      actionLabel: '설비 현황 화면으로 이동',
      secondaryActionUrl: '/mcs/plc-events',
      secondaryActionLabel: 'PLC 이벤트 확인',
      columns: [
        { key: 'equipmentCd', label: '설비' },
        { key: 'operStatus', label: '상태' },
        { key: 'health', label: '판단' },
        { key: 'woId', label: '작업지시' },
        { key: 'prodQty', label: '생산수량', align: 'right' }
      ],
      rows
    });
  };

  const openWorkOrders = (status) => {
    const rows = (status ? workOrders.filter((order) => order.woStatus === status) : workOrders)
      .map((order) => ({
        ...order,
        id: order.woId || order.woNo,
        detailUrl: buildUrl('/mes/work-orders', {
          woId: order.woId,
          woNo: order.woNo,
          woStatus: order.woStatus
        }),
        detailLabel: '작업 확인'
      }));

    setDetail({
      title: status ? `생산 지시 상태: ${workOrderStatusLabels[status] || status}` : '생산 지시 전체',
      description: '생산 지시 상태별 현황입니다.',
      guide: status === '대기'
        ? '대기 상태 작업은 자재 이동 완료 여부와 설비 가동 상태를 함께 확인하세요.'
        : '생산 흐름이 정상적으로 이어지는지 상태별 작업 건을 확인하세요.',
      guideSeverity: rows.length > 0 ? 'info' : 'success',
      actionUrl: '/mes/work-orders',
      actionLabel: '작업 오더 화면으로 이동',
      columns: [
        { key: 'woNo', label: '작업오더' },
        { key: 'itemNm', label: '품목' },
        { key: 'woStatus', label: '상태' },
        { key: 'orderQty', label: '지시수량', align: 'right' },
        { key: 'equipmentCd', label: '설비' }
      ],
      rows
    });
  };

  const openTransfers = (status) => {
    const rows = (status ? transfers.filter((transfer) => transfer.transferStatus === status) : transfers)
      .map((transfer) => ({
        ...transfer,
        id: transfer.transferId || transfer.transferNo,
        detailUrl: buildUrl('/mcs/transfers', {
          transferId: transfer.transferId,
          transferNo: transfer.transferNo,
          transferStatus: transfer.transferStatus
        }),
        detailLabel: '이동 확인'
      }));

    setDetail({
      title: status ? `자재 이동 상태: ${transferStatusLabels[status] || status}` : '자재 이동 전체',
      description: 'MCS 자재 이동 상태별 현황입니다.',
      guide: status === 'FAILED'
        ? '실패 이동은 PLC 이벤트와 출발/도착 위치, LOT 정보를 함께 확인하세요.'
        : '자재 이동이 생산 시작 조건과 연결되어 있는지 확인하세요.',
      guideSeverity: status === 'FAILED' && rows.length > 0 ? 'warning' : 'info',
      actionUrl: '/mcs/transfers',
      actionLabel: '이동 관리 화면으로 이동',
      secondaryActionUrl: '/mcs/plc-events',
      secondaryActionLabel: 'PLC 이벤트 확인',
      columns: [
        { key: 'transferNo', label: '이동번호' },
        { key: 'transferStatus', label: '상태' },
        { key: 'fromLocationCd', label: '출발' },
        { key: 'toLocationCd', label: '도착' },
        { key: 'itemCd', label: '품목' }
      ],
      rows
    });
  };

  const openSignals = (status) => {
    const rows = (status ? plcEvents.filter((event) => getSignalStatus(event) === status) : plcEvents)
      .map((event) => ({
        ...event,
        id: event.eventId,
        detailUrl: buildUrl('/mcs/plc-events', {
          eventId: event.eventId,
          equipmentCd: event.equipmentCd,
          eventStatus: event.eventStatus,
          processResult: event.processResult
        }),
        detailLabel: '신호 확인'
      }));

    setDetail({
      title: status ? `PLC 신호: ${signalStatusLabels[status] || status}` : 'PLC 이벤트 전체',
      description: 'PLC 이벤트와 처리 결과를 확인합니다.',
      guide: status === 'VALIDATION_FAILED'
        ? '데이터 누락은 PLC payload 필수 필드와 태그 매핑을 먼저 확인하세요.'
        : '설비 신호가 자재 이동 실패와 연결되어 있는지 확인하세요.',
      guideSeverity: ['ERROR', 'INTERLOCK', 'VALIDATION_FAILED'].includes(status) && rows.length > 0 ? 'warning' : 'info',
      actionUrl: '/mcs/plc-events',
      actionLabel: 'PLC 이벤트 화면으로 이동',
      columns: [
        { key: 'eventDtm', label: '일시' },
        { key: 'equipmentCd', label: '설비' },
        { key: 'eventType', label: '이벤트' },
        { key: 'eventStatus', label: '상태' },
        { key: 'processResult', label: '처리결과' },
        { key: 'processMessage', label: '메시지' }
      ],
      rows
    });
  };

  return {
    dashboard,
    isLoading,
    errors,
    workOrders,
    transfers,
    plcEvents,
    defects,
    inspections,
    stocks,
    equipmentStatuses,
    detail,
    setDetail,
    openDefects,
    openInspections,
    openStocks,
    openEquipment,
    openWorkOrders,
    openTransfers,
    openSignals
  };
}

export function StatCard({ title, value, caption, color = 'primary', icon }) {
  return (
    <MainCard>
      <Stack spacing={1}>
        <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="body2" color="text.secondary">{title}</Typography>
          <Box sx={{ color: `${color}.main`, fontSize: 20 }}>{icon}</Box>
        </Stack>
        <Typography variant="h3">{value}</Typography>
        <Typography variant="caption" color="text.secondary">{caption}</Typography>
      </Stack>
    </MainCard>
  );
}

export function SectionTitle({ title, caption }) {
  return (
    <Box>
      <Typography variant="h5">{title}</Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
        {caption}
      </Typography>
    </Box>
  );
}

function EmptyRows({ colSpan }) {
  return (
    <TableRow>
      <TableCell colSpan={colSpan} align="center">표시할 데이터가 없습니다.</TableCell>
    </TableRow>
  );
}

export function DetailDrawer({ detail, onClose }) {
  const open = Boolean(detail);
  const rows = detail?.rows || [];
  const maxRows = detail?.maxRows || 12;
  const visibleRows = rows.slice(0, maxRows);
  const hiddenCount = Math.max((detail?.totalCount ?? rows.length) - visibleRows.length, 0);
  const hasRowLinks = visibleRows.some((row) => row.detailUrl);

  return (
    <Drawer
      anchor="right"
      open={open}
      onClose={onClose}
      slotProps={{ paper: { sx: { width: { xs: '100%', sm: 680 }, maxWidth: '100%' } } }}
    >
      <Box sx={{ p: 2.5 }}>
        <Stack direction="row" sx={{ alignItems: 'flex-start', justifyContent: 'space-between', gap: 2 }}>
          <Box>
            <Typography variant="h4">{detail?.title || '-'}</Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
              {detail?.description || ''}
            </Typography>
          </Box>
          <IconButton onClick={onClose} aria-label="닫기">
            <CloseOutlined />
          </IconButton>
        </Stack>

        <Divider sx={{ my: 2 }} />

        {detail?.guide && (
          <Alert severity={detail.guideSeverity || 'info'} sx={{ mb: 2 }}>
            {detail.guide}
          </Alert>
        )}

        {detail?.summaryItems?.length > 0 && (
          <Stack direction="row" spacing={1} sx={{ mb: 2, flexWrap: 'wrap', rowGap: 1 }}>
            {detail.summaryItems.map((item) => (
              <Chip key={item.label} label={`${item.label} ${item.value}`} color={item.color || 'default'} size="small" variant="light" />
            ))}
          </Stack>
        )}

        {(detail?.actionUrl || detail?.secondaryActionUrl) && (
          <Stack direction="row" spacing={1} sx={{ mb: 2, flexWrap: 'wrap', rowGap: 1 }}>
            {detail.actionUrl && (
              <Button component={RouterLink} to={detail.actionUrl} variant="contained" size="small">
                {detail.actionLabel || '관련 화면으로 이동'}
              </Button>
            )}
            {detail.secondaryActionUrl && (
              <Button component={RouterLink} to={detail.secondaryActionUrl} variant="outlined" size="small">
                {detail.secondaryActionLabel || '추가 화면 확인'}
              </Button>
            )}
          </Stack>
        )}

        {hiddenCount > 0 && (
          <Alert severity="info" sx={{ mb: 2 }}>
            화면이 길어지지 않도록 상위 {visibleRows.length}건만 표시합니다. 나머지 {hiddenCount}건은 전체 화면에서 확인하세요.
          </Alert>
        )}

        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                {(detail?.columns || []).map((column) => (
                  <TableCell key={column.key} align={column.align || 'left'}>{column.label}</TableCell>
                ))}
                {hasRowLinks && <TableCell align="center">바로가기</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {visibleRows.length === 0 && <EmptyRows colSpan={(detail?.columns?.length || 1) + (hasRowLinks ? 1 : 0)} />}
              {visibleRows.map((row, index) => (
                <TableRow key={row.id || index} hover>
                  {(detail?.columns || []).map((column) => (
                    <TableCell key={column.key} align={column.align || 'left'}>
                      {column.render ? column.render(row) : (row[column.key] ?? '-')}
                    </TableCell>
                  ))}
                  {hasRowLinks && (
                    <TableCell align="center">
                      {row.detailUrl ? (
                        <Button component={RouterLink} to={row.detailUrl} size="small" variant="outlined">
                          {row.detailLabel || '보기'}
                        </Button>
                      ) : '-'}
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Box>
    </Drawer>
  );
}

export function QualityResultPanel({ dashboard, onOpenDefects, onOpenInspections }) {
  const openByIndex = (index) => {
    if (index === 0 || index === 1) {
      onOpenDefects();
      return;
    }
    onOpenInspections();
  };

  return (
    <MainCard>
      <Stack spacing={2}>
        <SectionTitle title="품질 결과" caption="불량 이력과 검사 부적합을 한 화면에서 비교합니다." />
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 7 }}>
            <BarChart
              xAxis={[{ scaleType: 'band', data: ['불량 건수', '불량 수량', '검사 부적합'] }]}
              series={[{
                data: [dashboard.defectCount, dashboard.defectQty, dashboard.failedInspectionCount],
                label: '품질 이상',
                color: '#faad14'
              }]}
              height={280}
              margin={{ top: 24, right: 16, bottom: 36, left: 44 }}
              onItemClick={(_, item) => openByIndex(item.dataIndex)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 5 }}>
            <PieChart
              series={[{
                data: dashboard.qualityPieData,
                innerRadius: 48,
                paddingAngle: 2
              }]}
              height={260}
              margin={{ top: 8, bottom: 8, left: 8, right: 8 }}
              onItemClick={(_, item) => (item.dataIndex === 2 ? onOpenDefects() : onOpenInspections())}
            />
          </Grid>
        </Grid>
        <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', rowGap: 1 }}>
          <Button size="small" variant="outlined" onClick={onOpenDefects}>불량 이력 보기</Button>
          <Button size="small" variant="outlined" onClick={onOpenInspections}>검사 부적합 보기</Button>
        </Stack>
      </Stack>
    </MainCard>
  );
}

export function StockPanel({ dashboard, onOpen }) {
  return (
    <MainCard>
      <Stack spacing={2}>
        <SectionTitle title="재고 상태" caption="현재고와 가용재고, 부족/사용 제한 품목을 같이 봅니다." />
        <BarChart
          xAxis={[{ scaleType: 'band', data: ['전체 재고', '가용 재고', '부족 품목', '사용 제한'] }]}
          series={[{
            data: [dashboard.totalStockQty, dashboard.availableStockQty, dashboard.lowStockCount, dashboard.restrictedStockCount],
            label: '재고',
            color: '#1677ff'
          }]}
          height={270}
          margin={{ top: 24, right: 16, bottom: 36, left: 54 }}
          onItemClick={onOpen}
        />
        <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', rowGap: 1 }}>
          <Chip label={`가용 ${dashboard.availableStockQty}`} size="small" color="success" variant="light" />
          <Chip label={`부족 ${dashboard.lowStockCount}`} size="small" color={dashboard.lowStockCount > 0 ? 'warning' : 'success'} variant="light" />
          <Chip label={`사용 제한 ${dashboard.restrictedStockCount}`} size="small" color={dashboard.restrictedStockCount > 0 ? 'warning' : 'success'} variant="light" />
          <Button size="small" variant="outlined" onClick={onOpen}>재고 상세 보기</Button>
        </Stack>
      </Stack>
    </MainCard>
  );
}

export function EquipmentPanel({ dashboard, onOpen }) {
  return (
    <MainCard>
      <Stack spacing={2}>
        <SectionTitle title="설비 가동" caption="가동, 비가동, 확인 필요 설비를 비율과 건수로 봅니다." />
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 5 }}>
            <PieChart
              series={[{
                data: dashboard.equipmentPieData,
                innerRadius: 48,
                paddingAngle: 2
              }]}
              height={250}
              margin={{ top: 8, bottom: 8, left: 8, right: 8 }}
              onItemClick={onOpen}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 7 }}>
            <BarChart
              xAxis={[{ scaleType: 'band', data: ['가동', '비가동', '확인 필요'] }]}
              series={[{
                data: [dashboard.runningEquipmentCount, dashboard.downEquipmentCount, dashboard.unknownEquipmentCount],
                label: '설비',
                color: '#52c41a'
              }]}
              height={250}
              margin={{ top: 24, right: 16, bottom: 36, left: 44 }}
              onItemClick={onOpen}
            />
          </Grid>
        </Grid>
        <Button size="small" variant="outlined" onClick={onOpen} sx={{ alignSelf: 'flex-start' }}>
          설비 상세 보기
        </Button>
      </Stack>
    </MainCard>
  );
}

export function WorkOrderStatusPanel({ dashboard, onOpenWorkOrders }) {
  return (
    <MainCard>
      <Stack spacing={2}>
        <SectionTitle title="생산 지시 상태" caption="대기, 진행, 완료, 취소 상태를 그래프로 봅니다." />
        <PieChart
          series={[{
            data: toPieData(workOrderStatusKeys.map((key) => ({
              label: workOrderStatusLabels[key] || key,
              value: dashboard.workOrderCounts[key] || 0
            }))),
            innerRadius: 48,
            paddingAngle: 2
          }]}
          height={260}
          margin={{ top: 8, bottom: 8, left: 8, right: 8 }}
          onItemClick={(_, item) => onOpenWorkOrders(workOrderStatusKeys[item.dataIndex])}
        />
        <Button size="small" variant="outlined" onClick={() => onOpenWorkOrders()}>
          생산 지시 전체 보기
        </Button>
      </Stack>
    </MainCard>
  );
}

export function TransferStatusPanel({ dashboard, onOpenTransfers }) {
  return (
    <MainCard>
      <Stack spacing={2}>
        <SectionTitle title="자재 이동 상태" caption="요청, 이동 중, 완료, 실패, 취소 상태를 비교합니다." />
        <BarChart
          xAxis={[{ scaleType: 'band', data: transferStatusKeys.map((key) => transferStatusLabels[key] || key) }]}
          series={[{
            data: transferStatusKeys.map((key) => dashboard.transferCounts[key] || 0),
            label: '자재 이동',
            color: '#1677ff'
          }]}
          height={280}
          margin={{ top: 24, right: 16, bottom: 40, left: 44 }}
          onItemClick={(_, item) => onOpenTransfers(transferStatusKeys[item.dataIndex])}
        />
        <Button size="small" variant="outlined" onClick={() => onOpenTransfers()}>
          자재 이동 전체 보기
        </Button>
      </Stack>
    </MainCard>
  );
}

export function SignalStatusPanel({ dashboard, onOpenSignals }) {
  return (
    <MainCard>
      <Stack spacing={2}>
        <SectionTitle title="PLC 신호 분포" caption="정상, 오류, 인터록, 데이터 누락 신호를 따로 확인합니다." />
        <BarChart
          xAxis={[{ scaleType: 'band', data: signalStatusKeys.map((key) => signalStatusLabels[key] || key) }]}
          series={[{
            data: signalStatusKeys.map((key) => dashboard.signalCounts[key] || 0),
            label: 'PLC 신호',
            color: '#722ed1'
          }]}
          height={280}
          margin={{ top: 24, right: 16, bottom: 40, left: 44 }}
          onItemClick={(_, item) => onOpenSignals(signalStatusKeys[item.dataIndex])}
        />
        <Button size="small" variant="outlined" onClick={() => onOpenSignals()}>
          PLC 이벤트 전체 보기
        </Button>
      </Stack>
    </MainCard>
  );
}

export function PlcSignalTrendPanel({ dashboard, onOpenSignals }) {
  const t = dashboard.trend;
  if (!t || t.labels.length === 0) return null;

  return (
    <MainCard>
      <Stack spacing={2}>
        <SectionTitle title="PLC 신호 추이" caption="일자별 PLC 이벤트를 정상·이상·데이터 누락으로 쌓아 흐름을 봅니다." />
        <LineChart
          xAxis={[{ scaleType: 'point', data: t.labels }]}
          series={[
            { data: t.plcNormal, label: '정상', color: '#52c41a', area: true, stack: 'plc', showMark: false },
            { data: t.plcIssue, label: '이상', color: '#ff4d4f', area: true, stack: 'plc', showMark: false },
            { data: t.plcMissing, label: '데이터 누락', color: '#faad14', area: true, stack: 'plc', showMark: false }
          ]}
          height={300}
          margin={{ top: 24, right: 16, bottom: 40, left: 44 }}
        />
        <Button size="small" variant="outlined" onClick={() => onOpenSignals()} sx={{ alignSelf: 'flex-start' }}>
          PLC 이벤트 전체 보기
        </Button>
      </Stack>
    </MainCard>
  );
}

export function TransferProductionTrendPanel({ dashboard, onOpenTransfers }) {
  const t = dashboard.trend;
  if (!t || t.labels.length === 0) return null;

  return (
    <MainCard>
      <Stack spacing={2}>
        <SectionTitle title="이송·생산 완료 추이" caption="일자별 이송 완료·실패와 작업 완료 건수를 선으로 비교합니다." />
        <LineChart
          xAxis={[{ scaleType: 'point', data: t.labels }]}
          series={[
            { data: t.transferDone, label: '이송 완료', color: '#1677ff', curve: 'monotoneX' },
            { data: t.transferFailed, label: '이송 실패', color: '#ff4d4f', curve: 'monotoneX' },
            { data: t.workCompleted, label: '작업 완료', color: '#52c41a', curve: 'monotoneX' }
          ]}
          height={300}
          margin={{ top: 24, right: 16, bottom: 40, left: 44 }}
        />
        <Button size="small" variant="outlined" onClick={() => onOpenTransfers()} sx={{ alignSelf: 'flex-start' }}>
          자재 이동 전체 보기
        </Button>
      </Stack>
    </MainCard>
  );
}

export function OperationFlowPanel({ dashboard, onOpenWorkOrders, onOpenTransfers, onOpenSignals }) {
  return (
    <MainCard>
      <Stack spacing={2}>
        <SectionTitle title="생산·이동·PLC 흐름" caption="생산 대기, 자재 이동, PLC 신호 문제를 보조 지표로 확인합니다." />
        <BarChart
          xAxis={[{ scaleType: 'band', data: ['작업 대기', '작업 진행', '이동 대기/진행', '이동 실패', 'PLC 문제'] }]}
          series={[{
            data: [
              dashboard.waitingWorkOrderCount,
              dashboard.inProgressWorkOrderCount,
              dashboard.movingTransferCount,
              dashboard.failedTransferCount,
              dashboard.plcIssueCount
            ],
            label: '운영 흐름',
            color: '#722ed1'
          }]}
          height={300}
          margin={{ top: 24, right: 16, bottom: 40, left: 44 }}
          onItemClick={(_, item) => {
            if (item.dataIndex <= 1) onOpenWorkOrders();
            else if (item.dataIndex <= 3) onOpenTransfers();
            else onOpenSignals();
          }}
        />
        <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', rowGap: 1 }}>
          <Chip label={`작업 대기 ${dashboard.waitingWorkOrderCount}`} size="small" variant="light" />
          <Chip label={`이동 실패 ${dashboard.failedTransferCount}`} size="small" color={dashboard.failedTransferCount > 0 ? 'error' : 'success'} variant="light" />
          <Chip label={`PLC 문제 ${dashboard.plcIssueCount}`} size="small" color={dashboard.plcIssueCount > 0 ? 'warning' : 'success'} variant="light" />
        </Stack>
      </Stack>
    </MainCard>
  );
}

export function QualityPriorityTable({ defects, failedInspections }) {
  return (
    <MainCard title="품질 우선 확인 항목" content={false}>
      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>구분</TableCell>
              <TableCell>대상</TableCell>
              <TableCell align="right">수량</TableCell>
              <TableCell>원인/판정</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {defects.slice(0, 5).map((defect) => (
              <TableRow key={`defect-${defect.defectId}`} hover>
                <TableCell><Chip label="불량" size="small" color="warning" variant="light" /></TableCell>
                <TableCell>{defect.defectNm || defect.defectCd || '-'}</TableCell>
                <TableCell align="right">{toNumber(defect.defectQty)}</TableCell>
                <TableCell>{defect.defectCause || defect.defectAction || '-'}</TableCell>
              </TableRow>
            ))}
            {failedInspections.slice(0, 5).map((inspect) => (
              <TableRow key={`inspect-${inspect.inspectId}`} hover>
                <TableCell><Chip label="검사" size="small" color="error" variant="light" /></TableCell>
                <TableCell>{inspect.inspectNo || inspect.itemCd || '-'}</TableCell>
                <TableCell align="right">{toNumber(inspect.failQty)}</TableCell>
                <TableCell>{inspect.judgeResult || '부적합'}</TableCell>
              </TableRow>
            ))}
            {defects.length === 0 && failedInspections.length === 0 && <EmptyRows colSpan={4} />}
          </TableBody>
        </Table>
      </TableContainer>
    </MainCard>
  );
}

export function StockEquipmentPriorityTable({ stocks, equipmentStatuses }) {
  const lowStocks = stocks.filter((stock) => getStockStatus(stock) !== '정상');
  const issueEquipment = equipmentStatuses.filter((status) => getEquipmentStatus(status) !== '가동');

  return (
    <MainCard title="재고·설비 우선 확인 항목" content={false}>
      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>구분</TableCell>
              <TableCell>대상</TableCell>
              <TableCell>상태</TableCell>
              <TableCell>확인 방향</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {lowStocks.slice(0, 5).map((stock) => (
              <TableRow key={`stock-${stock.stockId || `${stock.itemCd}-${stock.locationCd}`}`} hover>
                <TableCell><Chip label="재고" size="small" color="warning" variant="light" /></TableCell>
                <TableCell>{stock.itemNm || stock.itemCd || '-'}</TableCell>
                <TableCell>{getStockStatus(stock)}</TableCell>
                <TableCell>MES 재고와 MCS 로케이션 재고를 함께 확인</TableCell>
              </TableRow>
            ))}
            {issueEquipment.slice(0, 5).map((status) => (
              <TableRow key={`equipment-${status.operId || status.equipmentCd}`} hover>
                <TableCell><Chip label="설비" size="small" color="error" variant="light" /></TableCell>
                <TableCell>{status.equipmentCd || '-'}</TableCell>
                <TableCell>{status.operStatus || getEquipmentStatus(status)}</TableCell>
                <TableCell>설비 현황과 PLC 이벤트를 함께 확인</TableCell>
              </TableRow>
            ))}
            {lowStocks.length === 0 && issueEquipment.length === 0 && <EmptyRows colSpan={4} />}
          </TableBody>
        </Table>
      </TableContainer>
    </MainCard>
  );
}
