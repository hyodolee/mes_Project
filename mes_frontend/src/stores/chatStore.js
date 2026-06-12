import { create } from 'zustand';

function createConversationId() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return `chat-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

export const useChatStore = create((set, get) => ({
  open: false,
  messages: [],
  loading: false,
  conversationId: createConversationId(),

  toggleOpen: () => set((s) => ({ open: !s.open })),
  setOpen: (open) => set({ open }),

  addUserMessage: (text) => {
    const msg = { id: Date.now(), role: 'user', text };
    set((s) => ({ messages: [...s.messages, msg] }));
    return msg.id;
  },

  addAiMessage: (text, dataPoints = [], aiGenerated = false, model = '') => {
    const msg = { id: Date.now() + 1, role: 'ai', text, dataPoints, aiGenerated, model };
    set((s) => ({ messages: [...s.messages, msg] }));
    return msg.id;
  },

  updateAiMessage: (id, patch) => {
    set((s) => ({
      messages: s.messages.map((message) => (
        message.id === id && message.role === 'ai' ? { ...message, ...patch } : message
      ))
    }));
  },

  appendAiMessageText: (id, token) => {
    if (!token) return;
    set((s) => ({
      messages: s.messages.map((message) => (
        message.id === id && message.role === 'ai'
          ? { ...message, text: `${message.text || ''}${token}` }
          : message
      ))
    }));
  },

  setLoading: (loading) => set({ loading }),

  clearMessages: () => set({ messages: [], conversationId: createConversationId() })
}));
