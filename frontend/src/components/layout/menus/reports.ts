import { MenuGroup } from './types';

/** 보고서관리 - backend: reporting 모듈 */
export const reportsMenu: MenuGroup[] = [
  {
    category: 'ACCOUNTING',
    group: '보고서관리',
    module: 'reporting',
    requiredRoles: ['ACCOUNTING_ADMIN', 'SYSTEM_ADMIN', 'AUDITOR'],
    items: [
      { icon: 'FileBarChart', label: '재무제표 조회',   href: '/reports/statements' },
      { icon: 'Download',     label: '보고서 내보내기',  href: '/reports/export' },
      { icon: 'FileSearch',   label: '주석 정보 마트',  href: '/reports/disclosure-notes' },
      { icon: 'Send',         label: '규제 보고 제출',  href: '/reports/regulatory' },
    ],
  },
];
