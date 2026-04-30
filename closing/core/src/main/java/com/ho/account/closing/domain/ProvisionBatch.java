package com.ho.account.closing.domain;

import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "provision_batches")
public class ProvisionBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_period_id", nullable = false)
    private FiscalPeriod fiscalPeriod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProvisionType provisionType;

    @Column(nullable = false)
    private LocalDateTime runDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProvisionBatchStatus status;

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

    public enum ProvisionType {
        BAD_DEBT,
        IMPAIRMENT,
        ECL,
        WARRANTY,
        RESTRUCTURING
    }

    public enum ProvisionBatchStatus {
        RUNNING,
        COMPLETED,
        FAILED,
        PENDING_APPROVAL
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ProvisionBatchStatus.RUNNING;
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

    public FiscalPeriod getFiscalPeriod() {
        return fiscalPeriod;
    }

    public void setFiscalPeriod(FiscalPeriod fiscalPeriod) {
        this.fiscalPeriod = fiscalPeriod;
    }

    public ProvisionType getProvisionType() {
        return provisionType;
    }

    public void setProvisionType(ProvisionType provisionType) {
        this.provisionType = provisionType;
    }

    public LocalDateTime getRunDateTime() {
        return runDateTime;
    }

    public void setRunDateTime(LocalDateTime runDateTime) {
        this.runDateTime = runDateTime;
    }

    public ProvisionBatchStatus getStatus() {
        return status;
    }

    public void setStatus(ProvisionBatchStatus status) {
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
