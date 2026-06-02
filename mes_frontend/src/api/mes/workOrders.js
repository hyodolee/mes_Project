import { createQueryString } from '../httpClient';
import { mesApi } from './client';

export const mesWorkOrderApi = {
  list: (params) => mesApi.get(`/api/v1/planning/work-orders${createQueryString(params)}`),
  detail: (woId) => mesApi.get(`/api/v1/planning/work-orders/${woId}`),
  create: (payload) => mesApi.post('/api/v1/planning/work-orders', payload),
  changeStatus: (woId, woStatus) => mesApi.patch(`/api/v1/planning/work-orders/${woId}/status`, { woStatus }),
  requestMaterialTransfer: (woId, payload) => mesApi.post(`/api/v1/planning/work-orders/${woId}/material-transfer`, payload),
  materialTransferStatus: (woId) => mesApi.get(`/api/v1/planning/work-orders/${woId}/material-transfer/status`),
  aiAnalysis: (woId) => mesApi.post(`/api/v1/ai/work-orders/${woId}/analysis`)
};
