package com.ho.account.budget.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 두 예산 사이의 이동 요청과 승인 증적을 보존합니다.
 */
public final class BudgetTransfer {

    private final Long id;
    private final String requestKey;
    private final Long sourcePlanId;
    private final Long targetPlanId;
    private final BigDecimal amount;
    private BudgetTransferStatus status;
    private final String requestedBy;
    private String approvedBy;

    private BudgetTransfer(
            Long id,
            String requestKey,
            Long sourcePlanId,
            Long targetPlanId,
            BigDecimal amount,
            BudgetTransferStatus status,
            String requestedBy,
            String approvedBy) {
        this.id = validateOptionalId(id, "id");
        this.requestKey = BudgetPlan.requireText(requestKey, "requestKey");
        this.sourcePlanId = validateRequiredId(sourcePlanId, "sourcePlanId");
        this.targetPlanId = validateRequiredId(targetPlanId, "targetPlanId");
        if (sourcePlanId.equals(targetPlanId)) {
            throw new IllegalArgumentException("출발 예산과 도착 예산은 달라야 합니다.");
        }
        this.amount = BudgetPrecision.positive(amount, "amount");
        this.status = Objects.requireNonNull(status, "status은(는) 필수입니다.");
        this.requestedBy = BudgetPlan.requireText(requestedBy, "requestedBy");
        this.approvedBy = approvedBy == null ? null : BudgetPlan.requireText(approvedBy, "approvedBy");
        if (status == BudgetTransferStatus.REQUESTED && approvedBy != null) {
            throw new IllegalArgumentException("REQUESTED 전용에는 승인자가 있을 수 없습니다.");
        }
        if (status == BudgetTransferStatus.APPROVED && this.approvedBy == null) {
            throw new IllegalArgumentException("APPROVED 전용에는 승인자가 필요합니다.");
        }
    }

    public static BudgetTransfer request(
            String requestKey,
            Long sourcePlanId,
            Long targetPlanId,
            BigDecimal amount,
            String requestedBy) {
        return new BudgetTransfer(
                null,
                requestKey,
                sourcePlanId,
                targetPlanId,
                amount,
                BudgetTransferStatus.REQUESTED,
                requestedBy,
                null);
    }

    public static BudgetTransfer restore(
            Long id,
            String requestKey,
            Long sourcePlanId,
            Long targetPlanId,
            BigDecimal amount,
            BudgetTransferStatus status,
            String requestedBy,
            String approvedBy) {
        if (id == null) {
            throw new IllegalArgumentException("복원하는 BudgetTransfer의 id는 필수입니다.");
        }
        return new BudgetTransfer(
                id, requestKey, sourcePlanId, targetPlanId, amount, status, requestedBy, approvedBy);
    }

    public void approve(String actor) {
        if (status != BudgetTransferStatus.REQUESTED) {
            throw new IllegalStateException("요청 상태의 전용만 승인할 수 있습니다.");
        }
        status = BudgetTransferStatus.APPROVED;
        approvedBy = BudgetPlan.requireText(actor, "actor");
    }

    public boolean hasSameRequest(
            Long sourceId, Long targetId, BigDecimal requestedAmount, String requester) {
        return sourcePlanId.equals(sourceId)
                && targetPlanId.equals(targetId)
                && amount.compareTo(BudgetPrecision.positive(requestedAmount, "amount")) == 0
                && requestedBy.equals(BudgetPlan.requireText(requester, "requester"));
    }

    public boolean hasSameRequest(Long sourceId, Long targetId, BigDecimal requestedAmount) {
        return sourcePlanId.equals(sourceId)
                && targetPlanId.equals(targetId)
                && amount.compareTo(BudgetPrecision.positive(requestedAmount, "amount")) == 0;
    }

    private static Long validateOptionalId(Long id, String fieldName) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException(fieldName + "는 양수여야 합니다.");
        }
        return id;
    }

    private static Long validateRequiredId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + "는 양수여야 합니다.");
        }
        return id;
    }

    public Long id() {
        return id;
    }

    public String requestKey() {
        return requestKey;
    }

    public Long sourcePlanId() {
        return sourcePlanId;
    }

    public Long targetPlanId() {
        return targetPlanId;
    }

    public BigDecimal amount() {
        return amount;
    }

    public BudgetTransferStatus status() {
        return status;
    }

    public String requestedBy() {
        return requestedBy;
    }

    public String approvedBy() {
        return approvedBy;
    }

    public Long getId() {
        return id();
    }

    public String getRequestKey() {
        return requestKey();
    }

    public Long getSourcePlanId() {
        return sourcePlanId();
    }

    public Long getTargetPlanId() {
        return targetPlanId();
    }

    public BigDecimal getAmount() {
        return amount();
    }

    public BudgetTransferStatus getStatus() {
        return status();
    }

    public String getRequestedBy() {
        return requestedBy();
    }

    public String getApprovedBy() {
        return approvedBy();
    }
}
