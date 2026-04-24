package com.ho.account.common.adapter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 紐⑤?由ъ뒪 ?꾪몴 湲곗엯 紐낅졊 (Record)
 */
public record MonolithJournalPostingCommand(
        LocalDate accountingDate,
        String description,
        String currencyCode,
        List<JournalLine> lines
) {
    public record JournalLine(
            String accountCode,
            BigDecimal debitAmount,
            BigDecimal creditAmount,
            String departmentCode,
            String businessPartnerCode,
            String description
    ) {}
}
