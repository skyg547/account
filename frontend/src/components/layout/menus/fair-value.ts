import { MenuGroup } from './types';

/** 고정자산 및 리스회계 - backend: asset-lease 모듈 */
export const fairValueMenu: MenuGroup[] = [
  {
    category: 'ASSET_LEASE',
    group: '유형 및 무형 고정자산',
    module: 'asset-lease',
    requiredRoles: ['ACCOUNTING_ADMIN', 'SYSTEM_ADMIN'],
    items: [
      { icon: 'Package',      label: '고정자산 대장',    href: '/fair-value/assets' },
      { icon: 'Activity',     label: '감가상각 실행',    href: '/fair-value/depreciation' },
      { icon: 'Trash2',       label: '자산 처분',       href: '/fair-value/disposal' },
      { icon: 'Scale',        label: '자산 재평가/손상',  href: '/fair-value/revaluation' },
    ],
  },
  {
    category: 'ASSET_LEASE',
    group: 'IFRS 16 리스 회계',
    module: 'asset-lease',
    requiredRoles: ['ACCOUNTING_ADMIN', 'SYSTEM_ADMIN'],
    items: [
      { icon: 'FileText',     label: 'IFRS 16 리스 계약', href: '/fair-value/lease' },
      { icon: 'CalendarDays', label: '리스 월결산',      href: '/fair-value/lease-monthly' },
      { icon: 'RefreshCw',    label: '리스 재측정',      href: '/fair-value/lease-remeasure' },
    ],
  },
];
