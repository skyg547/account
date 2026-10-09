package com.ho.account.closing.domain;

import java.time.LocalDateTime;

/**
 * Period lock for a fiscal period.
 * Stores the fiscal period as an ID value to keep closing independent from master-data entities.
 */

public class PeriodLock {

    private Long id;

    private Long fiscalPeriodId;

    private String fiscalYear;

    private String fiscalPeriod;

    private PeriodLockType lockType;

    private String lockedBy;

    private LocalDateTime lockedAt;

    private String reason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String auditUser;

    public enum PeriodLockType {
        ALL_TRANSACTIONS,
        NON_ADJUSTMENT_ENTRIES,
        PARTIAL_LOCK
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

    public PeriodLockType getLockType() {
        return lockType;
    }

    public void setLockType(PeriodLockType lockType) {
        this.lockType = lockType;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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
