/**
 * 메뉴 정의 통합 인덱스
 * 
 * 각 백엔드 모듈별로 분리된 메뉴 정의를 하나로 조합합니다.
 * 새 모듈을 추가할 때는 해당 모듈의 메뉴 파일을 만들고 여기에 import/spread만 하면 됩니다.
 */
export type { MenuItem, MenuGroup } from './types';

import { MenuGroup } from './types';

// ─── 백엔드 모듈별 메뉴 import ───
import { dashboardMenu }    from './dashboard';
import { ledgerMenu }       from './ledger';
import { closingMenu }      from './closing';
import { reportsMenu }      from './reports';
import { expenditureMenu }  from './expenditure';
import { taxMenu }          from './tax';
import { loanMenu }         from './loan';
import { fairValueMenu }    from './fair-value';
import { eclMenu }          from './ecl';
import { martMenu }         from './mart';
import { accountCodeMenu }  from './account-code';
import { partnerMenu }      from './partner';
import { governanceMenu }   from './governance';
import { systemMenu }       from './system';

/**
 * 전체 메뉴 정의 (순서 = 사이드바 표시 순서)
 */
export const allMenus: MenuGroup[] = [
  // DASHBOARD
  ...dashboardMenu,

  // ACCOUNTING: 원장 → 결산 → 보고서
  ...ledgerMenu,
  ...closingMenu,
  ...reportsMenu,

  // OPERATIONS: 지출 → 세무
  ...expenditureMenu,
  ...taxMenu,

  // CREDIT: 대출/이연 → 공정가치
  ...loanMenu,
  ...fairValueMenu,

  // RISK: ECL → 마트/대사
  ...eclMenu,
  ...martMenu,

  // MASTER: 계정과목 → 거래처
  ...accountCodeMenu,
  ...partnerMenu,

  // SYSTEM: 내부회계 → 시스템
  ...governanceMenu,
  ...systemMenu,
];
