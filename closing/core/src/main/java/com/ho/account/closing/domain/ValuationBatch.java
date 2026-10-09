package com.ho.account.closing.domain;

import java.time.LocalDateTime;

public class ValuationBatch {

    private Long id;

    private Long fiscalPeriodId;

    private String fiscalYear;

    private String fiscalPeriod;

    private ValuationType valuationType;

    private LocalDateTime runDateTime;

    private ValuationBatchStatus status;

    private Long generatedJournalEntryId;

    private String reportLink;

    private String runBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

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
        PENDING_APPROVAL
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
