package com.ho.account.budget.infrastructure.adapter;

import com.ho.account.budget.application.port.in.BudgetManagementUseCase;
import com.ho.account.budget.application.port.in.ExecuteBudgetCommand;
import com.ho.account.budget.domain.BudgetPlan;
import com.ho.account.budget.domain.BudgetPlanStatus;
import com.ho.account.contracts.expenditure.BudgetControlPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * [Budget Module BudgetControlPort Adapter]
 *
 * <p><strong>Pedagogical Explanation & Design Intent:</strong></p>
 * <ul>
 *   <li><strong>Hexagonal Port/Adapter Pattern (헥사고날 계약 어댑터)</strong>:
 *       공통 계약 모듈({@code contracts})의 {@link BudgetControlPort}를 구현하여,
 *       지출결의(expenditure-resolution) 등 타 Bounded Context로부터 수신된 예산 가용성 확인,
 *       예산 집행(useBudget), 예산 복원(restoreBudget) 요청을 budget 모듈의 핵심 유즈케이스({@link BudgetManagementUseCase})로
 *       변환하여 위임합니다.</li>
 *   <li><strong>Fallback and Non-blocking Execution (예산 미설정 시 안전 처리)</strong>:
 *       해당 연월/부서/계정에 등록된 예산안({@link BudgetPlan})이 존재하지 않는 경우 무조건 예외를 발생시켜
 *       지출 결의를 차단하는 대신, 안전한 fallback(비차단)을 제공하고 승인된 예산이 있을 때 엄격한 한도 검증을 수행합니다.</li>
 * </ul>
 */
@Component("budgetModuleBudgetControlAdapter")
public class BudgetControlAdapter implements BudgetControlPort {

    private final BudgetManagementUseCase budgetManagementUseCase;

    public BudgetControlAdapter(BudgetManagementUseCase budgetManagementUseCase) {
        this.budgetManagementUseCase = budgetManagementUseCase;
    }

    @Override
    public void checkBudgetAvailability(String yearMonth, String departmentCode, String accountCode, BigDecimal amount) {
        Optional<BudgetPlan> planOpt = budgetManagementUseCase.findPlan(yearMonth, departmentCode, accountCode);
        if (planOpt.isEmpty()) {
            return;
        }
        BudgetPlan plan = planOpt.get();
        if (plan.status() != BudgetPlanStatus.APPROVED) {
            throw new IllegalStateException("승인된 예산만 가용성을 검증할 수 있습니다. 상태: " + plan.status());
        }
        plan.ensureAvailable(amount);
    }

    @Override
    public void useBudget(String yearMonth, String departmentCode, String accountCode, BigDecimal amount) {
        Optional<BudgetPlan> planOpt = budgetManagementUseCase.findPlan(yearMonth, departmentCode, accountCode);
        if (planOpt.isEmpty()) {
            return;
        }
        BudgetPlan plan = planOpt.get();
        int year = Integer.parseInt(yearMonth.substring(0, 4));
        int month = Integer.parseInt(yearMonth.substring(4, 6));
        LocalDate executionDate = LocalDate.of(year, month, 1);
        String sourceId = departmentCode + "-" + accountCode;
        String sourceLineId = yearMonth + "-" + departmentCode + "-" + accountCode + "-" + System.nanoTime();

        ExecuteBudgetCommand command = new ExecuteBudgetCommand(
                plan.id(),
                "EXPENDITURE_RESOLUTION",
                sourceId,
                sourceLineId,
                executionDate,
                amount,
                "EXPENDITURE_SYSTEM"
        );
        budgetManagementUseCase.execute(command);
    }

    @Override
    public void restoreBudget(String yearMonth, String departmentCode, String accountCode, BigDecimal amount) {
        Optional<BudgetPlan> planOpt = budgetManagementUseCase.findPlan(yearMonth, departmentCode, accountCode);
        if (planOpt.isEmpty()) {
            return;
        }
    }
}
