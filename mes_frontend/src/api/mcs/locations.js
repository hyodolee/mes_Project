import { createQueryString } from '../httpClient';
import { mcsApi } from './client';

export const locationApi = {
  list: (params) => mcsApi.get(`/api/locations${createQueryString(params)}`),
  detail: (locationId) => mcsApi.get(`/api/locations/${locationId}`),
  create: (payload) => mcsApi.post('/api/locations', payload),
  update: (locationId, payload) => mcsApi.put(`/api/locations/${locationId}`, payload),
  remove: (locationId) => mcsApi.delete(`/api/locations/${locationId}`)
};
