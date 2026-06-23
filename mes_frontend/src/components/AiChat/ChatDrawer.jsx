import { useCallback, useEffect, useRef, useState } from 'react';
import { useLocation } from 'react-router-dom';

import Avatar from '@mui/material/Avatar';
import Box from '@mui/material/Box';
import Chip from '@mui/material/Chip';
import CircularProgress from '@mui/material/CircularProgress';
import Collapse from '@mui/material/Collapse';
import Divider from '@mui/material/Divider';
import Drawer from '@mui/material/Drawer';
import IconButton from '@mui/material/IconButton';
import InputAdornment from '@mui/material/InputAdornment';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';

import {
  CloseOutlined,
  DeleteOutlined,
  DownOutlined,
  PaperClipOutlined,
  RobotOutlined,
  SendOutlined,
  UserOutlined
} from '@ant-design/icons';

import { AiStreamError, aiApi } from 'api/mes/ai';
import { useChatStore } from 'stores/chatStore';

const MIN_DRAWER_WIDTH = 320;
const MAX_DRAWER_WIDTH = 900;
const DEFAULT_DRAWER_WIDTH = 380;
const DRAWER_WIDTH_STORAGE_KEY = 'aiChatDrawerWidth';

const SUGGESTIONS_BY_PATH = {
  '/mcs/transfers': ['실패한 자재 이동 있어?', '현재 이동 중인 건은?', '완료된 이동 몇 건이야?'],
  '/mcs/plc-events': ['최근 설비 오류 있어?', '데이터 누락 이벤트 있어?', '어떤 설비에서 문제가 났어?'],
  '/mes/work-orders': ['지금 대기 중인 작업은?', '진행 중인 작업 있어?', '완료된 작업 몇 건이야?'],
  '/mcs/location-stock': ['재고 있는 로케이션은?', '재고 현황 알려줘'],
  default: ['지금 공장 상태 어때?', '실패한 자재 이동 있어?', '진행 중인 작업 몇 개야?']
};

function clampDrawerWidth(width) {
  const maxAllowed = Math.min(MAX_DRAWER_WIDTH, typeof window !== 'undefined' ? window.innerWidth : MAX_DRAWER_WIDTH);
  return Math.max(MIN_DRAWER_WIDTH, Math.min(width, maxAllowed));
}

function getInitialDrawerWidth() {
  if (typeof window === 'undefined') return DEFAULT_DRAWER_WIDTH;
  const saved = Number(window.localStorage.getItem(DRAWER_WIDTH_STORAGE_KEY));
  return clampDrawerWidth(Number.isFinite(saved) && saved > 0 ? saved : DEFAULT_DRAWER_WIDTH);
}

function getSuggestions(pathname) {
  const key = Object.keys(SUGGESTIONS_BY_PATH).find((k) => k !== 'default' && pathname.startsWith(k));
  return SUGGESTIONS_BY_PATH[key] || SUGGESTIONS_BY_PATH.default;
}

function formatAiText(text) {
  if (!text) return '';
  return text
    .replace(/\r\n/g, '\n')
    .replace(/([,.!?])(?=[^\s)\]}])/g, '$1 ')
    .replace(/(현황|원인|확인할 화면|조치):/g, '\n$1:')
    .replace(/\n{3,}/g, '\n\n')
    .trim();
}

function getAiErrorMessage(error) {
  if (error instanceof AiStreamError) {
    return error.message;
  }
  return '서버와 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.';
}

function UserBubble({ text }) {
  return (
    <Stack direction="row" spacing={1} sx={{ justifyContent: 'flex-end', alignItems: 'flex-end', width: '100%' }}>
      <Box
        sx={{
          display: 'inline-block',
          maxWidth: 'min(78%, 420px)',
          bgcolor: 'primary.main',
          color: 'primary.contrastText',
          px: 2,
          py: 1,
          borderRadius: '18px 18px 4px 18px',
          wordBreak: 'break-word'
        }}
      >
        <Typography variant="body2">{text}</Typography>
      </Box>
      <Avatar sx={{ width: 28, height: 28, bgcolor: 'primary.dark', flexShrink: 0, alignSelf: 'flex-start', mt: 0.5 }}>
        <UserOutlined style={{ fontSize: 14 }} />
      </Avatar>
    </Stack>
  );
}

function AiBubble({ text, dataPoints, aiGenerated, model }) {
  const [showData, setShowData] = useState(false);
  const displayText = formatAiText(text);

  return (
    <Stack direction="row" spacing={1} sx={{ alignItems: 'flex-end', width: '100%' }}>
      <Avatar sx={{ width: 28, height: 28, bgcolor: 'secondary.main', flexShrink: 0, alignSelf: 'flex-start', mt: 0.5 }}>
        <RobotOutlined style={{ fontSize: 14 }} />
      </Avatar>
      <Box sx={{ minWidth: 0, maxWidth: 'min(82%, 520px)' }}>
        <Box
          sx={{
            display: 'inline-block',
            maxWidth: '100%',
            bgcolor: 'grey.100',
            px: 2,
            py: 1,
            borderRadius: '18px 18px 18px 4px',
            wordBreak: 'keep-all',
            overflowWrap: 'anywhere',
            whiteSpace: 'pre-wrap',
            lineHeight: 1.65
          }}
        >
          {displayText ? (
            <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', lineHeight: 1.65 }}>
              {displayText}
            </Typography>
          ) : (
            <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center', minHeight: 20 }}>
              {[0, 1, 2].map((i) => (
                <Box
                  key={i}
                  sx={{
                    width: 6,
                    height: 6,
                    borderRadius: '50%',
                    bgcolor: 'text.disabled',
                    animation: 'chatDot 1.2s infinite',
                    animationDelay: `${i * 0.2}s`,
                    '@keyframes chatDot': {
                      '0%, 80%, 100%': { opacity: 0.2, transform: 'scale(0.8)' },
                      '40%': { opacity: 1, transform: 'scale(1)' }
                    }
                  }}
                />
              ))}
            </Stack>
          )}
        </Box>
        {(dataPoints?.length > 0 || (aiGenerated && model)) && (
          <Stack direction="row" sx={{ alignItems: 'center', gap: 1, mt: 0.25, px: 0.5, minHeight: 16 }}>
            {dataPoints?.length > 0 && (
              <Box
                onClick={() => setShowData((v) => !v)}
                sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.25, cursor: 'pointer' }}
              >
                <Typography variant="caption" color="text.secondary">
                  근거 데이터
                </Typography>
                <DownOutlined
                  style={{
                    fontSize: 9,
                    color: '#8c8c8c',
                    transform: showData ? 'rotate(180deg)' : 'none',
                    transition: 'transform 0.2s'
                  }}
                />
              </Box>
            )}
            {aiGenerated && model && (
              <Typography variant="caption" color="text.disabled" sx={{ fontSize: 10 }}>
                {model}
              </Typography>
            )}
          </Stack>
        )}
        {dataPoints?.length > 0 && (
          <Collapse in={showData}>
            <Stack spacing={0.25} sx={{ mt: 0.25, px: 0.5 }}>
              {dataPoints.map((point, idx) => (
                <Typography key={idx} variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                  - {point}
                </Typography>
              ))}
            </Stack>
          </Collapse>
        )}
      </Box>
    </Stack>
  );
}

export default function ChatDrawer() {
  const location = useLocation();
  const {
    open,
    setOpen,
    messages,
    loading,
    conversationId,
    addUserMessage,
    addAiMessage,
    appendAiMessageText,
    updateAiMessage,
    setLoading,
    clearMessages
  } = useChatStore();
  const [input, setInput] = useState('');
  const [drawerWidth, setDrawerWidth] = useState(getInitialDrawerWidth);
  const [resizing, setResizing] = useState(false);
  const bottomRef = useRef(null);
  const inputRef = useRef(null);
  const fileInputRef = useRef(null);
  // 전송 대기 중인 첨부 이미지(base64 data URL). 전송하면 서버 보관소로 넘기고 비운다.
  const [attachedImages, setAttachedImages] = useState([]);
  const suggestions = getSuggestions(location.pathname);

  // 오른쪽 Drawer의 왼쪽 모서리를 잡고 폭을 조절합니다.
  const handleResizeStart = useCallback((e) => {
    e.preventDefault();
    setResizing(true);
  }, []);

  useEffect(() => {
    if (!resizing) return undefined;

    const handleMouseMove = (e) => {
      setDrawerWidth(clampDrawerWidth(window.innerWidth - e.clientX));
    };
    const handleMouseUp = () => setResizing(false);

    document.body.style.userSelect = 'none';
    document.body.style.cursor = 'ew-resize';
    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);

    return () => {
      document.body.style.userSelect = '';
      document.body.style.cursor = '';
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [resizing]);

  useEffect(() => {
    window.localStorage.setItem(DRAWER_WIDTH_STORAGE_KEY, String(drawerWidth));
  }, [drawerWidth]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  useEffect(() => {
    if (open && messages.length === 0) {
      addAiMessage('안녕하세요. 운영 AI 도우미입니다.\n무엇이 궁금하신가요?');
    }
    if (open) {
      setTimeout(() => inputRef.current?.focus(), 100);
    }
  }, [open]);

  // 파일 선택 → base64(data URL)로 변환해 최근 5장까지 담는다. (보관·만료는 서버가 처리)
  const handlePickImages = (event) => {
    Array.from(event.target.files || []).forEach((file) => {
      if (!file.type.startsWith('image/')) return;
      const reader = new FileReader();
      reader.onload = () => setAttachedImages((prev) => [...prev, reader.result].slice(-5));
      reader.readAsDataURL(file);
    });
    event.target.value = ''; // 같은 파일을 다시 선택할 수 있도록 초기화
  };

  const removeAttachedImage = (idx) => setAttachedImages((prev) => prev.filter((_, i) => i !== idx));

  const handleSend = async (text) => {
    if (loading) return;
    const imagesToSend = attachedImages;
    const typed = (text || input).trim();
    // 글 없이 이미지만 보냈으면 기본 질문으로 분석을 요청한다.
    const question = typed || (imagesToSend.length > 0 ? '첨부한 이미지를 분석해서 현재 상황과 문제점을 알려줘.' : '');
    if (!question) return;

    setInput('');
    setAttachedImages([]); // 입력창 첨부는 비움 (서버 보관소로 이동)
    const history = messages.slice(-6).map((m) => ({ role: m.role, text: m.text }));
    // 화면 말풍선에는 이미지 첨부 표시를 함께 남긴다.
    const userText = imagesToSend.length > 0 ? `${typed || '(이미지 분석 요청)'}  🖼️×${imagesToSend.length}` : question;
    addUserMessage(userText);
    const aiMessageId = addAiMessage('');
    setLoading(true);
    let receivedToken = false;
    let completed = false;

    try {
      await aiApi.streamQuery(
        question,
        location.pathname,
        history,
        conversationId,
        {
          onToken: (token) => {
            receivedToken = true;
            appendAiMessageText(aiMessageId, token);
          },
          onDataPoints: (dataPoints) => {
            updateAiMessage(aiMessageId, { dataPoints });
          },
          onDone: (data) => {
            completed = true;
            const patch = {};
            if (!receivedToken) {
              patch.text = data?.answer || '응답을 받지 못했습니다. 다시 시도해 주세요.';
            }
            if (Array.isArray(data?.dataPoints)) {
              patch.dataPoints = data.dataPoints;
            }
            if (typeof data?.aiGenerated === 'boolean') {
              patch.aiGenerated = data.aiGenerated;
            }
            if (data?.model) {
              patch.model = data.model;
            }
            updateAiMessage(aiMessageId, patch);
          },
          onError: (message) => {
            const errorMessage = message || '응답 생성 중 연결이 중단되었습니다.';
            if (receivedToken) {
              appendAiMessageText(aiMessageId, `\n\n${errorMessage}`);
              return;
            }
            updateAiMessage(aiMessageId, { text: '응답 생성 중 오류가 발생했습니다. 다시 시도해 주세요.' });
          }
        },
        imagesToSend
      );
    } catch (error) {
      const errorMessage = getAiErrorMessage(error);
      if (!receivedToken && !completed) {
        updateAiMessage(aiMessageId, { text: errorMessage });
      } else if (receivedToken && !completed) {
        appendAiMessageText(aiMessageId, `\n\n${errorMessage}`);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleClearMessages = async () => {
    const currentConversationId = conversationId;
    clearMessages();
    try {
      await aiApi.clearQueryMemory(currentConversationId);
    } catch {
      // 화면 대화는 지워졌으므로, 서버 메모리 초기화 실패는 조용히 넘깁니다.
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <Drawer
      anchor="right"
      open={open}
      onClose={() => setOpen(false)}
      slotProps={{
        paper: {
          sx: {
            width: drawerWidth,
            maxWidth: '100vw',
            display: 'flex',
            flexDirection: 'column',
            height: '100%',
            transition: resizing ? 'none' : 'width 0.15s ease',
            overflow: 'visible'
          }
        }
      }}
    >
      <Box
        onMouseDown={handleResizeStart}
        sx={{
          position: 'absolute',
          top: 0,
          left: 0,
          width: 6,
          height: '100%',
          cursor: 'ew-resize',
          zIndex: (theme) => theme.zIndex.drawer + 2,
          '&:hover': { bgcolor: 'primary.main', opacity: 0.3 },
          ...(resizing && { bgcolor: 'primary.main', opacity: 0.3 })
        }}
      />

      <Stack
        direction="row"
        sx={{
          alignItems: 'center',
          justifyContent: 'space-between',
          px: 2,
          py: 1.5,
          bgcolor: 'primary.main',
          color: 'primary.contrastText',
          flexShrink: 0
        }}
      >
        <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
          <RobotOutlined style={{ fontSize: 18 }} />
          <Box>
            <Typography variant="subtitle1" sx={{ fontWeight: 600, lineHeight: 1.2 }}>
              AI 운영 도우미
            </Typography>
            <Typography variant="caption" sx={{ opacity: 0.8 }}>
              MES/MCS 운영 데이터를 기준으로 답변합니다
            </Typography>
          </Box>
        </Stack>
        <Stack direction="row" spacing={0.5}>
          <Tooltip title="대화 초기화">
            <IconButton size="small" onClick={handleClearMessages} sx={{ color: 'inherit', opacity: 0.8 }}>
              <DeleteOutlined style={{ fontSize: 16 }} />
            </IconButton>
          </Tooltip>
          <IconButton size="small" onClick={() => setOpen(false)} sx={{ color: 'inherit' }}>
            <CloseOutlined style={{ fontSize: 16 }} />
          </IconButton>
        </Stack>
      </Stack>

      <Divider />

      <Box sx={{ flex: 1, overflowY: 'auto', p: 2 }}>
        <Stack spacing={1.5}>
          {messages.map((msg) =>
            msg.role === 'user' ? (
              <UserBubble key={msg.id} text={msg.text} />
            ) : (
              <AiBubble key={msg.id} text={msg.text} dataPoints={msg.dataPoints} aiGenerated={msg.aiGenerated} model={msg.model} />
            )
          )}
          <div ref={bottomRef} />
        </Stack>
      </Box>

      <Box sx={{ px: 2, pb: 1, flexShrink: 0 }}>
        <Stack direction="row" spacing={0.75} sx={{ flexWrap: 'wrap', gap: 0.75 }}>
          {suggestions.map((s) => (
            <Chip
              key={s}
              label={s}
              size="small"
              variant="outlined"
              clickable
              disabled={loading}
              onClick={() => handleSend(s)}
              sx={{ fontSize: 11 }}
            />
          ))}
        </Stack>
      </Box>

      <Box sx={{ px: 2, pb: 2, pt: 0.5, flexShrink: 0 }}>
        {/* 전송 전 첨부 이미지 미리보기 (최대 5장) */}
        {attachedImages.length > 0 && (
          <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 1, mb: 1 }}>
            {attachedImages.map((src, idx) => (
              <Box key={idx} sx={{ position: 'relative' }}>
                <Box
                  component="img"
                  src={src}
                  alt={`첨부 이미지 ${idx + 1}`}
                  sx={{
                    width: 56,
                    height: 56,
                    objectFit: 'cover',
                    borderRadius: 1.5,
                    display: 'block',
                    border: '1px solid',
                    borderColor: 'divider'
                  }}
                />
                <IconButton
                  size="small"
                  onClick={() => removeAttachedImage(idx)}
                  sx={{
                    position: 'absolute',
                    top: -8,
                    right: -8,
                    p: '2px',
                    bgcolor: 'background.paper',
                    boxShadow: 1,
                    '&:hover': { bgcolor: 'grey.200' }
                  }}
                >
                  <CloseOutlined style={{ fontSize: 10 }} />
                </IconButton>
              </Box>
            ))}
          </Stack>
        )}

        {/* 숨겨진 파일 선택기 — 📎 버튼으로 연다 */}
        <input type="file" accept="image/*" multiple hidden ref={fileInputRef} onChange={handlePickImages} />

        <TextField
          inputRef={inputRef}
          fullWidth
          multiline
          maxRows={3}
          placeholder="질문을 입력하세요. Enter로 전송"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          disabled={loading}
          size="small"
          slotProps={{
            input: {
              startAdornment: (
                <InputAdornment position="start">
                  <Tooltip title={attachedImages.length >= 5 ? '이미지는 최대 5장' : '이미지 첨부 (최대 5장)'}>
                    <span>
                      <IconButton
                        size="small"
                        onClick={() => fileInputRef.current?.click()}
                        disabled={loading || attachedImages.length >= 5}
                      >
                        <PaperClipOutlined style={{ fontSize: 16 }} />
                      </IconButton>
                    </span>
                  </Tooltip>
                </InputAdornment>
              ),
              endAdornment: (
                <InputAdornment position="end">
                  {loading ? (
                    <CircularProgress size={18} />
                  ) : (
                    <IconButton
                      size="small"
                      onClick={() => handleSend()}
                      disabled={!input.trim() && attachedImages.length === 0}
                      color="primary"
                    >
                      <SendOutlined style={{ fontSize: 16 }} />
                    </IconButton>
                  )}
                </InputAdornment>
              )
            }
          }}
        />
      </Box>
    </Drawer>
  );
}
