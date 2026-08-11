package com.ho.account.reconciliation.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 대사 차이(Reconciliation Difference) 엔티티.
 * 대사 실행 결과 발견된 차이 항목을 기록하고, 사유 코드 및 조정 전표와 연계합니다.
 */
/**
 * [DDD(도메인 주도 설계) - Aggregate Root & Rich Domain Model]
 * 대사 차이(Reconciliation Difference) 엔티티. 대사 실행 결과 발견된 불일치 항목과 그 해결 과정을 관리합니다.
 * 
 * 🐣 [초보자를 위한 개념 설명: Anemic Domain Model vs Rich Domain Model]
 * 1. 빈약한 도메인 모델 (Anemic Domain Model):
 *    - 엔티티가 단순 데이터 상자(Getter/Setter 모음) 역할만 수행하고, 모든 비즈니스 로직(검증, 상태 전이 규칙)이
 *      애플리케이션 서비스에 흩어져 있는 설계입니다.
 *    - 이는 객체지향 캡슐화(Encapsulation) 원칙을 위배하며, 엔티티 상태의 불변성(Invariant)을 보장하지 못해 
 *      잘못된 데이터 상태(예: 담당자 지정 없이 상태만 ASSIGNED로 변경됨)가 발생하기 쉽습니다.
 * 
 * 2. 풍부한 도메인 모델 (Rich Domain Model):
 *    - 상태(State)와 행위(Behavior)가 객체 내부에 결합되어 스스로 불변식(Invariant)을 지키는 도메인 모델입니다.
 *    - `assignOwner()`, `resolve()`와 같이 비즈니스 언어(유비쿼터스 언어)를 반영하는 서명(Method Signature)을 통해
 *      상태 전이와 검증 규칙을 엔티티 내부로 캡슐화합니다.
 *    - 이를 통해 Service 레이어는 도메인 객체의 포트 조율(Orchestration) 역할만 담당하게 되어 코드가 훨씬 깔끔하고 유지보수가 쉬워집니다.
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
    @Column(length = 255)
    private String sourceItemRef;

    @Column(length = 255)
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

    // --- Rich Domain Model Business Logic Methods ---

    /**
     * [교육적 주석 - Rich Domain Model 팩토리 메서드]
     * 신규 대사 차이 객체를 올바른 초기 상태(PENDING)로 생성합니다.
     * 
     * @param reconciliationRun 대사 실행 엔티티
     * @param differenceType 차이 유형
     * @param amountExpected 기대 금액
     * @param amountActual 실제 금액
     * @param differenceAmount 차이 금액
     * @param description 상세 설명
     * @param sourceItemRef 원천 참조 정보
     * @param targetItemRef 대상 참조 정보
     * @param reasonCode 차이 사유 코드
     * @param auditUser 작업 수행자
     * @return 갓 생성된 ReconciliationDifference 객체
     */
    public static ReconciliationDifference createDifference(
            ReconciliationRun reconciliationRun,
            DifferenceType differenceType,
            BigDecimal amountExpected,
            BigDecimal amountActual,
            BigDecimal differenceAmount,
            String description,
            String sourceItemRef,
            String targetItemRef,
            DifferenceReasonCode reasonCode,
            String auditUser) {
        
        ReconciliationDifference diff = new ReconciliationDifference();
        diff.reconciliationRun = reconciliationRun;
        diff.differenceType = differenceType;
        diff.amountExpected = amountExpected;
        diff.amountActual = amountActual;
        diff.differenceAmount = differenceAmount;
        diff.description = description;
        diff.sourceItemRef = sourceItemRef;
        diff.targetItemRef = targetItemRef;
        diff.reasonCode = reasonCode;
        diff.status = ReconciliationDifferenceStatus.PENDING;
        diff.auditUser = auditUser != null ? auditUser : "SYSTEM";
        return diff;
    }

    /**
     * [교육적 주석 - Rich Domain Model: 담당자 배정 도메인 메서드]
     * 대사 차이 항목에 담당자를 지정하고 SLA 기한을 세팅하며, 상태를 ASSIGNED로 원자적 전환합니다.
     * 외부 서비스에서 setter를 여러 번 호출하는 대신, 단일 행위 메서드로 불변식을 지킵니다.
     * 
     * @param assignedToUser 담당자 식별자
     * @param slaDueDate SLA 마감일시
     */
    public void assignOwner(String assignedToUser, LocalDateTime slaDueDate) {
        if (assignedToUser == null || assignedToUser.isBlank()) {
            throw new IllegalArgumentException("Assigned user cannot be null or empty.");
        }
        this.assignedToUser = assignedToUser.trim();
        this.slaDueDate = slaDueDate;
        this.status = ReconciliationDifferenceStatus.ASSIGNED;
    }

    /**
     * [교육적 주석 - Rich Domain Model: 차이 해결 및 도메인 규칙 검증 메서드]
     * 대사 차이를 해결(RESOLVED) 또는 무시(IGNORED) 상태로 완결합니다.
     * 
     * 💡 도메인 불변식 규칙:
     * 1. 최종 상태는 RESOLVED 또는 IGNORED여야 함.
     * 2. 사유 코드(ReasonCode)가 반드시 전달되어야 함.
     * 3. 사유 코드가 조정 대상(isAdjustable)인 경우, 조정 전표(adjustmentJournalEntryId) 링크가 필수적임.
     * 
     * @param reasonCode 차이 사유 코드
     * @param adjustmentJournalEntryId 연계된 조정 전표 ID (선택/필수)
     * @param targetStatus 최종 변경 상태 (RESOLVED 또는 IGNORED)
     * @param resolvedBy 해결 작업자
     */
    public void resolve(DifferenceReasonCode reasonCode, Long adjustmentJournalEntryId, ReconciliationDifferenceStatus targetStatus, String resolvedBy) {
        if (targetStatus != ReconciliationDifferenceStatus.RESOLVED && targetStatus != ReconciliationDifferenceStatus.IGNORED) {
            throw new IllegalArgumentException("Difference can only be finalized as RESOLVED or IGNORED.");
        }
        if (reasonCode == null) {
            throw new IllegalArgumentException("Reason code is required to finalize reconciliation difference.");
        }

        Long journalEntryIdToLink = adjustmentJournalEntryId != null ? adjustmentJournalEntryId : this.adjustmentJournalEntryId;

        if (reasonCode.isAdjustable() && journalEntryIdToLink == null) {
            throw new IllegalArgumentException("Adjustable reason code requires an adjustment journal entry link.");
        }

        this.reasonCode = reasonCode;
        this.adjustmentJournalEntryId = journalEntryIdToLink;
        this.status = targetStatus;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = LocalDateTime.now();
        this.auditUser = resolvedBy != null ? resolvedBy : "SYSTEM";
    }

    /**
     * [교육적 주석 - Rich Domain Model: 조정 전표 연계 메서드]
     * 생성된 조정 전표 ID를 차이 객체에 연계합니다.
     * 
     * @param adjustmentJournalEntryId 생성된 조정 전표 ID
     */
    public void attachAdjustmentJournalEntry(Long adjustmentJournalEntryId) {
        if (adjustmentJournalEntryId == null) {
            throw new IllegalArgumentException("Adjustment journal entry ID cannot be null.");
        }
        this.adjustmentJournalEntryId = adjustmentJournalEntryId;
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
