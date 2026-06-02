import { createQueryString } from '../httpClient';
import { mesApi } from './client';

export const mesEquipmentApi = {
  workcenters: (params) => mesApi.get(`/api/v1/equipment/workcenters${createQueryString(params)}`),
  options: (params) => mesApi.get(`/api/v1/equipment/options${createQueryString(params)}`),
  workers: (params) => mesApi.get(`/api/v1/equipment/workers${createQueryString(params)}`),
  operStatuses: (params) => mesApi.get(`/api/v1/equipment/oper-statuses${createQueryString(params)}`),
  createOperStatus: (payload) => mesApi.post('/api/v1/equipment/oper-statuses', payload),
  downtimes: (params) => mesApi.get(`/api/v1/equipment/downtimes${createQueryString(params)}`),
  createDowntime: (payload) => mesApi.post('/api/v1/equipment/downtimes', payload),
  maintHistories: (params) => mesApi.get(`/api/v1/equipment/maint-histories${createQueryString(params)}`),
  createMaintHistory: (payload) => mesApi.post('/api/v1/equipment/maint-histories', payload)
};
