package com.ho.account.budget.application.port.out;

import com.ho.account.budget.domain.BudgetFiscalYearControl;
import java.util.Optional;

/**
 * 사전 생성된 회계연도 제어 행을 잠그는 출력 Port입니다.
 *
 * <p>구현 Adapter는 반드시 pessimistic write lock을 사용해야 합니다. 없는 행은 잠금
 * 대상이 될 수 없으므로 지원 회계연도 행은 migration으로 미리 생성합니다.</p>
 */
public interface BudgetFiscalYearControlPersistencePort {

    Optional<BudgetFiscalYearControl> findByFiscalYearForUpdate(String fiscalYear);

    BudgetFiscalYearControl save(BudgetFiscalYearControl control);
}
