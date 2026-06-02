import { createQueryString } from '../httpClient';
import { mcsApi } from './client';

export const mcsReferenceApi = {
  plants: () => mcsApi.get('/api/references/plants'),
  warehouses: (params) => mcsApi.get(`/api/references/warehouses${createQueryString(params)}`),
  vendors: (params) => mcsApi.get(`/api/references/vendors${createQueryString(params)}`),
  items: (params) => mcsApi.get(`/api/references/items${createQueryString(params)}`),
  codes: (grpCd, params) => mcsApi.get(`/api/references/codes/${grpCd}${createQueryString(params)}`)
};
