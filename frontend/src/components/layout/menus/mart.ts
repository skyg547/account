import { MenuGroup } from './types';

/** 재무 데이터 마트 & 시장정보 - backend: account-mart 모듈 */
export const martMenu: MenuGroup[] = [
  {
    category: 'ACCOUNT_MART',
    group: '재무 데이터 마트 및 시장정보',
    module: 'account-mart',
    requiredRoles: ['RISK_MANAGER', 'RISK_ANALYST', 'ACCOUNTING_ADMIN', 'SYSTEM_ADMIN'],
    items: [
      { icon: 'Grid3X3',     label: '마트 탐색기',     href: '/mart/explorer' },
      { icon: 'ShieldCheck', label: 'DQ 품질 감사',    href: '/mart/dq-audit' },
      { icon: 'Globe',       label: '실시간 환율 관리', href: '/mart/exchange-rates' },
      { icon: 'Percent',     label: '시장금리 관리',   href: '/mart/market-rates' },
      { icon: 'TrendingUp',  label: '수익률곡선(YC)',  href: '/mart/yield-curves' },
    ],
  },
];
