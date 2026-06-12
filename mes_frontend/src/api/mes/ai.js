import { mesApi } from './client';
import { getMesApiBaseUrl } from '../apiBaseUrl';
import { authTokenStore } from '../authTokenStore';

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

export const aiApi = {
  getSummary: (refresh = false) => mesApi.get(`/api/v1/ai/operations/summary${refresh ? '?refresh=true' : ''}`),
  query: (question, pageContext, history = [], conversationId = '') =>
    mesApi.post('/api/v1/ai/query', { question, conversationId, pageContext, history }),
  streamQuery: async (question, pageContext, history = [], conversationId = '', handlers = {}) => {
    const base = getMesApiBaseUrl();
    const token = authTokenStore.getMesToken();
    const response = await fetch(`${base}/api/v1/ai/query/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json; charset=utf-8',
        Accept: 'text/event-stream',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: JSON.stringify({ question, conversationId, pageContext, history })
    });

    if (!response.ok) {
      if (response.status === 401) {
        window.dispatchEvent(new CustomEvent('app:unauthorized'));
      }
      throw new Error(`HTTP ${response.status}`);
    }
    if (!response.body) {
      throw new Error('Streaming response is not available.');
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
      dispatchSseEvent(buffer, handlers);
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
