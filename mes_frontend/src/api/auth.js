import { mesApi } from './mes/client';
import { mcsApi } from './mcs/client';
import { authTokenStore } from './authTokenStore';

async function safeMe(api) {
  try {
    return await api.get('/api/auth/me');
  } catch {
    return null;
  }
}

export const authApi = {
  me: async () => {
    if (!authTokenStore.hasTokens()) {
      return {
        authenticated: false,
        mes: null,
        mcs: null,
        user: null
      };
    }

    const [mes, mcs] = await Promise.all([safeMe(mesApi), safeMe(mcsApi)]);
    const mesUser = mes?.data;
    const mcsUser = mcs?.data;

    return {
      authenticated: Boolean(mesUser?.authenticated && mcsUser?.authenticated),
      mes: mesUser,
      mcs: mcsUser,
      user: mesUser || mcsUser
    };
  },
  login: async ({ username, password }) => {
    authTokenStore.clear();
    const payload = { username, password };
    const mes = await mesApi.post('/api/auth/login', payload);
    let mcs;
    try {
      mcs = await mcsApi.post('/api/auth/login', payload);
    } catch (error) {
      await mesApi.post('/api/auth/logout', {}).catch(() => {});
      authTokenStore.clear();
      throw error;
    }
    authTokenStore.setTokens({
      mesToken: mes.data?.accessToken,
      mcsToken: mcs.data?.accessToken
    });
    return {
      authenticated: true,
      mes: mes.data,
      mcs: mcs.data,
      user: mes.data
    };
  },
  logout: async () => {
    await Promise.allSettled([
      mesApi.post('/api/auth/logout', {}),
      mcsApi.post('/api/auth/logout', {})
    ]);
    authTokenStore.clear();
  }
};
