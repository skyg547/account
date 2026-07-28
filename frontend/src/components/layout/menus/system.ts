import { MenuGroup } from './types';

/** 시스템관리 - backend: admin 모듈 */
export const systemMenu: MenuGroup[] = [
  {
    category: 'SYSTEM',
    group: '시스템관리',
    module: 'admin',
    requiredRoles: ['SYSTEM_ADMIN'],
    items: [
      { icon: 'Users',    label: '사용자 관리',    href: '/system/users' },
      { icon: 'Building', label: '부서 관리',     href: '/system/departments' },
      { icon: 'Layers',   label: '메뉴 권한 관리', href: '/system/menus' },
      { icon: 'FileText', label: '시스템 로그',    href: '/system/logs' },
    ],
  },
];
