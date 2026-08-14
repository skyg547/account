import { MenuGroup } from './types';

/** 마트데이터 관리 및 대사 - backend: account-mart 모듈 */
export const martMenu: MenuGroup[] = [
  {
    category: 'RISK',
    group: '마트데이터 관리 및 대사',
    module: 'account-mart',
    requiredRoles: ['RISK_MANAGER', 'RISK_ANALYST', 'ACCOUNTING_ADMIN', 'SYSTEM_ADMIN'],
    items: [
      { icon: 'Grid3X3',     label: '마트 탐색기',     href: '/mart/explorer' },
      { icon: 'ShieldCheck', label: 'DQ 감사',        href: '/mart/dq-audit' },
      { icon: 'Globe',       label: '환율 관리',      href: '/mart/exchange-rates' },
      { icon: 'Percent',     label: '시장금리 관리',   href: '/mart/market-rates' },
      { icon: 'TrendingUp',  label: '수익률곡선',     href: '/mart/yield-curves' },
      { icon: 'Settings',    label: '대사 규칙 설정',  href: '/mart/reconciliation-setup' },
      { icon: 'Play',        label: '대사 실행',      href: '/mart/reconciliation-run' },
      { icon: 'GitCompare',  label: '차이 해소',      href: '/mart/reconciliation-diff' },
    ],
  },
];
