package com.ho.account.report.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "REGULATORY_SUBMISSION")
public class RegulatorySubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "REPORT_CODE", nullable = false, length = 50)
    private String reportCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SNAPSHOT_ID", nullable = false)
    private ReportSnapshotHeader snapshot;

    @Column(name = "SUBMISSION_DATE", nullable = false)
    private LocalDate submissionDate;

    @Column(name = "SUBMITTER", nullable = false, length = 50)
    private String submitter;

    @Column(name = "SUBMISSION_CHANNEL", length = 50)
    private String submissionChannel;

    @Column(name = "RESPONSE_STATUS", length = 50)
    private String responseStatus;

    @Column(name = "CREATE_DATE", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Column(name = "AUDIT_USER", nullable = false, length = 50)
    private String auditUser;

    @PrePersist
    protected void onCreate() {
        this.createDate = LocalDateTime.now();
        if (this.auditUser == null)
            this.auditUser = "SYSTEM";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReportCode() {
        return reportCode;
    }

    public void setReportCode(String reportCode) {
        this.reportCode = reportCode;
    }

    public ReportSnapshotHeader getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(ReportSnapshotHeader snapshot) {
        this.snapshot = snapshot;
    }

    public LocalDate getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(LocalDate submissionDate) {
        this.submissionDate = submissionDate;
    }

    public String getSubmitter() {
        return submitter;
    }

    public void setSubmitter(String submitter) {
        this.submitter = submitter;
    }

    public String getSubmissionChannel() {
        return submissionChannel;
    }

    public void setSubmissionChannel(String submissionChannel) {
        this.submissionChannel = submissionChannel;
    }

    public String getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(String responseStatus) {
        this.responseStatus = responseStatus;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(LocalDateTime createDate) {
        this.createDate = createDate;
    }

    public String getAuditUser() {
        return auditUser;
    }

    public void setAuditUser(String auditUser) {
        this.auditUser = auditUser;
    }
}
