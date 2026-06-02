import { createQueryString } from '../httpClient';
import { mcsApi } from './client';

export const transferApi = {
  list: (params) => mcsApi.get(`/api/transfers${createQueryString(params)}`),
  detail: (transferId) => mcsApi.get(`/api/transfers/${transferId}`),
  items: (transferId) => mcsApi.get(`/api/transfers/${transferId}/items`),
  create: (payload) => mcsApi.post('/api/transfers', payload),
  update: (transferId, payload) => mcsApi.put(`/api/transfers/${transferId}`, payload),
  changeStatus: (transferId, status) => mcsApi.post(`/api/transfers/${transferId}/status${createQueryString({ status })}`, {}),
  addItem: (transferId, payload) => mcsApi.post(`/api/transfers/${transferId}/items`, payload),
  removeItem: (transferId, transferItemId) => mcsApi.delete(`/api/transfers/${transferId}/items/${transferItemId}`),
  remove: (transferId) => mcsApi.delete(`/api/transfers/${transferId}`)
};
