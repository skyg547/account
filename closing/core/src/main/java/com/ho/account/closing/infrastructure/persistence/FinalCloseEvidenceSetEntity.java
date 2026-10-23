package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.domain.FinalCloseEvidenceControl;
import com.ho.account.closing.domain.FinalCloseEvidenceSet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.ho.account.contracts.journal.JournalPeriodCheckpoint;
import com.ho.account.contracts.journal.JournalPeriodScope;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "final_close_evidence_sets")
class FinalCloseEvidenceSetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evidence_set_id", nullable = false, length = 100, updatable = false)
    private String evidenceSetId;

    @Column(name = "calendar_id", nullable = false, updatable = false)
    private Long calendarId;

    @Column(name = "fiscal_period_id", nullable = false, updatable = false)
    private Long fiscalPeriodId;

    @Column(name = "fiscal_year", nullable = false, length = 10, updatable = false)
    private String fiscalYear;

    @Column(name = "fiscal_period", nullable = false, length = 20, updatable = false)
    private String fiscalPeriod;

    @Column(name = "ledger_cutoff", nullable = false, updatable = false)
    private LocalDate ledgerCutoff;

    @Column(name = "observed_at", nullable = false, updatable = false)
    private Instant observedAt;

    @Column(name = "submitted_by", nullable = false, length = 100, updatable = false)
    private String submittedBy;

    @Column(name = "content_digest", nullable = false, length = 64, updatable = false)
    private String contentDigest;

    @Column(name = "journal_revision", updatable = false)
    private Long journalRevision;
    @Column(name = "journal_start_date", updatable = false)
    private LocalDate journalStartDate;
    @Column(name = "journal_end_date", updatable = false)
    private LocalDate journalEndDate;

    protected FinalCloseEvidenceSetEntity() {
    }

    FinalCloseEvidenceSetEntity(FinalCloseEvidenceSet evidenceSet) {
        this.evidenceSetId = evidenceSet.evidenceSetId();
        this.calendarId = evidenceSet.calendarId();
        this.fiscalPeriodId = evidenceSet.fiscalPeriodId();
        this.fiscalYear = evidenceSet.fiscalYear();
        this.fiscalPeriod = evidenceSet.fiscalPeriod();
        this.ledgerCutoff = evidenceSet.ledgerCutoff();
        this.observedAt = evidenceSet.observedAt();
        this.submittedBy = evidenceSet.submittedBy();
        this.contentDigest = evidenceSet.contentDigest();
        if (evidenceSet.journalCheckpoint() != null) {
            this.journalRevision = evidenceSet.journalCheckpoint().revision();
            this.journalStartDate = evidenceSet.journalCheckpoint().scope().startDate();
            this.journalEndDate = evidenceSet.journalCheckpoint().scope().endDate();
        }
    }

    Long getId() {
        return id;
    }

    String getEvidenceSetId() {
        return evidenceSetId;
    }

    String getContentDigest() {
        return contentDigest;
    }

    FinalCloseEvidenceSet toDomain(List<FinalCloseEvidenceControl> controls) {
        return new FinalCloseEvidenceSet(
                evidenceSetId,
                calendarId,
                fiscalPeriodId,
                fiscalYear,
                fiscalPeriod,
                ledgerCutoff,
                observedAt,
                submittedBy,
                contentDigest,
                controls, journalRevision == null ? null : new JournalPeriodCheckpoint(
                        new JournalPeriodScope(fiscalPeriodId, journalStartDate, journalEndDate), journalRevision));
    }
}
