import { useEffect } from 'react';
import { Outlet } from 'react-router-dom';

import useMediaQuery from '@mui/material/useMediaQuery';
import Toolbar from '@mui/material/Toolbar';
import Box from '@mui/material/Box';

// project imports
import Drawer from './Drawer';
import Header from './Header';
import Footer from './Footer';
import Loader from 'components/Loader';
import Breadcrumbs from 'components/@extended/Breadcrumbs';
import ScrollTop from 'components/ScrollTop';

import { handlerDrawerOpen, useGetMenuMaster } from 'api/menu';

// ==============================|| MAIN LAYOUT ||============================== //

export default function DashboardLayout() {
  const { menuMasterLoading } = useGetMenuMaster();
  const downLG = useMediaQuery((theme) => theme.breakpoints.down('lg'));

  // set media wise responsive drawer
  useEffect(() => {
    handlerDrawerOpen(!downLG);
  }, [downLG]);

  if (menuMasterLoading) return <Loader />;

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', width: '100%' }}>
      <ScrollTop />
      <Header />
      <Drawer />

      <Box
        component="main"
        sx={{
          width: { xs: '100%', lg: 'calc(100% - 260px)' },
          minWidth: 0,
          height: '100vh',
          flexGrow: 1,
          overflowX: 'hidden',
          overflowY: 'auto',
          p: { xs: 2, sm: 3 }
        }}
      >
        <Toolbar sx={{ mt: 'inherit' }} />
        <Box
          sx={{
            ...{ px: { xs: 0, sm: 2 } },
            position: 'relative',
            minHeight: 'calc(100vh - 110px)',
            display: 'flex',
            flexDirection: 'column'
          }}
        >
          <Breadcrumbs />
          <Outlet />
          <Footer />
        </Box>
      </Box>
    </Box>
  );
}
