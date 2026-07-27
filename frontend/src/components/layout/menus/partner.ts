import { MenuGroup } from './types';

/** 거래처 관리 - backend: master-data 모듈 */
export const partnerMenu: MenuGroup[] = [
  {
    category: 'MASTER',
    group: '거래처 관리',
    module: 'master-data',
    requiredRoles: ['MASTER_MANAGER', 'ACCOUNTING_ADMIN', 'SYSTEM_ADMIN'],
    items: [
      { icon: 'Users',       label: '거래처 조회/등록',  href: '/partner/list' },
      { icon: 'CheckCircle', label: '기준정보 변경 승인', href: '/partner/approval' },
    ],
  },
];
