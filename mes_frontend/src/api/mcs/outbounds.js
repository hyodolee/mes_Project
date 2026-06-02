import { createQueryString } from '../httpClient';
import { mcsApi } from './client';

export const outboundApi = {
  list: (params) => mcsApi.get(`/api/outbounds${createQueryString(params)}`),
  detail: (outboundId) => mcsApi.get(`/api/outbounds/${outboundId}`),
  items: (outboundId) => mcsApi.get(`/api/outbounds/${outboundId}/items`),
  create: (payload) => mcsApi.post('/api/outbounds', payload),
  update: (outboundId, payload) => mcsApi.put(`/api/outbounds/${outboundId}`, payload),
  changeStatus: (outboundId, status) => mcsApi.post(`/api/outbounds/${outboundId}/status${createQueryString({ status })}`, {}),
  remove: (outboundId) => mcsApi.delete(`/api/outbounds/${outboundId}`)
};
