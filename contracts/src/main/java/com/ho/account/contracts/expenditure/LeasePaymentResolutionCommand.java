package com.ho.account.contracts.expenditure;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LeasePaymentResolutionCommand(
        String title,
        LocalDate resolutionDate,
        LocalDate paymentDate,
        String departmentCode,
        String accountCode,
        String businessPartnerCode,
        BigDecimal amount,
        String detailDescription
) {
}
