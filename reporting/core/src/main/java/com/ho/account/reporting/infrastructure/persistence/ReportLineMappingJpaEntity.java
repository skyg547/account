package com.ho.account.reporting.infrastructure.persistence;

import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.ReportLineMapping;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "rpt_line_mapping")
class ReportLineMappingJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "statement_type", nullable = false, length = 40)
    private String statementType;

    @Column(name = "line_code", nullable = false, length = 80)
    private String lineCode;

    @Column(name = "label", nullable = false, length = 200)
    private String label;

    @Column(name = "account_code", nullable = false, length = 40)
    private String accountCode;

    @Column(name = "note_number", length = 40)
    private String noteNumber;

    @Column(name = "line_level", nullable = false)
    private int lineLevel;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    protected ReportLineMappingJpaEntity() {
    }

    ReportLineMappingJpaEntity(
            FinancialStatement.StatementType statementType,
            String lineCode,
            String label,
            String accountCode,
            String noteNumber,
            int lineLevel,
            int displayOrder,
            LocalDate validFrom,
            LocalDate validTo) {
        this.statementType = statementType.name();
        this.lineCode = lineCode;
        this.label = label;
        this.accountCode = accountCode;
        this.noteNumber = noteNumber;
        this.lineLevel = lineLevel;
        this.displayOrder = displayOrder;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    ReportLineMapping main(List<String> accountCodes) {
        return new ReportLineMapping(
                FinancialStatement.StatementType.valueOf(statementType),
                lineCode,
                label,
                accountCodes,
                noteNumber,
                lineLevel,
                displayOrder,
                validFrom,
                validTo);
    }

    String getLineCode() {
        return lineCode;
    }

    String getLabel() {
        return label;
    }

    String getAccountCode() {
        return accountCode;
    }

    String getNoteNumber() {
        return noteNumber;
    }

    int getLineLevel() {
        return lineLevel;
    }

    int getDisplayOrder() {
        return displayOrder;
    }

    LocalDate getValidFrom() {
        return validFrom;
    }

    LocalDate getValidTo() {
        return validTo;
    }
}
