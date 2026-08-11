package com.ho.account.contracts.expenditure;

import java.math.BigDecimal;

public interface BudgetControlPort {

    void checkBudgetAvailability(String yearMonth, String departmentCode, String accountCode, BigDecimal amount);

    void useBudget(String yearMonth, String departmentCode, String accountCode, BigDecimal amount);

    void restoreBudget(String yearMonth, String departmentCode, String accountCode, BigDecimal amount);
}
