/**
 * 메뉴 정의 통합 인덱스
 * 
 * 백엔드 16대 마이크로서비스 모듈 경계와 1:1로 완전 분리된 프론트엔드 메뉴 정의를 하나로 조합합니다.
 */
export type { MenuItem, MenuGroup } from './types';

import { MenuGroup } from './types';

// ─── 백엔드 모듈별 완전 분리 메뉴 import ───
import { dashboardMenu }      from './dashboard';
import { ledgerMenu }         from './ledger';
import { closingMenu }        from './closing';
import { reportsMenu }        from './reports';
import { expenditureMenu }    from './expenditure';
import { budgetMenu }         from './budget';
import { taxMenu }            from './tax';
import { loanMenu }           from './loan';
import { depositMenu }        from './deposit';
import { fairValueMenu }      from './fair-value';
import { eclMenu }            from './ecl';
import { reconciliationMenu } from './reconciliation';
import { martMenu }           from './mart';
import { accountCodeMenu }    from './account-code';
import { partnerMenu }        from './partner';
import { governanceMenu }     from './governance';
import { systemMenu }         from './system';

/**
 * 전체 메뉴 정의 (순서 = 사이드바 및 카테고리 매핑 순서)
 */
export const allMenus: MenuGroup[] = [
  // 1. DASHBOARD
  ...dashboardMenu,

  // 2. JOURNAL (journal-ledger)
  ...ledgerMenu,

  // 3. CLOSING (closing)
  ...closingMenu,

  // 4. REPORTING (reporting)
  ...reportsMenu,

  // 5. EXPENDITURE (expenditure-resolution, payable, receivable)
  ...expenditureMenu,

  // 6. BUDGET (budget)
  ...budgetMenu,

  // 7. TAX (tax)
  ...taxMenu,

  // 8. LOAN (loan)
  ...loanMenu,

  // 9. DEPOSIT (deposit)
  ...depositMenu,

  // 10. ASSET_LEASE (asset-lease)
  ...fairValueMenu,

  // 11. ECL (ecl)
  ...eclMenu,

  // 12. RECONCILIATION (reconciliation)
  ...reconciliationMenu,

  // 13. ACCOUNT_MART (account-mart)
  ...martMenu,

  // 14. MASTER (master-data)
  ...accountCodeMenu,
  ...partnerMenu,

  // 15. INTERNAL_AUDIT (internal-audit)
  ...governanceMenu,

  // 16. SYSTEM_SECURITY (auth, admin)
  ...systemMenu,
];
