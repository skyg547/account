import { MenuGroup } from './types';

/** 내부회계 관리 - backend: governance 모듈 */
export const governanceMenu: MenuGroup[] = [
  {
    category: 'SYSTEM',
    group: '내부회계 관리',
    module: 'governance',
    requiredRoles: ['AUDITOR', 'SYSTEM_ADMIN'],
    items: [
      { icon: 'FileSearch',     label: '감사 로그 탐색기',  href: '/governance/audit-logs' },
      { icon: 'Shield',         label: '역할/권한 매트릭스', href: '/governance/rbac' },
      { icon: 'ClipboardCheck', label: '내부통제 체크리스트', href: '/governance/controls' },
    ],
  },
];
