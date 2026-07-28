package com.ho.account.reporting.adapter.in.web.dto;

import com.ho.account.reporting.domain.model.DisclosureNoteMartEntry;
import com.ho.account.reporting.domain.model.FinancialStatement;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DisclosureNoteMartEntryResponseDto(
        String entryId,
        String martId,
        String statementId,
        FinancialStatement.StatementType statementType,
        LocalDateTime baseDate,
        String noteNumber,
        DisclosureNoteMartEntry.NoteCategory noteCategory,
        String sourceLineCode,
        String sourceLineLabel,
        DisclosureNoteMartEntry.MaturityBucket maturityBucket,
        DisclosureNoteMartEntry.RateType rateType,
        String currencyCode,
        DisclosureNoteMartEntry.RiskCategory riskCategory,
        BigDecimal currentAmount,
        BigDecimal previousAmount,
        String generatedBy,
        LocalDateTime generatedAt) {

    public static DisclosureNoteMartEntryResponseDto from(DisclosureNoteMartEntry entry) {
        return new DisclosureNoteMartEntryResponseDto(
                entry.getEntryId(),
                entry.getMartId(),
                entry.getStatementId(),
                entry.getStatementType(),
                entry.getBaseDate(),
                entry.getNoteNumber(),
                entry.getNoteCategory(),
                entry.getSourceLineCode(),
                entry.getSourceLineLabel(),
                entry.getMaturityBucket(),
                entry.getRateType(),
                entry.getCurrencyCode(),
                entry.getRiskCategory(),
                entry.getCurrentAmount(),
                entry.getPreviousAmount(),
                entry.getGeneratedBy(),
                entry.getGeneratedAt());
    }
}