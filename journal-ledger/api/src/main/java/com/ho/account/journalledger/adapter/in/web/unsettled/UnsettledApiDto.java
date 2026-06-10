package com.ho.account.journalledger.adapter.in.web.unsettled;

import com.ho.account.journalledger.domain.unsettled.UnsettledItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

final class UnsettledApiDto {

    private UnsettledApiDto() {
    }

    record SettlementRequest(
            @NotNull @Positive BigDecimal amount,
            @NotBlank String settlementReference) {
    }

    record View(
            Long id,
            String managementNo,
            String accountCode,
            String businessPartnerCode,
            LocalDate occurrenceDate,
            BigDecimal originalAmount,
            BigDecimal settledAmount,
            BigDecimal remainingAmount,
            String status,
            boolean resolved,
            String lastSettledBy,
            String lastSettlementReference) {

        static View from(UnsettledItem item) {
            return new View(
                    item.getId(),
                    item.getManagementNo(),
                    item.getAccountCode(),
                    item.getBusinessPartnerCode(),
                    item.getOccurrenceDate(),
                    item.getOriginalAmount(),
                    item.getSettledAmount(),
                    item.getRemainingAmount(),
                    item.getStatus(),
                    item.isResolved(),
                    item.getLastSettledBy(),
                    item.getLastSettlementReference());
        }
    }
}
