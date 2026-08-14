import { MenuGroup } from './types';

/** 이연·대출·부대손익관리 - backend: loan 모듈 */
export const loanMenu: MenuGroup[] = [
  {
    category: 'CREDIT',
    group: '이연·대출·부대손익관리',
    module: 'loan',
    requiredRoles: ['ACCOUNTING_ADMIN', 'SYSTEM_ADMIN', 'RISK_MANAGER'],
    items: [
      { icon: 'FileText',     label: '대출 계약 관리',  href: '/loan/contracts' },
      { icon: 'Banknote',     label: '대출 실행(지급)',  href: '/loan/disbursal' },
      { icon: 'Clock',        label: '이연 수수료/원가', href: '/loan/deferred' },
      { icon: 'TrendingDown', label: 'EIR 상각 스케줄', href: '/loan/amortization' },
      { icon: 'RefreshCw',    label: '대출 이벤트 처리', href: '/loan/events' },
      { icon: 'Landmark',     label: '예금 계좌 관리',  href: '/loan/deposit' },
    ],
  },
];
