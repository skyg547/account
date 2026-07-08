package com.ho.account.reconciliation.application.port.in;

import java.time.LocalDate;
import java.util.Objects;

/** 대사 실행 유즈케이스 입력값입니다. */
public record RunReconciliationCommand(Long reconciliationUnitId, LocalDate reconciliationDate, String runBy) {
    public RunReconciliationCommand {
        if (reconciliationUnitId == null || reconciliationUnitId <= 0) {
            throw new IllegalArgumentException("reconciliationUnitId must be greater than zero");
        }
        reconciliationDate = Objects.requireNonNull(reconciliationDate, "reconciliationDate is required");
        runBy = requireText(runBy, "runBy");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}