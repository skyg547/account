package com.ho.account.reconciliation.application.port.in;

import java.time.LocalDateTime;
import java.util.Objects;

/** 대사 차이 담당자 배정 유즈케이스 입력값입니다. */
public record AssignDifferenceCommand(Long differenceId, String assignedToUser, LocalDateTime slaDueDate) {
    public AssignDifferenceCommand {
        if (differenceId == null || differenceId <= 0) {
            throw new IllegalArgumentException("differenceId must be greater than zero");
        }
        assignedToUser = requireText(assignedToUser, "assignedToUser");
        slaDueDate = Objects.requireNonNull(slaDueDate, "slaDueDate is required");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}