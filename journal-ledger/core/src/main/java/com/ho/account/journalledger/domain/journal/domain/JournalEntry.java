package com.ho.account.journalledger.domain.journal.domain;

import com.ho.account.masterdata.core.domain.model.Currency;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 회계 전표(Journal Entry) — 분개 정보를 담는 핵심 Aggregate Root.
 *
 * ─────────────────────────────────────────────────
 * [업무 설명]
 * 전표(傳票, Journal Entry)는 회계에서 모든 거래를 기록하는 기본 단위입니다.
 * "언제(날짜), 어떤 계정에, 얼마를, 차변/대변 어느 쪽으로" 기록할지를 정의합니다.
 *
 * 하나의 전표(JournalEntry)는 여러 개의 전표 상세(JournalDetail) 라인으로 구성됩니다.
 * 복식부기 원칙에 따라 차변 합계 = 대변 합계가 반드시 성립해야 합니다.
 *
 * 전표 생명주기:
 *   DRAFT(초안) → REQUESTED(승인요청) → APPROVED(승인) → POSTED(전기/원장반영)
 *                                      ↘ REJECTED(반려)
 *   POSTED → REVERSED(역분개 취소)
 *
 * 핵심 필드:
 *   - slipNo       : 전표 번호 (시스템이 자동 채번, 유니크)
 *   - slipDate     : 전표 작성일 (문서 기준일)
 *   - accountingDate : 회계 반영일 (원장 기표 기준일, 회계 기간 마감에 사용)
 *   - entryType    : NORMAL(일반), REVERSAL(역분개) 등 전표 유형
 *   - lineageSourceType / lineageSourceId : 원천 문서 추적용
 *                    (예: "EXPENDITURE_RESOLUTION" / "REQ-20260101-001")
 *                    drill-down 기능에서 원천 문서로 되돌아갈 때 사용합니다.
 *
 * ─────────────────────────────────────────────────
 * [개발 설명]
 * - 이 클래스는 헥사고날 아키텍처의 도메인 레이어에 위치합니다.
 * - JPA @Entity로 journal_entries 테이블에 매핑됩니다.
 * - 상태 전이(approve/reject/post)는 서비스가 아닌 도메인 메서드에서 처리합니다 (Rich Domain Model).
 * - details 컬렉션은 CascadeType.ALL + orphanRemoval=true로 전표와 생명주기를 함께합니다.
 * - validateBalance()는 차대변 일치 여부를 검증하며, createJournalEntry 시 반드시 호출됩니다.
 * - slipNo 채번은 JournalEntryService에서 담당합니다 (도메인 외부 관심사).
 * - 인덱스: slipNo, accountingDate, status, lineageSourceType+lineageSourceId
 * ─────────────────────────────────────────────────
 */
@Entity
@Table(name = "journal_entries", indexes = {
        @Index(name = "idx_journal_entry_slip_no", columnList = "slipNo"),
        @Index(name = "idx_journal_entry_posting_lookup", columnList = "status, accountingDate"),
        @Index(name = "idx_journal_entry_lineage", columnList = "lineageSourceType, lineageSourceId")
})
public class JournalEntry {

    /** 시스템 내부 PK (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 전표 번호 (슬립 번호).
     * 시스템이 자동 채번하며, 한 시스템 내에서 유일해야 합니다.
     * 예: "JE-20260101-0001"
     */
    @Column(nullable = false, unique = true, length = 20)
    private String slipNo;

    /**
     * 전표 작성일 (문서 기준일).
     * 사용자가 전표를 작성한 날짜입니다.
     * accountingDate와 다를 수 있습니다 (예: 월말 마감 후 소급 처리).
     */
    @Column(nullable = false)
    private LocalDate slipDate;

    /**
     * 회계 반영일 (기표일).
     * 원장(GL/SL)에 금액이 반영되는 기준 날짜입니다.
     * 회계 기간(fiscal period) 계산에 사용됩니다.
     * null이면 slipDate와 동일하게 자동 설정됩니다(@PrePersist).
     */
    @Column(nullable = false)
    private LocalDate accountingDate;

    /** 전표 적요 — 거래 내용을 한 줄로 요약한 설명 (예: "지출결의: 사무용품 구매") */
    @Column(length = 200)
    private String description;

    /**
     * 전표 상태.
     * DRAFT → REQUESTED → APPROVED → POSTED 순으로 진행됩니다.
     * 각 상태의 의미는 JournalEntryStatus 열거형을 참고하세요.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private JournalEntryStatus status;

    /**
     * 전표 유형.
     * - NORMAL   : 일반 분개 전표
     * - REVERSAL : 역분개(취소) 전표
     * null이면 @PrePersist에서 "NORMAL"로 자동 설정됩니다.
     */
    @Column(length = 50)
    private String entryType;

    /**
     * 거래 통화.
     * 외화 거래 시 해당 통화를 설정하고, exchangeRate로 기본 통화 환산 금액을 계산합니다.
     * 국내 원화 거래이면 null 또는 KRW.
     */
    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    /**
     * 환율 (거래 통화 → 기본 통화 변환 비율).
     * 예: USD 거래 시 1,350.00 (USD 1 = KRW 1,350)
     * 정밀도 손실 방지를 위해 BigDecimal(19,8) 사용.
     */
    @Column(precision = 19, scale = 8)
    private BigDecimal exchangeRate;

    /** 반려 사유 — 결재자가 전표를 반려할 때 기록하는 사유 텍스트 */
    @Column(length = 500)
    private String rejectionReason;

    /**
     * 전표 상세 라인 목록.
     * 하나의 전표는 최소 2개(차변 1개 + 대변 1개) 이상의 상세 라인을 가집니다.
     * CascadeType.ALL: 전표 저장/삭제 시 상세도 함께 처리됩니다.
     * orphanRemoval=true: 컬렉션에서 제거된 상세는 자동으로 DB에서도 삭제됩니다.
     */
    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalDetail> details = new ArrayList<>();

    /** 최초 생성 일시 (수정 불가 — @Column(updatable=false)) */
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** 최종 수정 일시 */
    private LocalDateTime updatedAt;

    /** 전표 작성자 ID 또는 이름 */
    @Column(length = 50)
    private String createdBy;

    /** 최종 처리자 (승인/반려/전기한 사람). 상태 변경 시 자동 갱신됩니다. */
    @Column(length = 50)
    private String auditUser;

    /**
     * 원천 문서 유형 (Lineage Source Type).
     * 이 전표를 생성한 원천 업무 문서의 종류를 저장합니다.
     * 예: "EXPENDITURE_RESOLUTION", "PURCHASE_INVOICE", "LEASE_PAYMENT"
     * drill-down 기능에서 원장 항목 → 원천 문서로 역추적할 때 사용합니다.
     */
    @Column(length = 50)
    private String lineageSourceType;

    /**
     * 원천 문서 식별자 (Lineage Source ID).
     * lineageSourceType과 함께 원천 문서를 특정합니다.
     * 예: "REQ-20260101-001" (지출결의 번호), "INV-2026-0042" (매입 인보이스 번호)
     */
    @Column(length = 100)
    private String lineageSourceId;

    // ─── 생명주기 콜백 ────────────────────────────────────

    /**
     * 최초 저장(INSERT) 시 자동 실행되는 초기화 로직.
     * - createdAt, updatedAt 자동 설정
     * - status 기본값: DRAFT
     * - accountingDate 미입력 시 slipDate로 대체
     * - entryType 기본값: NORMAL
     * - auditUser 기본값: createdBy 또는 "SYSTEM"
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = JournalEntryStatus.DRAFT;
        }
        if (this.accountingDate == null) {
            // 회계 반영일 미입력 시 전표 작성일과 동일하게 처리
            this.accountingDate = this.slipDate;
        }
        if (this.entryType == null) {
            this.entryType = "NORMAL";
        }
        if (this.auditUser == null) {
            this.auditUser = this.createdBy != null ? this.createdBy : "SYSTEM";
        }
    }

    /** 수정(UPDATE) 시 updatedAt 자동 갱신 */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ─── 도메인 비즈니스 메서드 ──────────────────────────────

    /**
     * 전표를 승인합니다 (상태: DRAFT/REQUESTED → APPROVED).
     *
     * [업무 규칙]
     * - DRAFT 또는 REQUESTED 상태에서만 승인 가능합니다.
     * - 승인 전 반드시 차대변 합계 일치(validateBalance)를 검증합니다.
     * - 승인된 전표는 PostingService를 통해 GL/SL 원장에 전기됩니다.
     *
     * [개발 참고]
     * - 호출 위치: JournalEntryService.approveJournalEntry()
     * - Rich Domain Model 원칙: 상태 전이 로직을 엔티티 안에 캡슐화합니다.
     *
     * @param approver 승인자 ID 또는 이름
     */
    public void approve(String approver) {
        if (this.status != JournalEntryStatus.DRAFT && this.status != JournalEntryStatus.REQUESTED) {
            throw new IllegalStateException("승인 가능한 상태가 아닙니다. 현재 상태: " + this.status);
        }
        validateBalance(); // 차대변 합계 불일치 시 예외 발생
        this.status = JournalEntryStatus.APPROVED;
        this.auditUser = approver;
    }

    /**
     * 전표를 반려합니다 (상태: → REJECTED).
     *
     * [업무 규칙]
     * - 결재자가 전표 내용에 문제가 있다고 판단할 때 반려합니다.
     * - 반려된 전표는 작성자가 수정 후 다시 승인 요청할 수 있습니다.
     *
     * @param approver 반려자 ID 또는 이름
     * @param reason   반려 사유 (작성자에게 표시됨)
     */
    public void reject(String approver, String reason) {
        this.status = JournalEntryStatus.REJECTED;
        this.rejectionReason = reason;
        this.auditUser = approver;
    }

    /**
     * 전표를 전기합니다 (상태: APPROVED → POSTED).
     *
     * [업무 설명]
     * 전기(Posting)는 승인된 전표를 실제 총계정원장(GL)과 보조원장(SL)에
     * 반영하는 최종 확정 단계입니다. POSTED 상태가 되면 전표는 변경 불가합니다.
     *
     * [개발 참고]
     * - 실제 원장 데이터 생성은 PostingService.postJournalEntry()에서 처리합니다.
     * - 이 메서드는 상태 변경과 감사 기록만 담당합니다.
     *
     * @param poster 전기 처리자 ID 또는 이름
     */
    public void post(String poster) {
        if (this.status != JournalEntryStatus.APPROVED) {
            throw new IllegalStateException("승인된 전표만 전기할 수 있습니다.");
        }
        this.status = JournalEntryStatus.POSTED;
        this.auditUser = poster;
    }

    /**
     * 현재 전표를 취소하는 역분개(Reversal) 전표를 생성합니다.
     *
     * [업무 규칙]
     * - POSTED 상태인 전표만 역분개할 수 있습니다.
     * - 원본 전표의 모든 라인의 차대변 방향을 반전시켜 생성합니다.
     * - 역분개 전표는 생성 즉시 승인(APPROVED) 상태로 두거나, 정책에 따라 DRAFT로 둘 수 있습니다.
     *   (여기서는 명시적 검토를 위해 DRAFT로 생성합니다)
     *
     * @param creator        역분개 작성자
     * @param accountingDate 역분개 회계 반영일
     * @param reason         역분개 사유
     * @return 생성된 역분개 전표 (id 없음)
     */
    public JournalEntry createReversal(String creator, LocalDate accountingDate, String reason) {
        if (this.status != JournalEntryStatus.POSTED) {
            throw new IllegalStateException("전기 완료된 전표만 역분개할 수 있습니다.");
        }

        JournalEntry reversal = new JournalEntry();
        reversal.setSlipDate(LocalDate.now());
        reversal.setAccountingDate(accountingDate);
        reversal.setDescription("[역분개 취소] " + this.description + " (사유: " + reason + ")");
        reversal.setEntryType("REVERSAL");
        reversal.setCurrencyCode(this.currencyCode);
        reversal.setExchangeRate(this.exchangeRate);
        reversal.setCreatedBy(creator);
        reversal.setLineageSourceType("JOURNAL_ENTRY");
        reversal.setLineageSourceId(this.id.toString());

        for (JournalDetail originalDetail : this.details) {
            reversal.addDetail(originalDetail.copyWithFlippedSide());
        }

        reversal.validateBalance();
        return reversal;
    }

    /**
     * 차변/대변 합계 일치 여부를 검증합니다.
     *
     * [업무 설명]
     * 복식부기의 핵심 원칙: 모든 전표는 차변 합계 = 대변 합계여야 합니다.
     * 이 검증이 통과해야 전표 승인이 가능합니다.
     *
     * [예외 발생 조건]
     * 1. details가 비어있는 경우 (상세 라인 없음)
     * 2. 차변 합계 ≠ 대변 합계인 경우
     *
     * @throws IllegalStateException 정합성 검증 실패 시
     */
    public void validateBalance() {
        if (details.isEmpty()) {
            throw new IllegalStateException("전표 상세 내역이 없습니다.");
        }

        // 차변(DEBIT) 라인 금액 합계
        BigDecimal debitSum = details.stream()
                .filter(d -> JournalSide.DEBIT.equals(d.getSide()))
                .map(JournalDetail::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 대변(CREDIT) 라인 금액 합계
        BigDecimal creditSum = details.stream()
                .filter(d -> JournalSide.CREDIT.equals(d.getSide()))
                .map(JournalDetail::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (debitSum.compareTo(creditSum) != 0) {
            throw new IllegalStateException(
                    String.format("차대변 합계가 일치하지 않습니다. (차변: %s, 대변: %s)", debitSum, creditSum));
        }
    }

    // ─── 상세 라인 관리 편의 메서드 ──────────────────────────

    /**
     * 전표에 상세 라인을 추가합니다.
     * 양방향 연관관계를 모두 설정합니다 (detail.journalEntry도 자동 설정).
     *
     * @param detail 추가할 전표 상세 라인
     */
    public void addDetail(JournalDetail detail) {
        details.add(detail);
        detail.setJournalEntry(this);
    }

    /**
     * 전표에서 특정 상세 라인을 제거합니다.
     * orphanRemoval=true 설정으로 DB에서도 자동 삭제됩니다.
     *
     * @param detail 제거할 전표 상세 라인
     */
    public void removeDetail(JournalDetail detail) {
        details.remove(detail);
        detail.setJournalEntry(null);
    }

    /**
     * 전표의 모든 상세 라인을 초기화합니다.
     * 전표 수정 시 기존 라인을 모두 제거하고 새로 추가할 때 사용합니다.
     */
    public void clearDetails() {
        this.details.clear();
    }

    // ─── Getter / Setter ──────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSlipNo() { return slipNo; }
    public void setSlipNo(String slipNo) { this.slipNo = slipNo; }

    public LocalDate getSlipDate() { return slipDate; }
    public void setSlipDate(LocalDate slipDate) { this.slipDate = slipDate; }

    public LocalDate getAccountingDate() { return accountingDate; }
    public void setAccountingDate(LocalDate accountingDate) { this.accountingDate = accountingDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public JournalEntryStatus getStatus() { return status; }
    public void setStatus(JournalEntryStatus status) { this.status = status; }

    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public BigDecimal getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(BigDecimal exchangeRate) { this.exchangeRate = exchangeRate; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public List<JournalDetail> getDetails() { return details; }
    public void setDetails(List<JournalDetail> details) { this.details = details; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getAuditUser() { return auditUser; }
    public void setAuditUser(String auditUser) { this.auditUser = auditUser; }

    public String getLineageSourceType() { return lineageSourceType; }
    public void setLineageSourceType(String lineageSourceType) { this.lineageSourceType = lineageSourceType; }

    public String getLineageSourceId() { return lineageSourceId; }
    public void setLineageSourceId(String lineageSourceId) { this.lineageSourceId = lineageSourceId; }
}
