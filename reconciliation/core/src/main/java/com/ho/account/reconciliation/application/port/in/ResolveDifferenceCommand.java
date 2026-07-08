package com.ho.account.reconciliation.application.port.in;

import com.ho.account.reconciliation.domain.ReconciliationDifference;
import java.util.Objects;

/** 대사 차이 해결/무시 확정 유즈케이스 입력값입니다. */
public record ResolveDifferenceCommand(
        Long differenceId,
        Long reasonCodeId,
        Long adjustmentJournalEntryId,
        ReconciliationDifference.ReconciliationDifferenceStatus status,
        String resolvedBy
) {
    public ResolveDifferenceCommand {
        if (differenceId == null || differenceId <= 0) {
            throw new IllegalArgumentException("differenceId must be greater than zero");
        }
        if (reasonCodeId == null || reasonCodeId <= 0) {
            throw new IllegalArgumentException("reasonCodeId must be greater than zero");
        }
        if (adjustmentJournalEntryId != null && adjustmentJournalEntryId <= 0) {
            throw new IllegalArgumentException("adjustmentJournalEntryId must be greater than zero when supplied");
        }
        status = Objects.requireNonNull(status, "status is required");
        resolvedBy = requireText(resolvedBy, "resolvedBy");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}