package com.ho.account.reconciliation.api.dto;

import java.math.BigDecimal;
import java.util.Objects;

/** Transaction row shown on the inter-branch banking dashboard. */
public record InterBranchTransactionResponse(
        Long id,
        String sourceBranch,
        String targetBranch,
        String transactionType,
        BigDecimal amount,
        Status status) {

    public InterBranchTransactionResponse {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(sourceBranch, "sourceBranch must not be null");
        Objects.requireNonNull(targetBranch, "targetBranch must not be null");
        Objects.requireNonNull(transactionType, "transactionType must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }

    public enum Status {
        MATCHED,
        DISCREPANCY,
        PENDING
    }
}
