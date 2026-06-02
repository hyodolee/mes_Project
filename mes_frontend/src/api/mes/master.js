import { createQueryString } from '../httpClient';
import { mesApi } from './client';

export const mesMasterApi = {
  plants: (params) => mesApi.get(`/api/v1/master/plants${createQueryString(params)}`),
  plant: (plantCd) => mesApi.get(`/api/v1/master/plants/${plantCd}`),
  createPlant: (payload) => mesApi.post('/api/v1/master/plants', payload),
  updatePlant: (plantCd, payload) => mesApi.put(`/api/v1/master/plants/${plantCd}`, payload),
  items: (params) => mesApi.get(`/api/v1/master/items${createQueryString(params)}`)
};
