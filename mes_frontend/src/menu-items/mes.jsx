import {
  AppstoreOutlined,
  BarChartOutlined,
  BuildOutlined,
  DashboardOutlined,
  DatabaseOutlined,
  ExperimentOutlined,
  ProfileOutlined
} from '@ant-design/icons';

const icons = {
  AppstoreOutlined,
  BarChartOutlined,
  BuildOutlined,
  DashboardOutlined,
  DatabaseOutlined,
  ExperimentOutlined,
  ProfileOutlined
};

const mes = {
  id: 'group-mes',
  title: '',
  type: 'group',
  children: [
    {
      id: 'mes',
      title: 'MES',
      type: 'collapse',
      icon: icons.AppstoreOutlined,
      children: [
        {
          id: 'mes-dashboard',
          title: 'MES 대시보드',
          type: 'item',
          url: '/mes/dashboard',
          icon: icons.DashboardOutlined,
          breadcrumbs: false
        },
        {
          id: 'mes-work-orders',
          title: '작업 오더',
          type: 'item',
          url: '/mes/work-orders',
          icon: icons.ProfileOutlined,
          breadcrumbs: false
        },
        {
          id: 'mes-prod-plans',
          title: '생산 계획',
          type: 'item',
          url: '/mes/prod-plans',
          icon: icons.BarChartOutlined,
          breadcrumbs: false
        },
        {
          id: 'mes-work-results',
          title: '생산 실적',
          type: 'item',
          url: '/mes/work-results',
          icon: icons.BarChartOutlined,
          breadcrumbs: false
        },
        {
          id: 'mes-inventory',
          title: 'MES 재고',
          type: 'item',
          url: '/mes/inventory',
          icon: icons.DatabaseOutlined,
          breadcrumbs: false
        },
        {
          id: 'mes-quality',
          title: '품질 검사',
          type: 'item',
          url: '/mes/quality',
          icon: icons.ExperimentOutlined,
          breadcrumbs: false
        },
        {
          id: 'mes-defects',
          title: '불량 이력',
          type: 'item',
          url: '/mes/defects',
          icon: icons.ExperimentOutlined,
          breadcrumbs: false
        },
        {
          id: 'mes-equipment',
          title: '설비 현황',
          type: 'item',
          url: '/mes/equipment',
          icon: icons.BuildOutlined,
          breadcrumbs: false
        },
        {
          id: 'mes-master-data',
          title: '기준 정보',
          type: 'item',
          url: '/mes/master-data',
          icon: icons.AppstoreOutlined,
          breadcrumbs: false
        }
      ]
    }
  ]
};

export default mes;
