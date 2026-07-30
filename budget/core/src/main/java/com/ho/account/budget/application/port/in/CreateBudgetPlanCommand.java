package com.ho.account.budget.application.port.in;

import com.ho.account.budget.application.exception.BudgetRuleViolationException;
import com.ho.account.budget.domain.BudgetPrecision;
import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * HTTP나 Batch 입력과 무관한 예산안 생성 명령입니다.
 */
public record CreateBudgetPlanCommand(
        String planCode,
        String yearMonth,
        String departmentCode,
        String accountCode,
        BigDecimal allocatedAmount,
        String actor) {

    private static final Pattern YEAR_MONTH = Pattern.compile("\\d{6}");

    public CreateBudgetPlanCommand {
        planCode = requireText(planCode, "planCode", 50);
        yearMonth = requireYearMonth(yearMonth);
        departmentCode = requireText(departmentCode, "departmentCode", 50);
        accountCode = requireText(accountCode, "accountCode", 50);
        try {
            allocatedAmount = BudgetPrecision.positive(allocatedAmount, "allocatedAmount");
        } catch (IllegalArgumentException exception) {
            throw new BudgetRuleViolationException(exception.getMessage(), exception);
        }
        actor = requireText(actor, "actor", 80);
    }

    private static String requireYearMonth(String value) {
        String normalized = requireText(value, "yearMonth", 6);
        if (!YEAR_MONTH.matcher(normalized).matches()) {
            throw new BudgetRuleViolationException("yearMonth는 YYYYMM 형식이어야 합니다.");
        }
        int month = Integer.parseInt(normalized.substring(4));
        if (month < 1 || month > 12) {
            throw new BudgetRuleViolationException("yearMonth의 월은 01부터 12 사이여야 합니다.");
        }
        return normalized;
    }

    static String requireText(String value, String fieldName, int maximumLength) {
        if (value == null || value.isBlank()) {
            throw new BudgetRuleViolationException(fieldName + "은(는) 필수입니다.");
        }
        String normalized = value.trim();
        if (normalized.length() > maximumLength) {
            throw new BudgetRuleViolationException(
                    fieldName + "은(는) " + maximumLength + "자 이하여야 합니다.");
        }
        return normalized;
    }
}
