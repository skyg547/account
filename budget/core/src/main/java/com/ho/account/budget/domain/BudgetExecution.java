package com.ho.account.budget.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * 외부 원천 문서의 한 라인이 예산을 사용한 사실과 취소 상태를 보존합니다.
 */
public final class BudgetExecution {

    private final Long id;
    private final Long budgetPlanId;
    private final String sourceType;
    private final String sourceId;
    private final String sourceLineId;
    private final LocalDate executionDate;
    private final BigDecimal amount;
    private BudgetExecutionStatus status;
    private final String executedBy;
    private String cancelledBy;

    private BudgetExecution(
            Long id,
            Long budgetPlanId,
            String sourceType,
            String sourceId,
            String sourceLineId,
            LocalDate executionDate,
            BigDecimal amount,
            BudgetExecutionStatus status,
            String executedBy,
            String cancelledBy) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("id는 양수여야 합니다.");
        }
        if (budgetPlanId == null || budgetPlanId <= 0) {
            throw new IllegalArgumentException("budgetPlanId는 양수여야 합니다.");
        }
        this.id = id;
        this.budgetPlanId = budgetPlanId;
        this.sourceType = BudgetPlan.requireText(sourceType, "sourceType");
        this.sourceId = BudgetPlan.requireText(sourceId, "sourceId");
        this.sourceLineId = BudgetPlan.requireText(sourceLineId, "sourceLineId");
        this.executionDate = Objects.requireNonNull(executionDate, "executionDate은(는) 필수입니다.");
        this.amount = BudgetPrecision.positive(amount, "amount");
        this.status = Objects.requireNonNull(status, "status은(는) 필수입니다.");
        this.executedBy = BudgetPlan.requireText(executedBy, "executedBy");
        this.cancelledBy = cancelledBy == null ? null : BudgetPlan.requireText(cancelledBy, "cancelledBy");
        if (status == BudgetExecutionStatus.EXECUTED && cancelledBy != null) {
            throw new IllegalArgumentException("EXECUTED 집행에는 취소자가 있을 수 없습니다.");
        }
        if (status == BudgetExecutionStatus.CANCELLED && this.cancelledBy == null) {
            throw new IllegalArgumentException("CANCELLED 집행에는 취소자가 필요합니다.");
        }
    }

    public static BudgetExecution execute(
            Long budgetPlanId,
            String sourceType,
            String sourceId,
            String sourceLineId,
            LocalDate executionDate,
            BigDecimal amount,
            String executedBy) {
        return new BudgetExecution(
                null,
                budgetPlanId,
                sourceType,
                sourceId,
                sourceLineId,
                executionDate,
                amount,
                BudgetExecutionStatus.EXECUTED,
                executedBy,
                null);
    }

    public static BudgetExecution restore(
            Long id,
            Long budgetPlanId,
            String sourceType,
            String sourceId,
            String sourceLineId,
            LocalDate executionDate,
            BigDecimal amount,
            BudgetExecutionStatus status,
            String executedBy,
            String cancelledBy) {
        if (id == null) {
            throw new IllegalArgumentException("복원하는 BudgetExecution의 id는 필수입니다.");
        }
        return new BudgetExecution(
                id,
                budgetPlanId,
                sourceType,
                sourceId,
                sourceLineId,
                executionDate,
                amount,
                status,
                executedBy,
                cancelledBy);
    }

    public void cancel(String actor) {
        String normalizedActor = BudgetPlan.requireText(actor, "actor");
        if (status == BudgetExecutionStatus.CANCELLED) {
            return;
        }
        status = BudgetExecutionStatus.CANCELLED;
        cancelledBy = normalizedActor;
    }

    public boolean hasSameExecution(
            Long planId,
            LocalDate requestedExecutionDate,
            BigDecimal requestedAmount,
            String executor) {
        return budgetPlanId.equals(planId)
                && executionDate.equals(requestedExecutionDate)
                && amount.compareTo(BudgetPrecision.positive(requestedAmount, "amount")) == 0
                && executedBy.equals(BudgetPlan.requireText(executor, "executor"));
    }

    public boolean hasSameExecution(
            Long planId, LocalDate requestedExecutionDate, BigDecimal requestedAmount) {
        return budgetPlanId.equals(planId)
                && executionDate.equals(requestedExecutionDate)
                && amount.compareTo(BudgetPrecision.positive(requestedAmount, "amount")) == 0;
    }

    public Long id() {
        return id;
    }

    public Long budgetPlanId() {
        return budgetPlanId;
    }

    public String sourceType() {
        return sourceType;
    }

    public String sourceId() {
        return sourceId;
    }

    public String sourceLineId() {
        return sourceLineId;
    }

    public LocalDate executionDate() {
        return executionDate;
    }

    public BigDecimal amount() {
        return amount;
    }

    public BudgetExecutionStatus status() {
        return status;
    }

    public String executedBy() {
        return executedBy;
    }

    public String cancelledBy() {
        return cancelledBy;
    }

    public Long getId() {
        return id();
    }

    public Long getBudgetPlanId() {
        return budgetPlanId();
    }

    public String getSourceType() {
        return sourceType();
    }

    public String getSourceId() {
        return sourceId();
    }

    public String getSourceLineId() {
        return sourceLineId();
    }

    public LocalDate getExecutionDate() {
        return executionDate();
    }

    public BigDecimal getAmount() {
        return amount();
    }

    public BudgetExecutionStatus getStatus() {
        return status();
    }

    public String getExecutedBy() {
        return executedBy();
    }

    public String getCancelledBy() {
        return cancelledBy();
    }
}
