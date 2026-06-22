package com.ho.account.reconciliation.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 대사 차이(Reconciliation Difference) 엔티티.
 * 대사 실행 결과 발견된 차이 항목을 기록하고, 사유 코드 및 조정 전표와 연계합니다.
 */
/**
 * [DDD(도메인 주도 설계) - Aggregate Root]
 * 대사 차이(Reconciliation Difference) 엔티티. 대사 실행 결과 발견된 불일치 항목과 그 해결 과정을 관리합니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 대사 차이는 한마디로 '장부가 맞지 않는 내역'입니다.
 * 예를 들어, 은행 통장에는 10,000원이 입금되었는데 회사 장부에는 9,000원만 기록되어 있다면 1,000원의 차이가 발생하겠죠?
 * 이 클래스는 그 1,000원이 왜 차이 나는지(사유), 누구에게 확인을 맡겼는지(담당자), 
 * 그리고 최종적으로 어떻게 해결했는지(조정 전표 등)를 기록하는 일지 역할을 합니다.
 */
@Entity
@Table(name = "reconciliation_differences")
public class ReconciliationDifference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_run_id", nullable = false)
    private ReconciliationRun reconciliationRun;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DifferenceType differenceType; // 차이 유형 (MISSING_SOURCE, MISSING_TARGET, AMOUNT_MISMATCH 등)

    @Column(precision = 19, scale = 2)
    private BigDecimal amountExpected; // 기대 금액

    @Column(precision = 19, scale = 2)
    private BigDecimal amountActual; // 실제 금액

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal differenceAmount; // 차이 금액

    @Column(length = 1000)
    private String description; // 차이 상세 설명

    // 원천/대상 데이터 항목에 대한 참조 (예: JSON 문자열로 { "type": "BANK_TRANSACTION", "id": "TXN123" } 또는 { "type": "JOURNAL_ENTRY_DETAIL", "id": "JD456" })
    @Column(columnDefinition = "TEXT")
    private String sourceItemRef;

    @Column(columnDefinition = "TEXT")
    private String targetItemRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reason_code_id")
    private DifferenceReasonCode reasonCode; // 차이 사유 코드 (DoD: 차이는 원인코드로 반드시 수렴)

    @Column(name = "adjustment_journal_entry_id")
    private Long adjustmentJournalEntryId; // 조정 전표 ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReconciliationDifferenceStatus status; // 차이 처리 상태 (PENDING, ASSIGNED, RESOLVED, IGNORED)

    @Column(length = 50)
    private String assignedToUser; // 담당자 (User 엔티티와 FK 연결 가능하나, 일단 String으로)

    private LocalDateTime slaDueDate; // SLA 기한

    private LocalDateTime resolvedAt; // 해결 일시

    @Column(length = 50)
    private String resolvedBy; // 해결자

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
        if (this.status == null) {
            this.status = ReconciliationDifferenceStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Enums ---
    public enum DifferenceType {
        AMOUNT_MISMATCH, // 금액 불일치
        MISSING_SOURCE,  // 원천 데이터 누락
        MISSING_TARGET,  // 대상 데이터 누락
        DATE_MISMATCH,   // 날짜 불일치
        OTHER            // 기타
    }

    public enum ReconciliationDifferenceStatus {
        PENDING,   // 처리 대기
        ASSIGNED,  // 담당자 할당
        IN_REVIEW, // 검토 중
        RESOLVED,  // 해결 완료
        IGNORED    // 무시됨
    }

    // --- Getter 및 Setter ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ReconciliationRun getReconciliationRun() {
        return reconciliationRun;
    }

    public void setReconciliationRun(ReconciliationRun reconciliationRun) {
        this.reconciliationRun = reconciliationRun;
    }

    public DifferenceType getDifferenceType() {
        return differenceType;
    }

    public void setDifferenceType(DifferenceType differenceType) {
        this.differenceType = differenceType;
    }

    public BigDecimal getAmountExpected() {
        return amountExpected;
    }

    public void setAmountExpected(BigDecimal amountExpected) {
        this.amountExpected = amountExpected;
    }

    public BigDecimal getAmountActual() {
        return amountActual;
    }

    public void setAmountActual(BigDecimal amountActual) {
        this.amountActual = amountActual;
    }

    public BigDecimal getDifferenceAmount() {
        return differenceAmount;
    }

    public void setDifferenceAmount(BigDecimal differenceAmount) {
        this.differenceAmount = differenceAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSourceItemRef() {
        return sourceItemRef;
    }

    public void setSourceItemRef(String sourceItemRef) {
        this.sourceItemRef = sourceItemRef;
    }

    public String getTargetItemRef() {
        return targetItemRef;
    }

    public void setTargetItemRef(String targetItemRef) {
        this.targetItemRef = targetItemRef;
    }

    public DifferenceReasonCode getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(DifferenceReasonCode reasonCode) {
        this.reasonCode = reasonCode;
    }

    public Long getAdjustmentJournalEntryId() {
        return adjustmentJournalEntryId;
    }

    public void setAdjustmentJournalEntryId(Long adjustmentJournalEntryId) {
        this.adjustmentJournalEntryId = adjustmentJournalEntryId;
    }

    public ReconciliationDifferenceStatus getStatus() {
        return status;
    }

    public void setStatus(ReconciliationDifferenceStatus status) {
        this.status = status;
    }

    public String getAssignedToUser() {
        return assignedToUser;
    }

    public void setAssignedToUser(String assignedToUser) {
        this.assignedToUser = assignedToUser;
    }

    public LocalDateTime getSlaDueDate() {
        return slaDueDate;
    }

    public void setSlaDueDate(LocalDateTime slaDueDate) {
        this.slaDueDate = slaDueDate;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
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