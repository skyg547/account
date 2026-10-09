package com.ho.account.closing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;

@Entity
@Table(name = "valuation_batches")
public class ValuationBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fiscal_period_id", nullable = false)
    private Long fiscalPeriodId;

    @Transient
    private String fiscalYear;

    @Transient
    private String fiscalPeriod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ValuationType valuationType;

    @Column(name = "execution_key", length = 100, unique = true)
    private String executionKey;

    @Column(name = "journal_count")
    private Integer journalCount;

    @Column(nullable = false)
    private LocalDateTime runDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ValuationBatchStatus status;

    @Column(name = "generated_journal_entry_id")
    private Long generatedJournalEntryId;

    @Column(length = 200)
    private String reportLink;

    @Column(length = 50)
    private String runBy;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    public enum ValuationType {
        FX_RATE,
        FINANCIAL_INSTRUMENT,
        INVENTORY
    }

    public enum ValuationBatchStatus {
        RUNNING,
        COMPLETED,
        FAILED,
        PENDING_APPROVAL,
        RECONCILIATION_REQUIRED
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ValuationBatchStatus.RUNNING;
        }
        if (this.auditUser == null) {
            this.auditUser = "SYSTEM";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFiscalPeriodId() {
        return fiscalPeriodId;
    }

    public void setFiscalPeriodId(Long fiscalPeriodId) {
        this.fiscalPeriodId = fiscalPeriodId;
    }

    public String getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(String fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public String getFiscalPeriod() {
        return fiscalPeriod;
    }

    public void setFiscalPeriod(String fiscalPeriod) {
        this.fiscalPeriod = fiscalPeriod;
    }

    public void assignFiscalPeriod(Long fiscalPeriodId, String fiscalYear, String fiscalPeriod) {
        this.fiscalPeriodId = fiscalPeriodId;
        this.fiscalYear = fiscalYear;
        this.fiscalPeriod = fiscalPeriod;
    }

    public ValuationType getValuationType() {
        return valuationType;
    }

    public String getExecutionKey() { return executionKey; }
    public void setExecutionKey(String executionKey) { this.executionKey = executionKey; }
    public Integer getJournalCount() { return journalCount; }
    public void setJournalCount(Integer journalCount) { this.journalCount = journalCount; }

    public void setValuationType(ValuationType valuationType) {
        this.valuationType = valuationType;
    }

    public LocalDateTime getRunDateTime() {
        return runDateTime;
    }

    public void setRunDateTime(LocalDateTime runDateTime) {
        this.runDateTime = runDateTime;
    }

    public ValuationBatchStatus getStatus() {
        return status;
    }

    public void setStatus(ValuationBatchStatus status) {
        this.status = status;
    }

    public Long getGeneratedJournalEntryId() {
        return generatedJournalEntryId;
    }

    public void setGeneratedJournalEntryId(Long generatedJournalEntryId) {
        this.generatedJournalEntryId = generatedJournalEntryId;
    }

    public String getReportLink() {
        return reportLink;
    }

    public void setReportLink(String reportLink) {
        this.reportLink = reportLink;
    }

    public String getRunBy() {
        return runBy;
    }

    public void setRunBy(String runBy) {
        this.runBy = runBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAuditUser() {
        return auditUser;
    }

    public void setAuditUser(String auditUser) {
        this.auditUser = auditUser;
    }
}
