import { createQueryString } from '../httpClient';
import { mcsApi } from './client';

export const zoneApi = {
  list: (params) => mcsApi.get(`/api/zones${createQueryString(params)}`),
  detail: (zoneId) => mcsApi.get(`/api/zones/${zoneId}`),
  create: (payload) => mcsApi.post('/api/zones', payload),
  update: (zoneId, payload) => mcsApi.put(`/api/zones/${zoneId}`, payload),
  remove: (zoneId) => mcsApi.delete(`/api/zones/${zoneId}`)
};
