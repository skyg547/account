package com.ho.account.budget.api.dto;

import com.ho.account.budget.domain.BudgetTransfer;
import com.ho.account.budget.domain.BudgetTransferStatus;
import java.math.BigDecimal;

/** Stable HTTP projection of a budget transfer. */
public record BudgetTransferResponse(
        Long id,
        String requestKey,
        Long sourcePlanId,
        Long targetPlanId,
        BigDecimal amount,
        BudgetTransferStatus status,
        String requestedBy,
        String approvedBy) {

    public static BudgetTransferResponse from(BudgetTransfer transfer) {
        return new BudgetTransferResponse(
                transfer.id(),
                transfer.requestKey(),
                transfer.sourcePlanId(),
                transfer.targetPlanId(),
                transfer.amount(),
                transfer.status(),
                transfer.requestedBy(),
                transfer.approvedBy());
    }
}
