package com.ho.account.expenditure.application.service;

import com.ho.account.expenditure.application.port.out.BudgetPersistencePort;
import com.ho.account.expenditure.domain.Budget;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BudgetService의 예산 복원 및 집행 로직에 대한 단위 테스트입니다.
 */
@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetPersistencePort budgetPersistencePort;

    @InjectMocks
    private BudgetService budgetService;

    @Test
    void restoreBudgetReducesUsedAmountAndSavesBudget() {
        Budget budget = new Budget();
        budget.setYearMonth("202604");
        budget.setDeptCode("D001");
        budget.setAccountCode("EXP001");
        budget.setAssignedAmount(new BigDecimal("10000.00"));
        budget.setUsedAmount(new BigDecimal("3000.00"));

        when(budgetPersistencePort.findByYearMonthAndDepartmentCodeAndAccountCode("202604", "D001", "EXP001"))
                .thenReturn(Optional.of(budget));

        budgetService.restoreBudget("202604", "D001", "EXP001", new BigDecimal("1000.00"));

        assertEquals(new BigDecimal("2000.00"), budget.getUsedAmount());
        assertEquals(new BigDecimal("8000.00"), budget.getRemainingAmount());
        verify(budgetPersistencePort).save(budget);
    }
}
