import { mesApi } from './client';
import { createQueryString } from '../httpClient';

// BOM(자재명세서) 관리 API
export const bomApi = {
  list: (params) => mesApi.get(`/api/v1/master/boms${createQueryString(params)}`),
  detail: (bomId) => mesApi.get(`/api/v1/master/boms/${bomId}`),
  create: (payload) => mesApi.post('/api/v1/master/boms', payload),
  update: (bomId, payload) => mesApi.put(`/api/v1/master/boms/${bomId}`, payload),
  remove: (bomId) => mesApi.delete(`/api/v1/master/boms/${bomId}`)
};
