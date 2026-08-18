import { MenuGroup } from './types';

/** 내부통제 및 감사 - backend: internal-audit 모듈 */
export const governanceMenu: MenuGroup[] = [
  {
    category: 'INTERNAL_AUDIT',
    group: '내부회계관리제도(K-SOX) & 감사',
    module: 'internal-audit',
    requiredRoles: ['AUDITOR', 'SYSTEM_ADMIN'],
    items: [
      { icon: 'FileSearch',     label: '감사 추적 로그 탐색기',  href: '/governance/audit-logs' },
      { icon: 'Shield',         label: '역할/권한 매트릭스',     href: '/governance/rbac' },
      { icon: 'ClipboardCheck', label: '내부통제 점검 체크리스트', href: '/governance/controls' },
    ],
  },
];
