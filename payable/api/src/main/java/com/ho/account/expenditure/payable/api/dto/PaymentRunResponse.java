package com.ho.account.expenditure.payable.api.dto;

import com.ho.account.expenditure.domain.PaymentRun;
import com.ho.account.expenditure.domain.PaymentRunStatus;
import java.time.LocalDate;

/** 지급 런 API 응답 DTO입니다. */
public record PaymentRunResponse(
        Long id,
        LocalDate runDate,
        String description,
        PaymentRunStatus status,
        String createdBy
) {
    public static PaymentRunResponse from(PaymentRun paymentRun) {
        return new PaymentRunResponse(
                paymentRun.getId(),
                paymentRun.getRunDate(),
                paymentRun.getDescription(),
                paymentRun.getStatus(),
                paymentRun.getCreatedBy());
    }
}