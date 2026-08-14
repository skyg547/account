import { MenuGroup } from './types';

/** 지출관리 - backend: expenditure-resolution + payable + receivable 모듈 */
export const expenditureMenu: MenuGroup[] = [
  {
    category: 'OPERATIONS',
    group: '지출관리',
    module: 'expenditure-resolution',
    requiredRoles: ['ACCOUNTING_ADMIN', 'USER', 'SYSTEM_ADMIN'],
    items: [
      { icon: 'Receipt',          label: '지출결의서 작성',  href: '/expenditure/resolution' },
      { icon: 'CheckSquare',      label: '지출 승인',       href: '/expenditure/approval' },
      { icon: 'CreditCard',       label: '매입채무(AP)',    href: '/expenditure/payable' },
      { icon: 'DollarSign',       label: '매출채권(AR)',    href: '/expenditure/receivable' },
      { icon: 'Banknote',         label: '지급 실행',      href: '/expenditure/payment' },
      { icon: 'Wallet',           label: '선급금 관리',     href: '/expenditure/advance' },
      { icon: 'ArrowDownToLine',  label: '수금 관리',      href: '/expenditure/collection' },
      { icon: 'Calculator',       label: '예산 편성/통제',  href: '/expenditure/budget', requiredRoles: ['ACCOUNTING_ADMIN', 'SYSTEM_ADMIN'] },
      { icon: 'TrendingUp',       label: '자금 수지 계획',  href: '/expenditure/cashflow' },
    ],
  },
];
