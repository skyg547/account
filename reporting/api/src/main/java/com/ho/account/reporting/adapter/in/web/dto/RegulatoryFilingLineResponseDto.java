package com.ho.account.reporting.adapter.in.web.dto;

import com.ho.account.reporting.domain.model.RegulatoryFilingLine;
import java.math.BigDecimal;

public record RegulatoryFilingLineResponseDto(
        String reportCode,
        String fieldCode,
        String fieldLabel,
        String sourceNoteNumber,
        String sourceLineCode,
        String sourceLineLabel,
        BigDecimal currentAmount,
        BigDecimal previousAmount,
        int displayOrder) {

    public static RegulatoryFilingLineResponseDto from(RegulatoryFilingLine line) {
        return new RegulatoryFilingLineResponseDto(
                line.reportCode(),
                line.fieldCode(),
                line.fieldLabel(),
                line.sourceNoteNumber(),
                line.sourceLineCode(),
                line.sourceLineLabel(),
                line.currentAmount(),
                line.previousAmount(),
                line.displayOrder());
    }
}