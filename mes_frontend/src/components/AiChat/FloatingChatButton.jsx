import Box from '@mui/material/Box';
import Fab from '@mui/material/Fab';
import Tooltip from '@mui/material/Tooltip';
import Zoom from '@mui/material/Zoom';

import { RobotOutlined } from '@ant-design/icons';

import { useChatStore } from 'stores/chatStore';
import ChatDrawer from './ChatDrawer';

export default function FloatingChatButton() {
  const { open, toggleOpen } = useChatStore();

  return (
    <>
      <ChatDrawer />
      <Zoom in={!open}>
        <Tooltip title="AI 운영 도우미" placement="left">
          <Box
            sx={{
              position: 'fixed',
              bottom: 32,
              right: 32,
              zIndex: (theme) => theme.zIndex.drawer - 1
            }}
          >
            <Fab
              color="primary"
              onClick={toggleOpen}
              sx={{
                width: 52,
                height: 52,
                boxShadow: 4,
                '&:hover': { transform: 'scale(1.08)' },
                transition: 'transform 0.15s'
              }}
            >
              <RobotOutlined style={{ fontSize: 22 }} />
            </Fab>
          </Box>
        </Tooltip>
      </Zoom>
    </>
  );
}
