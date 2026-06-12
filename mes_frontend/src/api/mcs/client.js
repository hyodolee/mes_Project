import { createHttpClient } from '../httpClient';
import { getMcsApiBaseUrl } from '../apiBaseUrl';
import { authTokenStore } from '../authTokenStore';

export const mcsApi = createHttpClient(getMcsApiBaseUrl(), {
  getToken: authTokenStore.getMcsToken
});
