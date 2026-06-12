import { useEffect, useRef, useState, useCallback } from 'react';

import useMediaQuery from '@mui/material/useMediaQuery';
import Avatar from '@mui/material/Avatar';
import Badge from '@mui/material/Badge';
import Box from '@mui/material/Box';
import ClickAwayListener from '@mui/material/ClickAwayListener';
import Divider from '@mui/material/Divider';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemAvatar from '@mui/material/ListItemAvatar';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemText from '@mui/material/ListItemText';
import Paper from '@mui/material/Paper';
import Popper from '@mui/material/Popper';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';

import MainCard from 'components/MainCard';
import IconButton from 'components/@extended/IconButton';
import Transitions from 'components/@extended/Transitions';

import BellOutlined from '@ant-design/icons/BellOutlined';
import CheckCircleOutlined from '@ant-design/icons/CheckCircleOutlined';
import WarningOutlined from '@ant-design/icons/WarningOutlined';
import InfoCircleOutlined from '@ant-design/icons/InfoCircleOutlined';
import CloseCircleOutlined from '@ant-design/icons/CloseCircleOutlined';

import { aiApi } from 'api/mes/ai';
import { useAuthStore } from 'stores/authStore';

const severityConfig = {
  ERROR:   { icon: <CloseCircleOutlined />,  color: 'error.main',   bg: 'error.lighter' },
  WARNING: { icon: <WarningOutlined />,      color: 'warning.main', bg: 'warning.lighter' },
  INFO:    { icon: <InfoCircleOutlined />,   color: 'info.main',    bg: 'info.lighter' }
};

function timeAgo(createdAt) {
  if (!createdAt) return '';
  const diff = Math.floor((Date.now() - new Date(createdAt)) / 1000);
  if (diff < 60) return '방금 전';
  if (diff < 3600) return Math.floor(diff / 60) + '분 전';
  if (diff < 86400) return Math.floor(diff / 3600) + '시간 전';
  return Math.floor(diff / 86400) + '일 전';
}

export default function Notification() {
  const authenticated = useAuthStore((state) => state.authenticated);
  const authLoading = useAuthStore((state) => state.loading);
  const downMD = useMediaQuery((theme) => theme.breakpoints.down('md'));
  const anchorRef = useRef(null);
  const eventSourceRef = useRef(null);
  const reconnectTimerRef = useRef(null);
  const reconnectDelayRef = useRef(5000);
  const openRef = useRef(false);

  const [open, setOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    openRef.current = open;
  }, [open]);

  const fetchNotifications = useCallback(async () => {
    if (!authenticated) return;
    try {
      const res = await aiApi.getNotifications(20);
      setNotifications(res?.data || []);
    } catch {
      // 미연결 시 무시
    }
  }, [authenticated]);

  const fetchUnreadCount = useCallback(async () => {
    if (!authenticated) return;
    try {
      const res = await aiApi.getUnreadCount();
      setUnreadCount(res?.data?.count ?? 0);
    } catch {
      // 무시
    }
  }, [authenticated]);

  useEffect(() => {
    let disposed = false;

    const clearReconnectTimer = () => {
      if (reconnectTimerRef.current) {
        clearTimeout(reconnectTimerRef.current);
        reconnectTimerRef.current = null;
      }
    };

    const MIN_DELAY = 5000;
    const MAX_DELAY = 60000;

    const connect = () => {
      if (disposed) return;

      const es = aiApi.subscribeNotifications();
      if (!es) return;
      eventSourceRef.current = es;

      // 연결 성공 시 백오프 간격 초기화
      es.onopen = () => {
        reconnectDelayRef.current = MIN_DELAY;
      };

      es.addEventListener('notification', () => {
        fetchUnreadCount();
        if (openRef.current) fetchNotifications();
      });

      es.onerror = () => {
        es.close();
        if (eventSourceRef.current === es) eventSourceRef.current = null;
        clearReconnectTimer();
        // 지수 백오프: 5s → 10s → 20s → 40s → 60s(상한)
        const delay = reconnectDelayRef.current;
        reconnectTimerRef.current = setTimeout(connect, delay);
        reconnectDelayRef.current = Math.min(delay * 2, MAX_DELAY);
      };
    };

    if (!authLoading && authenticated) {
      connect();
      fetchUnreadCount();
    }

    return () => {
      disposed = true;
      clearReconnectTimer();
      eventSourceRef.current?.close();
      eventSourceRef.current = null;
    };
  }, [authenticated, authLoading, fetchNotifications, fetchUnreadCount]);

  // 드롭다운 열 때 목록 갱신
  useEffect(() => {
    if (open && authenticated) fetchNotifications();
  }, [authenticated, open, fetchNotifications]);

  const handleToggle = () => setOpen((prev) => !prev);

  const handleClose = (event) => {
    if (anchorRef.current && anchorRef.current.contains(event.target)) return;
    setOpen(false);
  };

  const handleMarkAsRead = async (id) => {
    try {
      await aiApi.markAsRead(id);
      setNotifications((prev) => prev.map((n) => n.id === id ? { ...n, read: true } : n));
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch {
      // 무시
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await aiApi.markAllAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch {
      // 무시
    }
  };

  return (
    <Box sx={{ flexShrink: 0, ml: 0.75 }}>
      <IconButton
        color="secondary"
        variant="light"
        sx={{ color: 'text.primary', bgcolor: open ? 'grey.100' : 'transparent' }}
        aria-label="알림"
        ref={anchorRef}
        aria-controls={open ? 'notification-grow' : undefined}
        aria-haspopup="true"
        onClick={handleToggle}
      >
        <Badge badgeContent={unreadCount > 0 ? unreadCount : null} color="error">
          <BellOutlined />
        </Badge>
      </IconButton>

      <Popper
        placement={downMD ? 'bottom' : 'bottom-end'}
        open={open}
        anchorEl={anchorRef.current}
        role={undefined}
        transition
        disablePortal
        popperOptions={{ modifiers: [{ name: 'offset', options: { offset: [downMD ? -5 : 0, 9] } }] }}
      >
        {({ TransitionProps }) => (
          <Transitions type="grow" position={downMD ? 'top' : 'top-right'} in={open} {...TransitionProps}>
            <Paper sx={(theme) => ({ boxShadow: theme.customShadows.z1, width: '100%', minWidth: 300, maxWidth: { xs: 300, md: 400 } })}>
              <ClickAwayListener onClickAway={handleClose}>
                <MainCard
                  title="AI 설비 알림"
                  elevation={0}
                  border={false}
                  content={false}
                  secondary={
                    unreadCount > 0 && (
                      <Tooltip title="모두 읽음 처리">
                        <IconButton color="success" size="small" onClick={handleMarkAllAsRead}>
                          <CheckCircleOutlined style={{ fontSize: '1.15rem' }} />
                        </IconButton>
                      </Tooltip>
                    )
                  }
                >
                  <List component="nav" sx={{ p: 0 }}>
                    {notifications.length === 0 ? (
                      <ListItem sx={{ py: 3, justifyContent: 'center' }}>
                        <Typography variant="body2" color="text.secondary">
                          알림이 없습니다
                        </Typography>
                      </ListItem>
                    ) : (
                      notifications.map((n, idx) => {
                        const cfg = severityConfig[n.severity] || severityConfig.INFO;
                        return (
                          <Box key={n.id}>
                            <ListItem
                              component={ListItemButton}
                              selected={!n.read}
                              onClick={() => !n.read && handleMarkAsRead(n.id)}
                              secondaryAction={
                                <Typography variant="caption" color="text.secondary" sx={{ whiteSpace: 'nowrap' }}>
                                  {timeAgo(n.createdAt)}
                                </Typography>
                              }
                              sx={{
                                py: 1,
                                px: 2,
                                alignItems: 'flex-start',
                                bgcolor: n.read ? 'transparent' : 'action.hover',
                                '& .MuiListItemSecondaryAction-root': { top: 12, transform: 'none' }
                              }}
                            >
                              <ListItemAvatar sx={{ minWidth: 44 }}>
                                <Avatar sx={{ width: 34, height: 34, color: cfg.color, bgcolor: cfg.bg, fontSize: '1rem' }}>
                                  {cfg.icon}
                                </Avatar>
                              </ListItemAvatar>
                              <ListItemText
                                primary={
                                  <Typography variant="subtitle2" sx={{ fontWeight: n.read ? 400 : 600, pr: 5 }}>
                                    {n.title}
                                  </Typography>
                                }
                                secondary={
                                  <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.25 }}>
                                    {n.message}
                                  </Typography>
                                }
                              />
                            </ListItem>
                            {idx < notifications.length - 1 && <Divider />}
                          </Box>
                        );
                      })
                    )}
                  </List>
                </MainCard>
              </ClickAwayListener>
            </Paper>
          </Transitions>
        )}
      </Popper>
    </Box>
  );
}
