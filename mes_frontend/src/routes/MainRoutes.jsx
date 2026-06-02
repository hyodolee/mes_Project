import { lazy } from 'react';

// project imports
import Loadable from 'components/Loadable';
import DashboardLayout from 'layout/Dashboard';

// render - operations
const DashboardDefault = Loadable(lazy(() => import('pages/dashboard/default')));
const MesDashboard = Loadable(lazy(() => import('pages/dashboard/mes')));
const McsDashboard = Loadable(lazy(() => import('pages/dashboard/mcs')));
const McsZones = Loadable(lazy(() => import('pages/mcs/zones')));
const McsLocations = Loadable(lazy(() => import('pages/mcs/locations')));
const McsInbounds = Loadable(lazy(() => import('pages/mcs/inbounds')));
const McsOutbounds = Loadable(lazy(() => import('pages/mcs/outbounds')));
const McsTransfers = Loadable(lazy(() => import('pages/mcs/transfers')));
const McsRouteManagement = Loadable(lazy(() => import('pages/mcs/route-management')));
const McsRouteOptimizer = Loadable(lazy(() => import('pages/mcs/route-optimizer')));
const McsLocationStock = Loadable(lazy(() => import('pages/mcs/location-stock')));
const McsInventoryTransactions = Loadable(lazy(() => import('pages/mcs/inventory-transactions')));
const McsPlcEvents = Loadable(lazy(() => import('pages/mcs/plc-events')));
const MesWorkOrders = Loadable(lazy(() => import('pages/mes/work-orders')));
const MesProdPlans = Loadable(lazy(() => import('pages/mes/prod-plans')));
const MesWorkResults = Loadable(lazy(() => import('pages/mes/work-results')));
const MesInventory = Loadable(lazy(() => import('pages/mes/inventory')));
const MesQuality = Loadable(lazy(() => import('pages/mes/quality')));
const MesDefects = Loadable(lazy(() => import('pages/mes/defects')));
const MesEquipment = Loadable(lazy(() => import('pages/mes/equipment')));
const MesMasterData = Loadable(lazy(() => import('pages/mes/master-data')));
const AiOperations = Loadable(lazy(() => import('pages/ai/operations')));

// ==============================|| MAIN ROUTING ||============================== //

const MainRoutes = {
  path: '/',
  element: <DashboardLayout />,
  children: [
    {
      path: '/',
      element: <DashboardDefault />
    },
    {
      path: 'dashboard',
      element: <DashboardDefault />
    },
    {
      path: 'mcs/dashboard',
      element: <McsDashboard />
    },
    {
      path: 'mcs/zones',
      element: <McsZones />
    },
    {
      path: 'mcs/locations',
      element: <McsLocations />
    },
    {
      path: 'mcs/inbounds',
      element: <McsInbounds />
    },
    {
      path: 'mcs/outbounds',
      element: <McsOutbounds />
    },
    {
      path: 'mcs/transfers',
      element: <McsTransfers />
    },
    {
      path: 'mcs/route-management',
      element: <McsRouteManagement />
    },
    {
      path: 'mcs/route-optimizer',
      element: <McsRouteOptimizer />
    },
    {
      path: 'mcs/location-stock',
      element: <McsLocationStock />
    },
    {
      path: 'mcs/inventory-transactions',
      element: <McsInventoryTransactions />
    },
    {
      path: 'mcs/plc-events',
      element: <McsPlcEvents />
    },
    {
      path: 'mes/dashboard',
      element: <MesDashboard />
    },
    {
      path: 'mes/work-orders',
      element: <MesWorkOrders />
    },
    {
      path: 'mes/prod-plans',
      element: <MesProdPlans />
    },
    {
      path: 'mes/work-results',
      element: <MesWorkResults />
    },
    {
      path: 'mes/inventory',
      element: <MesInventory />
    },
    {
      path: 'mes/quality',
      element: <MesQuality />
    },
    {
      path: 'mes/defects',
      element: <MesDefects />
    },
    {
      path: 'mes/equipment',
      element: <MesEquipment />
    },
    {
      path: 'mes/master-data',
      element: <MesMasterData />
    },
    {
      path: 'ai/operations',
      element: <AiOperations />
    }
  ]
};

export default MainRoutes;
