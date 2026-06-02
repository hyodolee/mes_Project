const contentType = 'application/json; charset=utf-8';

async function request(baseUrl, path, options = {}) {
  const response = await fetch(`${baseUrl}${path}`, {
    headers: {
      'Content-Type': contentType,
      ...(options.headers || {})
    },
    ...options
  });

  const text = await response.text();
  const data = text ? JSON.parse(text) : null;

  if (!response.ok) {
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

export function createHttpClient(baseUrl) {
  return {
    get: (path, options) => request(baseUrl, path, { ...options, method: 'GET' }),
    post: (path, body, options) => request(baseUrl, path, { ...options, method: 'POST', body: JSON.stringify(body) }),
    put: (path, body, options) => request(baseUrl, path, { ...options, method: 'PUT', body: JSON.stringify(body) }),
    patch: (path, body, options) => request(baseUrl, path, { ...options, method: 'PATCH', body: JSON.stringify(body) }),
    delete: (path, options) => request(baseUrl, path, { ...options, method: 'DELETE' })
  };
}
