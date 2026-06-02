import { createQueryString } from '../httpClient';
import { mcsApi } from './client';

export const inventoryApi = {
  stocks: (params) => mcsApi.get(`/api/inventory/stocks${createQueryString(params)}`),
  stock: (locStockId) => mcsApi.get(`/api/inventory/stocks/${locStockId}`),
  adjustStock: (locStockId, payload) => mcsApi.post(`/api/inventory/stocks/${locStockId}/adjust`, payload),
  transactions: (params) => mcsApi.get(`/api/inventory/transactions${createQueryString(params)}`)
};
