import PropTypes from 'prop-types';
import { useEffect, useMemo, useState } from 'react';
import { matchPath, useLocation } from 'react-router-dom';

import Collapse from '@mui/material/Collapse';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Typography from '@mui/material/Typography';

import { DownOutlined, RightOutlined } from '@ant-design/icons';

import { handlerDrawerOpen, useGetMenuMaster } from 'api/menu';
import NavItem from './NavItem';

function hasActiveChild(children = [], pathname) {
  return children.some((child) => {
    if (child.type === 'collapse') return hasActiveChild(child.children, pathname);
    return !!matchPath({ path: child.url, end: false }, pathname);
  });
}

export default function NavCollapse({ item, level }) {
  const { menuMaster } = useGetMenuMaster();
  const drawerOpen = menuMaster.isDashboardDrawerOpened;
  const { pathname } = useLocation();
  const selected = useMemo(() => hasActiveChild(item.children, pathname), [item.children, pathname]);
  const [open, setOpen] = useState(selected);

  useEffect(() => {
    if (selected) setOpen(true);
  }, [selected]);

  const Icon = item.icon;
  const itemIcon = Icon ? <Icon style={{ fontSize: drawerOpen ? '1rem' : '1.25rem' }} /> : null;
  const ArrowIcon = open ? DownOutlined : RightOutlined;

  const handleClick = () => {
    if (!drawerOpen) handlerDrawerOpen(true);
    setOpen((current) => !current);
  };

  return (
    <>
      <ListItemButton
        onClick={handleClick}
        selected={selected}
        sx={(theme) => ({
          zIndex: 1201,
          pl: drawerOpen ? `${level * 28}px` : 1.5,
          py: !drawerOpen && level === 1 ? 1.25 : 1,
          ...(drawerOpen && {
            '&:hover': { bgcolor: 'primary.lighter' },
            '&.Mui-selected': {
              bgcolor: 'primary.lighter',
              borderRight: '2px solid',
              borderColor: 'primary.main',
              color: 'primary.main',
              '&:hover': { color: 'primary.main', bgcolor: 'primary.lighter' }
            }
          }),
          ...(!drawerOpen && {
            '&:hover': { bgcolor: 'transparent' },
            '&.Mui-selected': { '&:hover': { bgcolor: 'transparent' }, bgcolor: 'transparent' }
          })
        })}
      >
        {itemIcon && (
          <ListItemIcon
            sx={{
              minWidth: 28,
              color: selected ? 'primary.main' : 'text.primary',
              ...(!drawerOpen && {
                borderRadius: 1.5,
                width: 36,
                height: 36,
                alignItems: 'center',
                justifyContent: 'center',
                bgcolor: selected ? 'primary.lighter' : 'transparent',
                '&:hover': { bgcolor: selected ? 'primary.lighter' : 'secondary.lighter' }
              })
            }}
          >
            {itemIcon}
          </ListItemIcon>
        )}
        {drawerOpen && (
          <>
            <ListItemText
              primary={
                <Typography variant="h6" sx={{ color: selected ? 'primary.main' : 'text.primary' }}>
                  {item.title}
                </Typography>
              }
            />
            <ArrowIcon style={{ fontSize: '0.75rem' }} />
          </>
        )}
      </ListItemButton>

      <Collapse in={drawerOpen && open} timeout="auto" unmountOnExit>
        <List component="div" disablePadding>
          {item.children?.map((child) => {
            if (child.type === 'collapse') return <NavCollapse key={child.id} item={child} level={level + 1} />;
            return <NavItem key={child.id} item={child} level={level + 1} />;
          })}
        </List>
      </Collapse>
    </>
  );
}

NavCollapse.propTypes = {
  item: PropTypes.object,
  level: PropTypes.number
};
