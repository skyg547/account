import { MenuGroup } from './types';

/** 수신 및 법인계좌 관리 - backend: deposit 모듈 */
export const depositMenu: MenuGroup[] = [
  {
    category: 'BANKING_ASSET',
    group: '수신 및 법인 예적금',
    module: 'deposit',
    requiredRoles: ['ACCOUNTING_ADMIN', 'SYSTEM_ADMIN'],
    items: [
      { icon: 'Landmark',     label: '예금 계좌 관리',  href: '/loan/deposit' },
    ],
  },
];
