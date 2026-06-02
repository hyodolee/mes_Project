import { createHttpClient } from '../httpClient';

export const mcsApi = createHttpClient(import.meta.env.VITE_MCS_API_BASE_URL || 'http://localhost:8081');
