import { MenuGroup } from './types';

/** 예산 및 자금수지 관리 - backend: budget 모듈 */
export const budgetMenu: MenuGroup[] = [
  {
    category: 'OPERATIONS',
    group: '예산 편성 및 집행 통제',
    module: 'budget',
    requiredRoles: ['ACCOUNTING_ADMIN', 'SYSTEM_ADMIN'],
    items: [
      { icon: 'Calculator',       label: '예산 편성/통제',  href: '/expenditure/budget', requiredRoles: ['ACCOUNTING_ADMIN', 'SYSTEM_ADMIN'] },
      { icon: 'TrendingUp',       label: '자금 수지 계획',  href: '/expenditure/cashflow' },
    ],
  },
];
