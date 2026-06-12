import { createHttpClient } from '../httpClient';
import { getMesApiBaseUrl } from '../apiBaseUrl';
import { authTokenStore } from '../authTokenStore';

export const mesApi = createHttpClient(getMesApiBaseUrl(), {
  getToken: authTokenStore.getMesToken
});
