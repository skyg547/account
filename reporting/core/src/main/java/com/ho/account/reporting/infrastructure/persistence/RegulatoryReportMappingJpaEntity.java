package com.ho.account.reporting.infrastructure.persistence;

import com.ho.account.reporting.domain.model.DisclosureNoteMartEntry.NoteCategory;
import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.RegulatoryReportMapping;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "rpt_regulatory_report_mapping")
class RegulatoryReportMappingJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "statement_type", nullable = false, length = 40)
    private String statementType;

    @Column(name = "target_agency", nullable = false, length = 40)
    private String targetAgency;

    @Column(name = "report_code", nullable = false, length = 80)
    private String reportCode;

    @Column(name = "field_code", nullable = false, length = 80)
    private String fieldCode;

    @Column(name = "field_label", nullable = false, length = 200)
    private String fieldLabel;

    @Column(name = "source_note_category", nullable = false, length = 40)
    private String sourceNoteCategory;

    @Column(name = "source_note_number", length = 40)
    private String sourceNoteNumber;

    @Column(name = "source_line_code", length = 80)
    private String sourceLineCode;

    @Column(name = "required", nullable = false)
    private boolean required;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    protected RegulatoryReportMappingJpaEntity() {
    }

    RegulatoryReportMapping main() {
        return new RegulatoryReportMapping(
                FinancialStatement.StatementType.valueOf(statementType),
                targetAgency,
                reportCode,
                fieldCode,
                fieldLabel,
                NoteCategory.valueOf(sourceNoteCategory),
                sourceNoteNumber,
                sourceLineCode,
                required,
                displayOrder,
                validFrom,
                validTo);
    }
}
