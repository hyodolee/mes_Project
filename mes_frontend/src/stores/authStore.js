import { create } from 'zustand';

import { authApi } from 'api/auth';
import { authTokenStore } from 'api/authTokenStore';

const emptySystems = { mes: null, mcs: null };

export const useAuthStore = create((set) => ({
  user: null,
  systems: emptySystems,
  loading: true,
  authenticated: false,

  refresh: async () => {
    set({ loading: true });
    try {
      const session = await authApi.me();
      set({
        user: session.authenticated ? session.user : null,
        systems: { mes: session.mes, mcs: session.mcs },
        authenticated: Boolean(session.authenticated)
      });
      return session;
    } finally {
      set({ loading: false });
    }
  },

  login: async (credentials) => {
    const session = await authApi.login(credentials);
    set({
      user: session.user,
      systems: { mes: session.mes, mcs: session.mcs },
      authenticated: true,
      loading: false
    });
    return session;
  },

  logout: async () => {
    await authApi.logout();
    set({
      user: null,
      systems: emptySystems,
      authenticated: false,
      loading: false
    });
  },

  clearAuth: () => {
    authTokenStore.clear();
    set({
      user: null,
      systems: emptySystems,
      authenticated: false,
      loading: false
    });
  }
}));
