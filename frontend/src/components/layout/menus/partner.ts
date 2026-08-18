import { MenuGroup } from './types';

/** 거래처 및 조직 기준정보 - backend: master-data 모듈 */
export const partnerMenu: MenuGroup[] = [
  {
    category: 'MASTER',
    group: '거래처 및 조직 정보',
    module: 'master-data',
    requiredRoles: ['MASTER_MANAGER', 'ACCOUNTING_ADMIN', 'SYSTEM_ADMIN'],
    items: [
      { icon: 'Users',       label: '거래처 조회/등록',  href: '/partner/list' },
      { icon: 'CheckCircle', label: '기준정보 변경 승인', href: '/master-data/partner' },
      { icon: 'Building',    label: '조직 및 부서 관리', href: '/system/departments' },
    ],
  },
];
