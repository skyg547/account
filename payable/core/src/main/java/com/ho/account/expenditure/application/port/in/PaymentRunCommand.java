package com.ho.account.expenditure.application.port.in;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 지급 런 생성 유즈케이스 입력값입니다.
 *
 * <p>초보자용 설명: 지급 런은 특정 기준일에 지급 대상 채무를 모으는 업무 실행 단위입니다.
 * API와 Batch가 같은 command를 사용하면 실행일, 생성자, 설명의 의미가 한 곳에서 유지됩니다.</p>
 */
public record PaymentRunCommand(LocalDate runDate, String description, String createdBy) {
    public PaymentRunCommand {
        runDate = Objects.requireNonNull(runDate, "runDate is required");
        createdBy = requireText(createdBy, "createdBy");
        if (description != null) {
            description = description.trim();
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}