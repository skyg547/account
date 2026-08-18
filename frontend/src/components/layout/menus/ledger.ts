import { MenuGroup } from './types';

/** 분개·원장 - backend: journal-ledger 모듈 */
export const ledgerMenu: MenuGroup[] = [
  {
    category: 'JOURNAL',
    group: '분개 및 원장 관리',
    module: 'journal-ledger',
    requiredRoles: ['ACCOUNTING_ADMIN', 'USER', 'SYSTEM_ADMIN', 'AUDITOR'],
    items: [
      { icon: 'Plus',        label: '전표 입력',      href: '/ledger/entry', requiredRoles: ['USER', 'ACCOUNTING_ADMIN', 'SYSTEM_ADMIN'] },
      { icon: 'FileText',    label: '전표 조회',      href: '/ledger/list' },
      { icon: 'BarChart3',   label: '총계정원장(GL)',  href: '/ledger/gl' },
      { icon: 'PieChart',    label: '보조원장(SL)',    href: '/ledger/sl' },
      { icon: 'Zap',         label: '자동 분개 규칙',  href: '/ledger/rules', requiredRoles: ['ACCOUNTING_ADMIN', 'SYSTEM_ADMIN'] },
      { icon: 'AlertCircle', label: '미결항목 정리',   href: '/ledger/unsettled' },
    ],
  },
];
