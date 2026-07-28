package com.ho.account.journalledger.adapter.in.web.journal;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

final class JournalApiDto {

    private JournalApiDto() {
    }

    record CreateRequest(
            @NotNull LocalDate slipDate,
            LocalDate accountingDate,
            @NotBlank String description,
            String entryType,
            String currencyCode,
            BigDecimal exchangeRate,
            String lineageSourceType,
            String lineageSourceId,
            @NotEmpty List<@Valid LineRequest> lines) {

        JournalEntry main(String actor) {
            JournalEntry entry = new JournalEntry();
            entry.setSlipDate(slipDate);
            entry.setAccountingDate(accountingDate);
            entry.setDescription(description);
            entry.setEntryType(entryType);
            entry.setCurrencyCode(currencyCode);
            entry.setExchangeRate(exchangeRate);
            entry.setLineageSourceType(lineageSourceType);
            entry.setLineageSourceId(lineageSourceId);
            entry.setCreatedBy(requireActor(actor));
            lines.forEach(line -> entry.addDetail(line.main(actor)));
            return entry;
        }
    }

    record LineRequest(
            @NotNull JournalSide side,
            @NotBlank String accountCode,
            @NotNull @Positive BigDecimal amount,
            @NotNull @Positive BigDecimal baseAmount,
            String departmentCode,
            String businessPartnerCode,
            String description) {

        JournalDetail main(String actor) {
            JournalDetail detail = new JournalDetail();
            detail.setSide(side);
            detail.setAccountCode(accountCode);
            detail.setAmount(amount);
            detail.setBaseAmount(baseAmount);
            detail.setDepartmentCode(departmentCode);
            detail.setBusinessPartnerCode(businessPartnerCode);
            detail.setDetailDescription(description);
            detail.setAuditUser(requireActor(actor));
            return detail;
        }
    }

    record EventRequest(@NotNull Map<String, Object> eventData) {
        Map<String, Object> eventDataWithActor(String actor) {
            Map<String, Object> enriched = new java.util.HashMap<>(eventData);
            enriched.putIfAbsent("createdBy", requireActor(actor));
            enriched.putIfAbsent("auditUser", requireActor(actor));
            return enriched;
        }
    }

    record View(
            Long id,
            String slipNo,
            LocalDate slipDate,
            LocalDate accountingDate,
            String description,
            String status,
            String entryType,
            String currencyCode,
            BigDecimal exchangeRate,
            String createdBy,
            String auditUser,
            String lineageSourceType,
            String lineageSourceId,
            List<LineView> lines) {

        static View from(JournalEntry entry) {
            return new View(
                    entry.getId(),
                    entry.getSlipNo(),
                    entry.getSlipDate(),
                    entry.getAccountingDate(),
                    entry.getDescription(),
                    entry.getStatus() == null ? null : entry.getStatus().name(),
                    entry.getEntryType(),
                    entry.getCurrencyCode(),
                    entry.getExchangeRate(),
                    entry.getCreatedBy(),
                    entry.getAuditUser(),
                    entry.getLineageSourceType(),
                    entry.getLineageSourceId(),
                    entry.getDetails().stream().map(LineView::from).toList());
        }
    }

    record LineView(
            Long id,
            String side,
            String accountCode,
            BigDecimal amount,
            BigDecimal baseAmount,
            String departmentCode,
            String businessPartnerCode,
            String description) {

        static LineView from(JournalDetail detail) {
            return new LineView(
                    detail.getId(),
                    detail.getSide() == null ? null : detail.getSide().name(),
                    detail.getAccountCode(),
                    detail.getAmount(),
                    detail.getBaseAmount(),
                    detail.getDepartmentCode(),
                    detail.getBusinessPartnerCode(),
                    detail.getDetailDescription());
        }
    }

    private static String requireActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("X-User-ID is required");
        }
        return actor.trim();
    }
}
