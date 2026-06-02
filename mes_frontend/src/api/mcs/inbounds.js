import { createQueryString } from '../httpClient';
import { mcsApi } from './client';

export const inboundApi = {
  list: (params) => mcsApi.get(`/api/inbounds${createQueryString(params)}`),
  detail: (inboundId) => mcsApi.get(`/api/inbounds/${inboundId}`),
  items: (inboundId) => mcsApi.get(`/api/inbounds/${inboundId}/items`),
  create: (payload) => mcsApi.post('/api/inbounds', payload),
  update: (inboundId, payload) => mcsApi.put(`/api/inbounds/${inboundId}`, payload),
  changeStatus: (inboundId, status) => mcsApi.post(`/api/inbounds/${inboundId}/status${createQueryString({ status })}`, {}),
  remove: (inboundId) => mcsApi.delete(`/api/inbounds/${inboundId}`)
};
