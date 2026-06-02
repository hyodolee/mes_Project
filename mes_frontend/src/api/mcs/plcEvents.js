import { createQueryString } from '../httpClient';
import { mcsApi } from './client';

export const plcEventApi = {
  list: (params) => mcsApi.get(`/api/plc/events${createQueryString(params)}`),
  create: (payload) => mcsApi.post('/api/plc/events', payload)
};
