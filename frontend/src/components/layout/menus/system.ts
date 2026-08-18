import { MenuGroup } from './types';

/** 시스템 및 보안 관리 - backend: auth, admin 모듈 */
export const systemMenu: MenuGroup[] = [
  {
    category: 'AUDIT_SYSTEM',
    group: '시스템 계정 및 토큰 보안',
    module: 'admin',
    requiredRoles: ['SYSTEM_ADMIN'],
    items: [
      { icon: 'Users',    label: '사용자 계정 관리', href: '/system/users' },
      { icon: 'Layers',   label: '메뉴 권한(RBAC)', href: '/system/menus' },
      { icon: 'KeyRound', label: 'PAT 토큰 거버넌스', href: '/system/tokens' },
      { icon: 'FileText', label: '시스템 로그',    href: '/system/logs' },
    ],
  },
];
