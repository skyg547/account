import { MenuGroup } from './types';

/** 대손충당금관리 (IFRS 9) - backend: ecl 모듈 */
export const eclMenu: MenuGroup[] = [
  {
    category: 'RISK',
    group: '대손충당금관리 (IFRS 9)',
    module: 'ecl',
    requiredRoles: ['RISK_MANAGER', 'RISK_ANALYST', 'SYSTEM_ADMIN'],
    items: [
      { icon: 'Cpu',          label: 'ECL 배치 관제탑', href: '/ecl/batch' },
      { icon: 'Database',     label: '여신 익스포저',   href: '/ecl/exposures' },
      { icon: 'Settings',     label: '모델 파라미터',   href: '/ecl/parameters' },
      { icon: 'BarChart3',    label: 'ECL 산출 결과',   href: '/ecl/results' },
      { icon: 'FlaskConical', label: 'EAD 엔진 검증',  href: '/ecl/ead' },
    ],
  },
];
