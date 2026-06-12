import { useEffect } from 'react';

import { useAuthStore } from './authStore';

export default function AuthBootstrap() {
  const refresh = useAuthStore((state) => state.refresh);
  const clearAuth = useAuthStore((state) => state.clearAuth);

  useEffect(() => {
    refresh();
  }, [refresh]);

  useEffect(() => {
    window.addEventListener('app:unauthorized', clearAuth);
    return () => window.removeEventListener('app:unauthorized', clearAuth);
  }, [clearAuth]);

  return null;
}
