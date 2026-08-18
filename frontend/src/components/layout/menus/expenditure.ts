import { MenuGroup } from './types';

/** 지출·지급·예산 - backend: expenditure-resolution, payable, receivable, budget 모듈 */
export const expenditureMenu: MenuGroup[] = [
  {
    category: 'EXPENDITURE',
    group: '지출 및 지급 집행',
    module: 'expenditure-resolution',
    requiredRoles: ['ACCOUNTING_ADMIN', 'USER', 'SYSTEM_ADMIN'],
    items: [
      { icon: 'Receipt',          label: '지출결의서 작성',  href: '/expenditure/resolution' },
      { icon: 'CheckSquare',      label: '지출 승인',       href: '/expenditure/approval' },
      { icon: 'Banknote',         label: '지급 실행',      href: '/expenditure/payment' },
      { icon: 'Wallet',           label: '선급금 관리',     href: '/expenditure/advance' },
    ],
  },
  {
    category: 'EXPENDITURE',
    group: '채권 및 채무 관리',
    module: 'payable',
    requiredRoles: ['ACCOUNTING_ADMIN', 'USER', 'SYSTEM_ADMIN'],
    items: [
      { icon: 'CreditCard',       label: '매입채무(AP)',    href: '/expenditure/payable' },
      { icon: 'DollarSign',       label: '매출채권(AR)',    href: '/expenditure/receivable' },
      { icon: 'ArrowDownToLine',  label: '수금 관리',      href: '/expenditure/collection' },
    ],
  },
  {
    category: 'EXPENDITURE',
    group: '예산 및 자금수지',
    module: 'budget',
    requiredRoles: ['ACCOUNTING_ADMIN', 'SYSTEM_ADMIN'],
    items: [
      { icon: 'Calculator',       label: '예산 편성/통제',  href: '/expenditure/budget', requiredRoles: ['ACCOUNTING_ADMIN', 'SYSTEM_ADMIN'] },
      { icon: 'TrendingUp',       label: '자금 수지 계획',  href: '/expenditure/cashflow' },
    ],
  },
];
