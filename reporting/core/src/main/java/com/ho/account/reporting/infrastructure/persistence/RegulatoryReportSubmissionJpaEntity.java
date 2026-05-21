package com.ho.account.reporting.infrastructure.persistence;

import com.ho.account.reporting.domain.model.RegulatoryReportSubmission;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "rpt_regulatory_submission",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_rpt_regulatory_submission_version",
                columnNames = {"statement_type", "base_date", "submission_version"}))
class RegulatoryReportSubmissionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submission_id", nullable = false, length = 80, unique = true)
    private String submissionId;

    @Column(name = "statement_id", nullable = false, length = 80)
    private String statementId;

    @Column(name = "statement_type", nullable = false, length = 40)
    private String statementType;

    @Column(name = "base_date", nullable = false)
    private LocalDateTime baseDate;

    @Column(name = "submission_version", nullable = false)
    private int submissionVersion;

    @Column(name = "submitted_by", nullable = false, length = 80)
    private String submittedBy;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "correction_reason", length = 500)
    private String correctionReason;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "validation_messages", length = 2000)
    private String validationMessages;

    protected RegulatoryReportSubmissionJpaEntity() {
    }

    static RegulatoryReportSubmissionJpaEntity from(RegulatoryReportSubmission submission) {
        RegulatoryReportSubmissionJpaEntity entity = new RegulatoryReportSubmissionJpaEntity();
        entity.submissionId = submission.getSubmissionId();
        entity.statementId = submission.getStatementId();
        entity.statementType = submission.getStatementType().name();
        entity.baseDate = submission.getBaseDate();
        entity.submissionVersion = submission.getVersion();
        entity.submittedBy = submission.getSubmittedBy();
        entity.submittedAt = submission.getSubmittedAt();
        entity.correctionReason = submission.getCorrectionReason();
        entity.status = submission.getStatus().name();
        entity.validationMessages = String.join("\n", submission.getValidationMessages());
        return entity;
    }
}
