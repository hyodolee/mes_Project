import { mesApi } from './mes/client';
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

    const mes = await safeMe(mesApi);
    const mesUser = mes?.data;

    return {
      authenticated: Boolean(mesUser?.authenticated),
      mes: mesUser,
      mcs: mesUser,
      user: mesUser
    };
  },
  login: async ({ username, password }) => {
    authTokenStore.clear();
    const payload = { username, password };
    const mes = await mesApi.post('/api/auth/login', payload);
    authTokenStore.setTokens({
      mesToken: mes.data?.accessToken
    });
    return {
      authenticated: true,
      mes: mes.data,
      mcs: mes.data,
      user: mes.data
    };
  },
  logout: async () => {
    await mesApi.post('/api/auth/logout', {}).catch(() => {});
    authTokenStore.clear();
  }
};
