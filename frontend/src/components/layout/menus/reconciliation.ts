import { MenuGroup } from './types';

/** 원장·보조원장 자동 대사 - backend: reconciliation 모듈 */
export const reconciliationMenu: MenuGroup[] = [
  {
    category: 'RECONCILIATION',
    group: '원장·보조원장 자동 대사',
    module: 'reconciliation',
    requiredRoles: ['ACCOUNTING_ADMIN', 'SYSTEM_ADMIN'],
    items: [
      { icon: 'Settings',    label: '대사 규칙 설정',  href: '/mart/reconciliation-setup' },
      { icon: 'Play',        label: '대사 배치 실행',  href: '/mart/reconciliation-run' },
      { icon: 'GitCompare',  label: '차이 원인 해소',  href: '/mart/reconciliation-diff' },
    ],
  },
];
