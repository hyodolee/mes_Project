function createLocalApiBaseUrl(port) {
  if (typeof window === 'undefined') {
    return `http://localhost:${port}`;
  }

  return `${window.location.protocol}//${window.location.hostname}:${port}`;
}

export function getMesApiBaseUrl() {
  return import.meta.env.VITE_MES_API_BASE_URL || createLocalApiBaseUrl(8080);
}

export function getMcsApiBaseUrl() {
  return import.meta.env.VITE_MCS_API_BASE_URL || createLocalApiBaseUrl(8081);
}
