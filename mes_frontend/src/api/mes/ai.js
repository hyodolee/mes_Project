import { mesApi } from './client';
import { getMesApiBaseUrl } from '../apiBaseUrl';
import { authTokenStore } from '../authTokenStore';

export class AiStreamError extends Error {
  constructor(message, { status, code } = {}) {
    super(message);
    this.name = 'AiStreamError';
    this.status = status;
    this.code = code;
  }
}

function parseSseBlock(block) {
  const eventLines = block.split(/\r?\n/);
  let eventName = 'message';
  const dataLines = [];

  eventLines.forEach((line) => {
    if (line.startsWith('event:')) {
      eventName = line.slice('event:'.length).trim();
      return;
    }
    if (line.startsWith('data:')) {
      dataLines.push(line.slice('data:'.length));
    }
  });

  return { eventName, data: dataLines.join('\n') };
}

function parseJsonOrText(value) {
  if (!value) return value;
  try {
    return JSON.parse(value);
  } catch {
    return value;
  }
}

function dispatchSseEvent(block, handlers) {
  if (!block.trim()) return null;
  const { eventName, data } = parseSseBlock(block);
  const parsed = eventName === 'token' ? data : parseJsonOrText(data);

  if (eventName === 'token') handlers.onToken?.(parsed);
  if (eventName === 'data-points') handlers.onDataPoints?.(Array.isArray(parsed) ? parsed : []);
  if (eventName === 'done') handlers.onDone?.(parsed);
  if (eventName === 'error') handlers.onError?.(parsed);

  return eventName;
}

async function parseApiResponse(response) {
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;

  if (!response.ok) {
    if (response.status === 401) {
      window.dispatchEvent(new CustomEvent('app:unauthorized'));
    }
    throw new Error(data?.message || `HTTP ${response.status}`);
  }

  if (data?.success === false) {
    throw new Error(data.message || data.code || 'Request failed');
  }

  return data;
}

export const aiApi = {
  getSummary: (refresh = false) => mesApi.get(`/api/v1/ai/operations/summary${refresh ? '?refresh=true' : ''}`),
  query: (question, pageContext, history = [], conversationId = '') =>
    mesApi.post('/api/v1/ai/query', { question, conversationId, pageContext, history }),
  getRagDocuments: () => mesApi.get('/api/v1/ai/rag/documents'),
  uploadRagDocument: async ({ file, documentCategory, documentType, tags }) => {
    const base = getMesApiBaseUrl();
    const token = authTokenStore.getMesToken();
    const formData = new FormData();
    formData.append('file', file);
    formData.append('documentCategory', documentCategory);
    formData.append('documentType', documentType);
    formData.append('tags', tags || '');

    const response = await fetch(`${base}/api/v1/ai/rag/documents`, {
      method: 'POST',
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: formData
    });

    return parseApiResponse(response);
  },
  reindexRagDocument: (documentId) => mesApi.post(`/api/v1/ai/rag/documents/${documentId}/reindex`, {}),
  reindexAllRagDocuments: () => mesApi.post('/api/v1/ai/rag/documents/reindex-all', {}),
  deleteRagDocument: (documentId) => mesApi.delete(`/api/v1/ai/rag/documents/${documentId}`),
  streamQuery: async (question, pageContext, history = [], conversationId = '', handlers = {}) => {
    const base = getMesApiBaseUrl();
    const token = authTokenStore.getMesToken();
    let response;

    try {
      response = await fetch(`${base}/api/v1/ai/query/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json; charset=utf-8',
          Accept: 'text/event-stream',
          ...(token ? { Authorization: `Bearer ${token}` } : {})
        },
        body: JSON.stringify({ question, conversationId, pageContext, history })
      });
    } catch {
      throw new AiStreamError('MES 서버에 접속할 수 없습니다. 백엔드가 실행 중인지 확인해 주세요.', {
        code: 'NETWORK_ERROR'
      });
    }

    if (!response.ok) {
      if (response.status === 401) {
        window.dispatchEvent(new CustomEvent('app:unauthorized'));
        throw new AiStreamError('로그인 정보가 만료되었습니다. 다시 로그인한 뒤 질문해 주세요.', {
          status: response.status,
          code: 'UNAUTHORIZED'
        });
      }

      let detail = '';
      try {
        detail = await response.text();
      } catch {
        detail = '';
      }

      throw new AiStreamError(
        detail || `AI 요청 처리 중 서버 오류가 발생했습니다. HTTP ${response.status}`,
        {
          status: response.status,
          code: 'HTTP_ERROR'
        }
      );
    }
    if (!response.body) {
      throw new AiStreamError('브라우저에서 스트리밍 응답을 읽을 수 없습니다.', {
        code: 'STREAM_UNAVAILABLE'
      });
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');
    let buffer = '';
    let doneReceived = false;

    while (!doneReceived) {
      const { value, done } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const blocks = buffer.split(/\r?\n\r?\n/);
      buffer = blocks.pop() || '';

      for (const block of blocks) {
        const eventName = dispatchSseEvent(block, handlers);
        if (eventName === 'done') {
          doneReceived = true;
          break;
        }
      }
    }

    if (doneReceived) {
      try {
        await reader.cancel();
      } catch {
        // 이미 닫힌 스트림이면 별도 처리가 필요 없습니다.
      }
      return;
    }

    if (buffer.trim()) {
      const eventName = dispatchSseEvent(buffer, handlers);
      if (eventName === 'done') {
        doneReceived = true;
      }
    }

    if (!doneReceived) {
      throw new AiStreamError('AI 응답이 끝나기 전에 연결이 종료되었습니다. 다시 시도해 주세요.', {
        code: 'STREAM_CLOSED'
      });
    }
  },
  clearQueryMemory: (conversationId) =>
    mesApi.post('/api/v1/ai/query/memory/clear', { conversationId }),

  getNotifications: (limit = 20) => mesApi.get('/api/v1/notifications?limit=' + limit),
  getUnreadCount: () => mesApi.get('/api/v1/notifications/unread-count'),
  markAsRead: (id) => mesApi.patch('/api/v1/notifications/' + id + '/read'),
  markAllAsRead: () => mesApi.patch('/api/v1/notifications/read-all'),

  subscribeNotifications: () => {
    const base = getMesApiBaseUrl();
    const token = authTokenStore.getMesToken();
    if (!token) return null;

    // EventSource는 Authorization 헤더를 붙일 수 없어 토큰을 쿼리 파라미터로 전달합니다.
    const tokenQuery = `?token=${encodeURIComponent(token)}`;
    return new EventSource(`${base}/api/v1/notifications/subscribe${tokenQuery}`);
  }
};
