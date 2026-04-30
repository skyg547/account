package com.ho.account.closing.domain;

import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 湲곌컙 ?좉툑 (Period Lock) ?뷀떚??
 * ?뱀젙 ?뚭퀎 湲곌컙?????嫄곕옒 ?낅젰 諛??섏젙 諛⑹?瑜??꾪빐 湲곌컙 ?좉툑 ?곹깭瑜?愿由ы빀?덈떎.
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
    private PeriodLockType lockType; // ALL_TRANSACTIONS, NON_ADJUSTMENT_ENTRIES ??

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
        ALL_TRANSACTIONS, // 紐⑤뱺 嫄곕옒 ?좉툑 (?꾩쟾 ?좉툑)
        NON_ADJUSTMENT_ENTRIES, // 議곗젙 ?꾪몴瑜??쒖쇅??紐⑤뱺 嫄곕옒 ?좉툑 (寃곗궛 議곗젙 媛??
        PARTIAL_LOCK // ?뱀젙 紐⑤뱢 ?먮뒗 ?뱀젙 ?ъ슜??洹몃９??????좉툑
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

    // Getter 諛?Setter
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
