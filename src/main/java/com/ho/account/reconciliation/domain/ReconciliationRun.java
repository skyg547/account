package com.ho.account.reconciliation.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 대사 실행(Reconciliation Run) 엔티티
 * 특정 대사 단위에 대한 실행 결과를 기록합니다.
 */
@Entity
@Table(name = "reconciliation_runs")
public class ReconciliationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_unit_id", nullable = false)
    private ReconciliationUnit reconciliationUnit;

    @Column(nullable = false)
    private LocalDate reconciliationDate; // 대사 기준일

    @Column(nullable = false)
    private LocalDateTime runStartTime; // 실행 시작 시간

    private LocalDateTime runEndTime; // 실행 종료 시간

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReconciliationRunStatus status; // 실행 상태 (SUCCESS, FAILED, PARTIAL, RUNNING)

    @Column(nullable = false)
    private Long totalItemsSource = 0L; // 원천 데이터 총 항목 수

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmountSource = BigDecimal.ZERO; // 원천 데이터 총 금액

    @Column(nullable = false)
    private Long totalItemsTarget = 0L; // 대상 데이터 총 항목 수

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmountTarget = BigDecimal.ZERO; // 대상 데이터 총 금액

    @Column(nullable = false)
    private Long matchedItemsCount = 0L; // 매칭된 항목 수

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal matchedAmount = BigDecimal.ZERO; // 매칭된 금액

    @Column(nullable = false)
    private Long unmatchedItemsCount = 0L; // 미매칭 항목 수

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unmatchedAmount = BigDecimal.ZERO; // 미매칭 금액

    @Column(length = 50)
    private String runBy; // 실행자

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.auditUser == null) {
            this.auditUser = "SYSTEM";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Enums ---
    public enum ReconciliationRunStatus {
        RUNNING, SUCCESS, FAILED, PARTIAL
    }

    // --- Getters and Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ReconciliationUnit getReconciliationUnit() {
        return reconciliationUnit;
    }

    public void setReconciliationUnit(ReconciliationUnit reconciliationUnit) {
        this.reconciliationUnit = reconciliationUnit;
    }

    public LocalDate getReconciliationDate() {
        return reconciliationDate;
    }

    public void setReconciliationDate(LocalDate reconciliationDate) {
        this.reconciliationDate = reconciliationDate;
    }

    public LocalDateTime getRunStartTime() {
        return runStartTime;
    }

    public void setRunStartTime(LocalDateTime runStartTime) {
        this.runStartTime = runStartTime;
    }

    public LocalDateTime getRunEndTime() {
        return runEndTime;
    }

    public void setRunEndTime(LocalDateTime runEndTime) {
        this.runEndTime = runEndTime;
    }

    public ReconciliationRunStatus getStatus() {
        return status;
    }

    public void setStatus(ReconciliationRunStatus status) {
        this.status = status;
    }

    public Long getTotalItemsSource() {
        return totalItemsSource;
    }

    public void setTotalItemsSource(Long totalItemsSource) {
        this.totalItemsSource = totalItemsSource;
    }

    public BigDecimal getTotalAmountSource() {
        return totalAmountSource;
    }

    public void setTotalAmountSource(BigDecimal totalAmountSource) {
        this.totalAmountSource = totalAmountSource;
    }

    public Long getTotalItemsTarget() {
        return totalItemsTarget;
    }

    public void setTotalItemsTarget(Long totalItemsTarget) {
        this.totalItemsTarget = totalItemsTarget;
    }

    public BigDecimal getTotalAmountTarget() {
        return totalAmountTarget;
    }

    public void setTotalAmountTarget(BigDecimal totalAmountTarget) {
        this.totalAmountTarget = totalAmountTarget;
    }

    public Long getMatchedItemsCount() {
        return matchedItemsCount;
    }

    public void setMatchedItemsCount(Long matchedItemsCount) {
        this.matchedItemsCount = matchedItemsCount;
    }

    public BigDecimal getMatchedAmount() {
        return matchedAmount;
    }

    public void setMatchedAmount(BigDecimal matchedAmount) {
        this.matchedAmount = matchedAmount;
    }

    public Long getUnmatchedItemsCount() {
        return unmatchedItemsCount;
    }

    public void setUnmatchedItemsCount(Long unmatchedItemsCount) {
        this.unmatchedItemsCount = unmatchedItemsCount;
    }

    public BigDecimal getUnmatchedAmount() {
        return unmatchedAmount;
    }

    public void setUnmatchedAmount(BigDecimal unmatchedAmount) {
        this.unmatchedAmount = unmatchedAmount;
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
