/**
 * 메뉴 정의 통합 인덱스
 * 
 * 백엔드 16대 마이크로서비스 모듈 경계와 1:1로 직결되는 프론트엔드 메뉴 정의를 하나로 조합합니다.
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
 * 전체 메뉴 정의 (순서 = 사이드바 및 카테고리 매핑 순서)
 */
export const allMenus: MenuGroup[] = [
  // 1. DASHBOARD
  ...dashboardMenu,

  // 2. JOURNAL: journal-ledger
  ...ledgerMenu,

  // 3. CLOSING: closing, reporting
  ...closingMenu,
  ...reportsMenu,

  // 4. EXPENDITURE: expenditure-resolution, payable, receivable, budget
  ...expenditureMenu,

  // 5. TAX: tax
  ...taxMenu,

  // 6. LOAN: loan, deposit
  ...loanMenu,

  // 7. ASSET_LEASE: asset-lease
  ...fairValueMenu,

  // 8. ECL: ecl
  ...eclMenu,

  // 9. MART_RECON: reconciliation, account-mart
  ...martMenu,

  // 10. MASTER: master-data
  ...accountCodeMenu,
  ...partnerMenu,

  // 11. AUDIT_SYSTEM: internal-audit, auth
  ...governanceMenu,
  ...systemMenu,
];
