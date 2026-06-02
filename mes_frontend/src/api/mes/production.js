import { createQueryString } from '../httpClient';
import { mesApi } from './client';

export const mesProductionApi = {
  workResults: (params) => mesApi.get(`/api/v1/production/work-results${createQueryString(params)}`),
  createWorkResult: (payload) => mesApi.post('/api/v1/production/work-results', payload),
  defectHistories: (params) => mesApi.get(`/api/v1/production/defect-histories${createQueryString(params)}`),
  createDefectHistory: (payload) => mesApi.post('/api/v1/production/defect-histories', payload)
};
