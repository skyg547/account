import { MenuGroup } from './types';

/** 세무회계 - backend: tax 모듈 */
export const taxMenu: MenuGroup[] = [
  {
    category: 'TAX',
    group: '전자세금계산서 및 부가세',
    module: 'tax',
    requiredRoles: ['ACCOUNTING_ADMIN', 'SYSTEM_ADMIN'],
    items: [
      { icon: 'FileInput',  label: '매입 세금계산서', href: '/tax/purchase' },
      { icon: 'FileOutput', label: '매출 세금계산서', href: '/tax/sales' },
      { icon: 'Percent',    label: '부가세 신고 기초', href: '/tax/vat' },
      { icon: 'Search',     label: '국세청 승인 조회', href: '/tax/nts-verification' },
    ],
  },
];
