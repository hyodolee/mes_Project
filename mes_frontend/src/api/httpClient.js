const contentType = 'application/json; charset=utf-8';

async function request(baseUrl, path, options = {}) {
  const token = options.getToken?.();
  const { getToken, ...requestOptions } = options;
  const response = await fetch(`${baseUrl}${path}`, {
    headers: {
      'Content-Type': contentType,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(requestOptions.headers || {})
    },
    ...requestOptions
  });

  const text = await response.text();
  const data = text ? JSON.parse(text) : null;

  if (!response.ok) {
    if (response.status === 401 && !path.startsWith('/api/auth/')) {
      window.dispatchEvent(new CustomEvent('app:unauthorized'));
    }
    const message = data?.message || `HTTP ${response.status}`;
    throw new Error(message);
  }

  if (data && data.success === false) {
    throw new Error(data.message || data.code || 'Request failed');
  }

  return data;
}

export function createQueryString(params = {}) {
  const query = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query.append(key, value);
    }
  });

  const value = query.toString();
  return value ? `?${value}` : '';
}

export function createHttpClient(baseUrl, options = {}) {
  const withAuth = (requestOptions = {}) => ({ ...requestOptions, getToken: options.getToken });

  return {
    get: (path, requestOptions) => request(baseUrl, path, withAuth({ ...requestOptions, method: 'GET' })),
    post: (path, body, requestOptions) => request(baseUrl, path, withAuth({ ...requestOptions, method: 'POST', body: JSON.stringify(body) })),
    put: (path, body, requestOptions) => request(baseUrl, path, withAuth({ ...requestOptions, method: 'PUT', body: JSON.stringify(body) })),
    patch: (path, body, requestOptions) => request(baseUrl, path, withAuth({ ...requestOptions, method: 'PATCH', body: JSON.stringify(body) })),
    delete: (path, requestOptions) => request(baseUrl, path, withAuth({ ...requestOptions, method: 'DELETE' }))
  };
}
