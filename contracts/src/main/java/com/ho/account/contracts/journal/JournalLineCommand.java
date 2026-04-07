package com.ho.account.contracts.journal;

import java.math.BigDecimal;

public record JournalLineCommand(
        String drcrType,
        String accountCode,
        BigDecimal amount,
        BigDecimal baseAmount,
        String departmentCode,
        String businessPartnerCode,
        String detailDescription) {
}
