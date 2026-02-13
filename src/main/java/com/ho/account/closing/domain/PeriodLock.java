package com.ho.account.closing.domain;

import com.ho.account.basic.domain.FiscalPeriod;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 기간 잠금 (Period Lock) 엔티티
 * 특정 회계 기간에 대한 거래 입력 및 수정 방지를 위해 기간 잠금 상태를 관리합니다.
 */
@Entity
@Table(name = "period_locks")
public class PeriodLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_period_id", nullable = false)
    private FiscalPeriod fiscalPeriod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PeriodLockType lockType; // ALL_TRANSACTIONS, NON_ADJUSTMENT_ENTRIES 등

    @Column(length = 50)
    private String lockedBy;

    private LocalDateTime lockedAt;

    @Column(length = 1000)
    private String reason;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    public enum PeriodLockType {
        ALL_TRANSACTIONS, // 모든 거래 잠금 (완전 잠금)
        NON_ADJUSTMENT_ENTRIES, // 조정 전표를 제외한 모든 거래 잠금 (결산 조정 가능)
        PARTIAL_LOCK // 특정 모듈 또는 특정 사용자 그룹에 대한 잠금
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

    // Getters and Setters
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
