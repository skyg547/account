package com.ho.account.budget.application.port.in;

public interface BudgetYearEndUseCase {

    /**
     * 회계연도의 APPROVED 예산을 닫고 실제 상태가 바뀐 건수를 반환합니다.
     */
    int closeFiscalYear(String fiscalYear, String actor);
}
