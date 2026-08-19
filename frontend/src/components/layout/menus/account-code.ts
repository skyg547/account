import { MenuGroup } from './types';

/** 계정과목 코드 관리 - backend: master-data 모듈 */
export const accountCodeMenu: MenuGroup[] = [
  {
    category: 'GOVERNANCE_SYSTEM',
    group: '표준 계정과목 체계 (master-data)',
    module: 'master-data',
    requiredRoles: ['MASTER_MANAGER', 'ACCOUNTING_ADMIN', 'SYSTEM_ADMIN'],
    items: [
      { icon: 'BookOpen', label: '계정과목 체계',     href: '/account-code/tree' },
      { icon: 'FilePlus', label: '계정과목 등록/수정', href: '/account-code/manage' },
      { icon: 'Package',  label: '상품 코드 관리',    href: '/account-code/products' },
    ],
  },
];
