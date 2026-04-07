package com.ho.account.reconciliation.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "RECONCILIATION_RESULTS")
public class ReconciliationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "RECONCILIATION_DATE", nullable = false)
    private LocalDate reconciliationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "RECONCILIATION_TYPE", nullable = false, length = 50)
    private ReconciliationType reconciliationType;

    @Column(name = "CLOSING_PERIOD_ID")
    private Long closingPeriodId;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 50)
    private ReconciliationStatus status;

    @Column(name = "TOTAL_COUNT_SOURCE", nullable = false)
    private Long totalCountSource;

    @Column(name = "TOTAL_AMOUNT_SOURCE", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmountSource;

    @Column(name = "TOTAL_COUNT_TARGET", nullable = false)
    private Long totalCountTarget;

    @Column(name = "TOTAL_AMOUNT_TARGET", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmountTarget;

    @Column(name = "VARIANCE_COUNT", nullable = false)
    private Long varianceCount;

    @Column(name = "VARIANCE_AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal varianceAmount;

    @Column(name = "RUN_BY", length = 50)
    private String runBy;

    @Column(name = "RUN_AT")
    private LocalDateTime runAt;

    @Column(name = "CREATE_DATE", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Column(name = "UPDATE_DATE", nullable = false)
    private LocalDateTime updateDate;

    @Column(name = "AUDIT_USER", nullable = false, length = 50)
    private String auditUser;

    @PrePersist
    protected void onCreate() {
        this.createDate = LocalDateTime.now();
        this.updateDate = LocalDateTime.now();
        if (this.auditUser == null) {
            this.auditUser = "SYSTEM";
        }
        if (this.runAt == null) {
            this.runAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateDate = LocalDateTime.now();
    }

    // Getter 및 Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getReconciliationDate() {
        return reconciliationDate;
    }

    public void setReconciliationDate(LocalDate reconciliationDate) {
        this.reconciliationDate = reconciliationDate;
    }

    public ReconciliationType getReconciliationType() {
        return reconciliationType;
    }

    public void setReconciliationType(ReconciliationType reconciliationType) {
        this.reconciliationType = reconciliationType;
    }

    public Long getClosingPeriodId() {
        return closingPeriodId;
    }

    public void setClosingPeriodId(Long closingPeriodId) {
        this.closingPeriodId = closingPeriodId;
    }

    public ReconciliationStatus getStatus() {
        return status;
    }

    public void setStatus(ReconciliationStatus status) {
        this.status = status;
    }

    public Long getTotalCountSource() {
        return totalCountSource;
    }

    public void setTotalCountSource(Long totalCountSource) {
        this.totalCountSource = totalCountSource;
    }

    public BigDecimal getTotalAmountSource() {
        return totalAmountSource;
    }

    public void setTotalAmountSource(BigDecimal totalAmountSource) {
        this.totalAmountSource = totalAmountSource;
    }

    public Long getTotalCountTarget() {
        return totalCountTarget;
    }

    public void setTotalCountTarget(Long totalCountTarget) {
        this.totalCountTarget = totalCountTarget;
    }

    public BigDecimal getTotalAmountTarget() {
        return totalAmountTarget;
    }

    public void setTotalAmountTarget(BigDecimal totalAmountTarget) {
        this.totalAmountTarget = totalAmountTarget;
    }

    public Long getVarianceCount() {
        return varianceCount;
    }

    public void setVarianceCount(Long varianceCount) {
        this.varianceCount = varianceCount;
    }

    public BigDecimal getVarianceAmount() {
        return varianceAmount;
    }

    public void setVarianceAmount(BigDecimal varianceAmount) {
        this.varianceAmount = varianceAmount;
    }

    public String getRunBy() {
        return runBy;
    }

    public void setRunBy(String runBy) {
        this.runBy = runBy;
    }

    public LocalDateTime getRunAt() {
        return runAt;
    }

    public void setRunAt(LocalDateTime runAt) {
        this.runAt = runAt;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(LocalDateTime createDate) {
        this.createDate = createDate;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }

    public String getAuditUser() {
        return auditUser;
    }

    public void setAuditUser(String auditUser) {
        this.auditUser = auditUser;
    }
}
