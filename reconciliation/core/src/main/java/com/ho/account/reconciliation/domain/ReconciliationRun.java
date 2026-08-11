package com.ho.account.reconciliation.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [DDD(도메인 주도 설계) - Aggregate Root & Rich Domain Model]
 * 대사 실행(Reconciliation Run) 엔티티. 특정 대사 단위에 대한 실행 결과를 기록하고 lifecycle을 관리합니다.
 * 
 * 🐣 [초보자를 위한 개념 설명: Anemic Domain Model vs Rich Domain Model]
 * 기존 Anemic Domain Model에서는 대사 실행의 상태(RUNNING -> SUCCESS / FAILED) 전환과
 * 집계 데이터 세팅을 외부 서비스(`ReconciliationService`)에서 setter로 일일이 호출했습니다.
 * 
 * Rich Domain Model로 전환함에 따라:
 * 1. 대사의 시작(`startRun`), 성공적 완료(`completeRun`), 실패(`failRun`)를 의미 명확한 도메인 메서드로 제공합니다.
 * 2. 실행 중(RUNNING)이 아닌 상태에서 중복으로 결과를 완료 처리하려고 시도하는 등 잘못된 상태 전이(State Transition)를
 *    엔티티 내부 검증 로직으로 차단합니다.
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

    // --- Rich Domain Model Business Logic Methods ---

    /**
     * [교육적 주석 - Rich Domain Model: 대사 실행 시작 팩토리 메서드]
     * 대사 실행 작업 객체를 생성하고 RUNNING 상태로 시작합니다.
     * 
     * @param reconciliationUnit 대상 대사 단위
     * @param reconciliationDate 대사 기준일
     * @param runBy 대사 실행자
     * @return 대사 실행 객체 (RUNNING 상태)
     */
    public static ReconciliationRun startRun(ReconciliationUnit reconciliationUnit, LocalDate reconciliationDate, String runBy) {
        if (reconciliationUnit == null) {
            throw new IllegalArgumentException("ReconciliationUnit cannot be null.");
        }
        if (reconciliationDate == null) {
            throw new IllegalArgumentException("ReconciliationDate cannot be null.");
        }

        ReconciliationRun run = new ReconciliationRun();
        run.reconciliationUnit = reconciliationUnit;
        run.reconciliationDate = reconciliationDate;
        run.runStartTime = LocalDateTime.now();
        run.status = ReconciliationRunStatus.RUNNING;
        run.runBy = runBy != null ? runBy : "SYSTEM";
        run.auditUser = run.runBy;
        return run;
    }

    /**
     * [교육적 주석 - Rich Domain Model: 대사 실행 성공 완료 메서드]
     * 대사 작업을 완료하고 대사 집계 결과를 세팅하며 상태를 SUCCESS로 변경합니다.
     * 
     * 💡 상태 불변식 검증:
     * RUNNING 상태가 아닌 실행 결과에 대해 완료를 시도하는 경우 예외를 발생시킵니다.
     */
    public void completeRun(
            long totalItemsSource, BigDecimal totalAmountSource,
            long totalItemsTarget, BigDecimal totalAmountTarget,
            long matchedItemsCount, BigDecimal matchedAmount,
            long unmatchedItemsCount, BigDecimal unmatchedAmount) {

        if (this.status != ReconciliationRunStatus.RUNNING) {
            throw new IllegalStateException("Cannot complete a reconciliation run that is not in RUNNING state. Current status: " + this.status);
        }

        this.totalItemsSource = totalItemsSource;
        this.totalAmountSource = totalAmountSource != null ? totalAmountSource : BigDecimal.ZERO;
        this.totalItemsTarget = totalItemsTarget;
        this.totalAmountTarget = totalAmountTarget != null ? totalAmountTarget : BigDecimal.ZERO;
        this.matchedItemsCount = matchedItemsCount;
        this.matchedAmount = matchedAmount != null ? matchedAmount : BigDecimal.ZERO;
        this.unmatchedItemsCount = unmatchedItemsCount;
        this.unmatchedAmount = unmatchedAmount != null ? unmatchedAmount : BigDecimal.ZERO;

        this.runEndTime = LocalDateTime.now();
        this.status = ReconciliationRunStatus.SUCCESS;
    }

    /**
     * [교육적 주석 - Rich Domain Model: 대사 실행 실패 처리 메서드]
     * 대사 작업 중 오류 발생 시 상태를 FAILED로 변경하고 종료 일시를 기록합니다.
     */
    public void failRun() {
        this.runEndTime = LocalDateTime.now();
        this.status = ReconciliationRunStatus.FAILED;
    }

    // --- Getter 및 Setter ---
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
