import { createQueryString } from '../httpClient';
import { mesApi } from './client';

export const mesPlanningApi = {
  prodPlans: (params) => mesApi.get(`/api/v1/planning/prod-plans${createQueryString(params)}`),
  prodPlan: (planId) => mesApi.get(`/api/v1/planning/prod-plans/${planId}`),
  createProdPlan: (payload) => mesApi.post('/api/v1/planning/prod-plans', payload)
};
