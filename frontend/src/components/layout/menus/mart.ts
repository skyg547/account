import { MenuGroup } from './types';

/** 회계 대사 및 재무 마트 - backend: account-mart, reconciliation 모듈 */
export const martMenu: MenuGroup[] = [
  {
    category: 'MART_RECON',
    group: '원장·보조원장 자동 대사',
    module: 'reconciliation',
    requiredRoles: ['ACCOUNTING_ADMIN', 'SYSTEM_ADMIN'],
    items: [
      { icon: 'Settings',    label: '대사 규칙 설정',  href: '/mart/reconciliation-setup' },
      { icon: 'Play',        label: '대사 실행',      href: '/mart/reconciliation-run' },
      { icon: 'GitCompare',  label: '차이 해소',      href: '/mart/reconciliation-diff' },
    ],
  },
  {
    category: 'MART_RECON',
    group: '재무 데이터 마트 & 시장정보',
    module: 'account-mart',
    requiredRoles: ['RISK_MANAGER', 'RISK_ANALYST', 'ACCOUNTING_ADMIN', 'SYSTEM_ADMIN'],
    items: [
      { icon: 'Grid3X3',     label: '마트 탐색기',     href: '/mart/explorer' },
      { icon: 'ShieldCheck', label: 'DQ 감사',        href: '/mart/dq-audit' },
      { icon: 'Globe',       label: '환율 관리',      href: '/mart/exchange-rates' },
      { icon: 'Percent',     label: '시장금리 관리',   href: '/mart/market-rates' },
      { icon: 'TrendingUp',  label: '수익률곡선',     href: '/mart/yield-curves' },
    ],
  },
];
