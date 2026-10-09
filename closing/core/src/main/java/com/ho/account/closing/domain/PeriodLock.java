package com.ho.account.closing.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Period lock for a fiscal period.
 * Stores the fiscal period as an ID value to keep closing independent from master-data entities.
 */
@Entity
@Table(name = "period_locks")
public class PeriodLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fiscal_period_id", nullable = false)
    private Long fiscalPeriodId;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    // A nullable unique slot lets released rows keep their original fiscal period.
    @Column(name = "active_fiscal_period_id")
    private Long activeFiscalPeriodId;

    @Transient
    private String fiscalYear;

    @Transient
    private String fiscalPeriod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PeriodLockType lockType;

    @Column(length = 50)
    private String lockedBy;

    private LocalDateTime lockedAt;

    @Column(name = "unlocked_by", length = 50)
    private String unlockedBy;

    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;

    @Column(length = 1000)
    private String reason;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    public enum PeriodLockType {
        ALL_TRANSACTIONS,
        NON_ADJUSTMENT_ENTRIES,
        PARTIAL_LOCK
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.auditUser == null)
            this.auditUser = "SYSTEM";
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

    public Long getActiveFiscalPeriodId() {
        return activeFiscalPeriodId;
    }

    public String getUnlockedBy() {
        return unlockedBy;
    }

    public LocalDateTime getUnlockedAt() {
        return unlockedAt;
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
