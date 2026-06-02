import { RobotOutlined } from '@ant-design/icons';

const icons = {
  RobotOutlined
};

const ai = {
  id: 'group-ai',
  title: '',
  type: 'group',
  children: [
    {
      id: 'ai',
      title: 'AI',
      type: 'collapse',
      icon: icons.RobotOutlined,
      children: [
        {
          id: 'ai-operations',
          title: '운영 분석',
          type: 'item',
          url: '/ai/operations',
          icon: icons.RobotOutlined,
          breadcrumbs: false
        }
      ]
    }
  ]
};

export default ai;
