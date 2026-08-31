package com.ho.account.budget.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.budget.application.port.in.BudgetManagementUseCase;
import com.ho.account.budget.application.port.in.ExecuteBudgetCommand;
import com.ho.account.budget.domain.BudgetPlan;
import com.ho.account.budget.domain.BudgetPlanStatus;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BudgetControlAdapterTest {

    private BudgetManagementUseCase budgetManagementUseCase;
    private BudgetControlAdapter adapter;

    @BeforeEach
    void setUp() {
        budgetManagementUseCase = mock(BudgetManagementUseCase.class);
        adapter = new BudgetControlAdapter(budgetManagementUseCase);
    }

    @Test
    void checkBudgetAvailabilitySucceedsWhenNoPlanConfigured() {
        when(budgetManagementUseCase.findPlan("202604", "D001", "EXP001")).thenReturn(Optional.empty());

        assertThatCode(() -> adapter.checkBudgetAvailability("202604", "D001", "EXP001", new BigDecimal("5000.00")))
                .doesNotThrowAnyException();
    }

    @Test
    void checkBudgetAvailabilitySucceedsWhenApprovedPlanHasSufficientAmount() {
        BudgetPlan plan = BudgetPlan.restore(
                1L,
                "PLAN-001",
                "202604",
                "D001",
                "EXP001",
                new BigDecimal("10000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BudgetPlanStatus.APPROVED,
                "maker",
                "checker",
                null
        );
        when(budgetManagementUseCase.findPlan("202604", "D001", "EXP001")).thenReturn(Optional.of(plan));

        assertThatCode(() -> adapter.checkBudgetAvailability("202604", "D001", "EXP001", new BigDecimal("5000.00")))
                .doesNotThrowAnyException();
    }

    @Test
    void checkBudgetAvailabilityThrowsWhenApprovedPlanHasInsufficientAmount() {
        BudgetPlan plan = BudgetPlan.restore(
                1L,
                "PLAN-001",
                "202604",
                "D001",
                "EXP001",
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BudgetPlanStatus.APPROVED,
                "maker",
                "checker",
                null
        );
        when(budgetManagementUseCase.findPlan("202604", "D001", "EXP001")).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> adapter.checkBudgetAvailability("202604", "D001", "EXP001", new BigDecimal("5000.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("가용 예산이 부족합니다");
    }

    @Test
    void useBudgetExecutesCommandWhenPlanExists() {
        BudgetPlan plan = BudgetPlan.restore(
                1L,
                "PLAN-001",
                "202604",
                "D001",
                "EXP001",
                new BigDecimal("10000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BudgetPlanStatus.APPROVED,
                "maker",
                "checker",
                null
        );
        when(budgetManagementUseCase.findPlan("202604", "D001", "EXP001")).thenReturn(Optional.of(plan));

        adapter.useBudget("202604", "D001", "EXP001", new BigDecimal("3000.00"));

        verify(budgetManagementUseCase).execute(any(ExecuteBudgetCommand.class));
    }

    @Test
    void useBudgetSucceedsWhenNoPlanConfigured() {
        when(budgetManagementUseCase.findPlan("202604", "D001", "EXP001")).thenReturn(Optional.empty());

        assertThatCode(() -> adapter.useBudget("202604", "D001", "EXP001", new BigDecimal("3000.00")))
                .doesNotThrowAnyException();
    }
}
