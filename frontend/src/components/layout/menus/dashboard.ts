import { MenuGroup } from './types';

/** 대시보드 - 전체 집계 */
export const dashboardMenu: MenuGroup[] = [
  {
    category: 'DASHBOARD',
    group: '대시보드',
    module: 'dashboard',
    items: [
      { icon: 'LayoutDashboard', label: '통합 재무 현황', href: '/' },
    ],
  },
];
