import { createQueryString } from '../httpClient';
import { mesApi } from './client';

export const mesInventoryApi = {
  stocks: (params) => mesApi.get(`/api/v1/inventory/stocks${createQueryString(params)}`),
  transactions: (params) => mesApi.get(`/api/v1/inventory/trans-histories${createQueryString(params)}`),
  createTransaction: (payload) => mesApi.post('/api/v1/inventory/trans', payload)
};
