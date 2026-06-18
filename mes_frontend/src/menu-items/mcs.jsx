import { AlertOutlined, AppstoreOutlined, DashboardOutlined, DatabaseOutlined, NodeIndexOutlined, SwapOutlined, ThunderboltOutlined, UnorderedListOutlined } from '@ant-design/icons';

const icons = {
  AlertOutlined,
  AppstoreOutlined,
  DashboardOutlined,
  DatabaseOutlined,
  NodeIndexOutlined,
  SwapOutlined,
  ThunderboltOutlined,
  UnorderedListOutlined
};

const mcs = {
  id: 'group-mcs',
  title: '',
  type: 'group',
  children: [
    {
      id: 'mcs',
      title: 'MCS',
      type: 'collapse',
      icon: icons.AppstoreOutlined,
      children: [
        {
          id: 'mcs-dashboard',
          title: 'MCS 대시보드',
          type: 'item',
          url: '/mcs/dashboard',
          icon: icons.DashboardOutlined,
          breadcrumbs: false
        },
        {
          id: 'mcs-zones',
          title: 'Zone 관리',
          type: 'item',
          url: '/mcs/zones',
          icon: icons.AppstoreOutlined,
          breadcrumbs: false
        },
        {
          id: 'mcs-locations',
          title: 'Location 관리',
          type: 'item',
          url: '/mcs/locations',
          icon: icons.DatabaseOutlined,
          breadcrumbs: false
        },
        {
          id: 'mcs-inbounds',
          title: '입고 관리',
          type: 'item',
          url: '/mcs/inbounds',
          icon: icons.UnorderedListOutlined,
          breadcrumbs: false
        },
        {
          id: 'mcs-outbounds',
          title: '출고 관리',
          type: 'item',
          url: '/mcs/outbounds',
          icon: icons.UnorderedListOutlined,
          breadcrumbs: false
        },
        {
          id: 'mcs-transfers',
          title: '이동 관리',
          type: 'item',
          url: '/mcs/transfers',
          icon: icons.SwapOutlined,
          breadcrumbs: false
        },
        {
          id: 'mcs-route-management',
          title: '경로 관리',
          type: 'item',
          url: '/mcs/route-management',
          icon: icons.NodeIndexOutlined,
          breadcrumbs: false
        },
        {
          id: 'mcs-route-optimizer',
          title: '경로 최적화',
          type: 'item',
          url: '/mcs/route-optimizer',
          icon: icons.NodeIndexOutlined,
          breadcrumbs: false
        },
        {
          id: 'mcs-location-stock',
          title: '로케이션 재고',
          type: 'item',
          url: '/mcs/location-stock',
          icon: icons.DatabaseOutlined,
          breadcrumbs: false
        },
        {
          id: 'mcs-inventory-transactions',
          title: '재고 이력',
          type: 'item',
          url: '/mcs/inventory-transactions',
          icon: icons.UnorderedListOutlined,
          breadcrumbs: false
        },
        {
          id: 'mcs-plc-events',
          title: 'PLC 이벤트',
          type: 'item',
          url: '/mcs/plc-events',
          icon: icons.AlertOutlined,
          breadcrumbs: false
        },
        {
          id: 'mcs-plc-simulator',
          title: 'PLC 시뮬레이터',
          type: 'item',
          url: '/mcs/plc-simulator',
          icon: icons.ThunderboltOutlined,
          breadcrumbs: false
        }
      ]
    }
  ]
};

export default mcs;
