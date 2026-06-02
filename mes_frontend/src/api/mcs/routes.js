import { createQueryString } from '../httpClient';
import { mcsApi } from './client';

export const routeApi = {
  nodes: (params) => mcsApi.get(`/api/route-nodes${createQueryString(params)}`),
  edges: (params) => mcsApi.get(`/api/route-edges${createQueryString(params)}`),
  changeEdgeStatus: (routeEdgeId, status) => mcsApi.patch(`/api/route-edges/${routeEdgeId}/status${createQueryString({ status })}`, {}),
  optimize: (payload) => mcsApi.post('/api/routes/optimize', payload),
  transferRoute: (transferId) => mcsApi.get(`/api/transfers/${transferId}/routes`),
  createTransferRoute: (transferId, optimizeRule = 'SHORTEST_TIME') =>
    mcsApi.post(`/api/transfers/${transferId}/routes${createQueryString({ optimizeRule })}`, {}),
  replanTransferRoute: (transferId, optimizeRule = 'AVOID_CONGESTION') =>
    mcsApi.post(`/api/transfers/${transferId}/routes/replan${createQueryString({ optimizeRule })}`, {})
};
