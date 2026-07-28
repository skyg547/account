package com.ho.account.reporting.infrastructure.persistence;

import com.ho.account.reporting.domain.model.DisclosureNoteMartEntry;
import com.ho.account.reporting.domain.model.DisclosureNoteMartEntry.MaturityBucket;
import com.ho.account.reporting.domain.model.DisclosureNoteMartEntry.NoteCategory;
import com.ho.account.reporting.domain.model.DisclosureNoteMartEntry.RateType;
import com.ho.account.reporting.domain.model.DisclosureNoteMartEntry.RiskCategory;
import com.ho.account.reporting.domain.model.FinancialStatement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rpt_disclosure_note_mart")
class DisclosureNoteMartJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_id", nullable = false, length = 80, unique = true)
    private String entryId;

    @Column(name = "mart_id", nullable = false, length = 80)
    private String martId;

    @Column(name = "statement_id", nullable = false, length = 80)
    private String statementId;

    @Column(name = "statement_type", nullable = false, length = 40)
    private String statementType;

    @Column(name = "base_date", nullable = false)
    private LocalDateTime baseDate;

    @Column(name = "note_number", nullable = false, length = 40)
    private String noteNumber;

    @Column(name = "note_category", nullable = false, length = 40)
    private String noteCategory;

    @Column(name = "source_line_code", nullable = false, length = 80)
    private String sourceLineCode;

    @Column(name = "source_line_label", nullable = false, length = 200)
    private String sourceLineLabel;

    @Column(name = "maturity_bucket", nullable = false, length = 40)
    private String maturityBucket;

    @Column(name = "rate_type", nullable = false, length = 40)
    private String rateType;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "risk_category", nullable = false, length = 40)
    private String riskCategory;

    @Column(name = "current_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentAmount;

    @Column(name = "previous_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal previousAmount;

    @Column(name = "generated_by", nullable = false, length = 80)
    private String generatedBy;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    protected DisclosureNoteMartJpaEntity() {
    }

    static DisclosureNoteMartJpaEntity from(DisclosureNoteMartEntry entry) {
        DisclosureNoteMartJpaEntity entity = new DisclosureNoteMartJpaEntity();
        entity.entryId = entry.getEntryId();
        entity.martId = entry.getMartId();
        entity.statementId = entry.getStatementId();
        entity.statementType = entry.getStatementType().name();
        entity.baseDate = entry.getBaseDate();
        entity.noteNumber = entry.getNoteNumber();
        entity.noteCategory = entry.getNoteCategory().name();
        entity.sourceLineCode = entry.getSourceLineCode();
        entity.sourceLineLabel = entry.getSourceLineLabel();
        entity.maturityBucket = entry.getMaturityBucket().name();
        entity.rateType = entry.getRateType().name();
        entity.currencyCode = entry.getCurrencyCode();
        entity.riskCategory = entry.getRiskCategory().name();
        entity.currentAmount = entry.getCurrentAmount();
        entity.previousAmount = entry.getPreviousAmount();
        entity.generatedBy = entry.getGeneratedBy();
        entity.generatedAt = entry.getGeneratedAt();
        return entity;
    }

    DisclosureNoteMartEntry main() {
        return new DisclosureNoteMartEntry(
                entryId,
                martId,
                statementId,
                FinancialStatement.StatementType.valueOf(statementType),
                baseDate,
                noteNumber,
                NoteCategory.valueOf(noteCategory),
                sourceLineCode,
                sourceLineLabel,
                MaturityBucket.valueOf(maturityBucket),
                RateType.valueOf(rateType),
                currencyCode,
                RiskCategory.valueOf(riskCategory),
                currentAmount,
                previousAmount,
                generatedBy,
                generatedAt);
    }
}
