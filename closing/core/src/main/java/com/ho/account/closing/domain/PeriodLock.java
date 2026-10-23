package com.ho.account.closing.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Period lock for a fiscal period.
 * Stores the fiscal period as an ID value to keep closing independent from master-data entities.
 */

public class PeriodLock {

    private Long id;

    private Long fiscalPeriodId;

    private boolean active = true;

    // A nullable unique slot lets released rows keep their original fiscal period.
    private Long activeFiscalPeriodId;
    private String fiscalYear;

    private String fiscalPeriod;

    private PeriodLockType lockType;

    private String lockedBy;

    private LocalDateTime lockedAt;

    private String unlockedBy;

    private LocalDateTime unlockedAt;
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
        if (active) {
            this.activeFiscalPeriodId = fiscalPeriodId;
        }
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
        setFiscalPeriodId(fiscalPeriodId);
        this.fiscalYear = fiscalYear;
        this.fiscalPeriod = fiscalPeriod;
    }

    public static PeriodLock createActive(
            Long fiscalPeriodId, String fiscalYear, String fiscalPeriod,
            PeriodLockType lockType, String actor, String reason) {
        if (fiscalPeriodId == null || fiscalPeriodId <= 0) {
            throw new IllegalArgumentException("fiscalPeriodId must be positive");
        }
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("actor must not be blank");
        }
        PeriodLock lock = new PeriodLock();
        lock.assignFiscalPeriod(fiscalPeriodId, fiscalYear, fiscalPeriod);
        lock.lockType = Objects.requireNonNull(lockType, "lockType must not be null");
        lock.lockedBy = actor;
        lock.lockedAt = LocalDateTime.now();
        lock.reason = reason;
        lock.auditUser = actor;
        return lock;
    }

    public void release(String actor) {
        if (!active) {
            throw new IllegalStateException("Fiscal period lock is already released.");
        }
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("actor must not be blank");
        }
        active = false;
        activeFiscalPeriodId = null;
        unlockedBy = actor;
        unlockedAt = LocalDateTime.now();
        auditUser = actor;
    }

    public boolean isActive() {
        return active;
    }

    /** Restores persisted lock state without running release or replacing its audit timestamps. */
    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getActiveFiscalPeriodId() {
        return activeFiscalPeriodId;
    }

    public void setActiveFiscalPeriodId(Long activeFiscalPeriodId) {
        this.activeFiscalPeriodId = activeFiscalPeriodId;
    }

    public String getUnlockedBy() {
        return unlockedBy;
    }

    public void setUnlockedBy(String unlockedBy) {
        this.unlockedBy = unlockedBy;
    }

    public LocalDateTime getUnlockedAt() {
        return unlockedAt;
    }

    public void setUnlockedAt(LocalDateTime unlockedAt) {
        this.unlockedAt = unlockedAt;
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
