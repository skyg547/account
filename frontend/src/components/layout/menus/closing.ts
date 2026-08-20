import { MenuGroup } from './types';

/** 결산관리 - backend: closing 모듈 */
export const closingMenu: MenuGroup[] = [
  {
    category: 'ACCOUNTING',
    group: '결산 마감 프로세스',
    module: 'closing',
    requiredRoles: ['ACCOUNTING_ADMIN', 'SYSTEM_ADMIN', 'AUDITOR'],
    items: [
      { icon: 'CalendarDays', label: '결산 캘린더',     href: '/closing/calendar' },
      { icon: 'CheckCircle',  label: '결산 태스크',     href: '/closing/tasks' },
      { icon: 'Shield',       label: '게이트 점검',     href: '/closing/gates' },
      { icon: 'Lock',         label: '기간 잠금/재개',  href: '/closing/period-lock' },
      { icon: 'RefreshCw',    label: '외화 평가 배치',  href: '/closing/valuation' },
      { icon: 'FileEdit',     label: '결산 조정 전표',  href: '/closing/adjustment' },
      { icon: 'Archive',      label: '연차 결산',      href: '/closing/annual' },
    ],
  },
];
