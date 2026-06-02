import { createQueryString } from '../httpClient';
import { mesApi } from './client';

export const mesQualityApi = {
  inspectResults: (params) => mesApi.get(`/api/v1/quality/inspect-results${createQueryString(params)}`),
  createInspectResult: (payload) => mesApi.post('/api/v1/quality/inspect-results', payload),
  inspectStds: (params) => mesApi.get(`/api/v1/quality/inspect-stds${createQueryString(params)}`)
};
