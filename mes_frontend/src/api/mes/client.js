import { createHttpClient } from '../httpClient';

export const mesApi = createHttpClient(import.meta.env.VITE_MES_API_BASE_URL || 'http://localhost:8080');
