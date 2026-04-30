package com.ho.account.journalledger.domain.journal.domain;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Department;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 전표 상세 라인 (Journal Detail) — 분개 전표의 개별 차변/대변 한 줄.
 *
 * ─────────────────────────────────────────────────
 * [업무 설명]
 * 하나의 전표(JournalEntry)는 여러 개의 상세 라인으로 구성됩니다.
 * 각 라인은 "어떤 계정에, 얼마를, 차변인지 대변인지"를 기록합니다.
 *
 * 예시) 현금 100만 원 비품 구입 전표의 상세 라인:
 *   라인1: 차변(DEBIT) | 계정: 비품(10300) | 금액: 1,000,000
 *   라인2: 대변(CREDIT) | 계정: 현금(10100) | 금액: 1,000,000
 *
 * 주요 필드:
 *   - side            : DEBIT(차변) 또는 CREDIT(대변)
 *   - accountSubject  : 계정과목 (예: 현금, 매출채권, 복리후생비 등)
 *   - amount          : 거래 통화 기준 금액 (외화 거래 시 외화 금액)
 *   - baseAmount      : 기본 통화(KRW) 기준 금액 (원장 잔액 계산에 사용)
 *   - department      : 귀속 부서 (부서별 원가 분석에 활용)
 *   - businessPartner : 거래처 (매입/매출처, 보조원장 SL 구분 키)
 *   - detailDescription : 라인별 상세 적요 (헤더 적요와 별개)
 *
 * ─────────────────────────────────────────────────
 * [개발 설명]
 * - JPA @Entity로 journal_details 테이블에 매핑됩니다.
 * - JournalEntry와 @ManyToOne 양방향 연관. 전표 삭제 시 자동 삭제(orphanRemoval).
 * - amount vs baseAmount:
 *     amount     = 거래통화 금액 (외화면 외화값 그대로)
 *     baseAmount = 기본통화(KRW) 환산 금액 (GL/SL 잔액 계산 기준)
 *   원화 거래이면 amount = baseAmount.
 * - 성능용 인덱스: journal_entry_id, account_code, dept_code, business_partner_code, side
 * - LedgerService는 baseAmount를 기준으로 GL/SL 잔액을 업데이트합니다.
 * ─────────────────────────────────────────────────
 */
@Entity
@Table(name = "journal_details", indexes = {
    @Index(name = "idx_journal_detail_journal_entry_id", columnList = "journal_entry_id"),
    @Index(name = "idx_journal_detail_account_code",    columnList = "account_code"),
    @Index(name = "idx_journal_detail_dept_code",       columnList = "dept_code"),
    @Index(name = "idx_journal_detail_business_partner_code", columnList = "business_partner_code"),
    @Index(name = "idx_journal_detail_side",            columnList = "side")
})
public class JournalDetail {

    /** 시스템 내부 PK (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 소속 전표 (상위 JournalEntry).
     * @ManyToOne + FetchType.LAZY: 필요할 때만 전표 헤더를 조회합니다(N+1 방지).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    /**
     * 차변/대변 구분.
     * DEBIT(차변, Dr) 또는 CREDIT(대변, Cr).
     * JournalEntry.validateBalance()에서 DEBIT 합계 = CREDIT 합계를 검증합니다.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private JournalSide side;

    /**
     * 계정과목 (Account Subject).
     * 예: 현금(10100), 매출채권(11000), 복리후생비(82100) 등.
     * master-data 모듈의 AccountSubject 엔티티를 참조합니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_code", nullable = false)
    private AccountSubject accountSubject;

    /**
     * 거래 통화 기준 금액.
     * 외화 거래: 외화 금액 그대로 저장. (예: USD 500.00)
     * 원화 거래: baseAmount와 동일.
     * precision=19, scale=2: 최대 9,999조 원까지 소수점 2자리 지원.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * 기본 통화(KRW) 기준 금액.
     * GL/SL 잔액 업데이트 시 이 금액을 사용합니다.
     * 원화 거래이면 amount와 동일. 외화 거래이면 amount × exchangeRate.
     * LedgerService.updateLedgerBalances()에서 참조합니다.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal baseAmount = BigDecimal.ZERO;

    /**
     * 귀속 부서 (Department).
     * 부서별 원가/비용 분석(관리회계)에 사용됩니다.
     * null 허용: 부서 귀속이 불필요한 계정(예: 현금, 차입금)은 null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_code", referencedColumnName = "code")
    private Department department;

    /**
     * 거래처 (Business Partner).
     * 보조원장(SL: Subsidiary Ledger) 집계 시 거래처별 잔액 분류 키입니다.
     * 예: 매입처 A사, 매출처 B사 등.
     * null 허용: 거래처 없는 내부 계정은 null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_partner_code", referencedColumnName = "businessPartnerCode")
    private BusinessPartner businessPartner;

    /**
     * 라인별 상세 적요.
     * 전표 헤더의 description보다 더 구체적인 라인 단위 설명.
     * 예: "2026년 1월 사무용품 구입 - 볼펜 50개"
     */
    @Column(length = 200)
    private String detailDescription;

    /** 최초 생성 일시 (수정 불가) */
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** 최종 수정 일시 */
    private LocalDateTime updatedAt;

    /** 처리자 (자동분개인 경우 "SYSTEM") */
    @Column(length = 50)
    private String auditUser;

    // ─── 생명주기 콜백 ──────────────────────────────────────

    /** 최초 저장 시 createdAt, updatedAt, auditUser 기본값 설정 */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.auditUser == null) this.auditUser = "SYSTEM";
    }

    /** 수정 시 updatedAt 자동 갱신 */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ─── Getter / Setter ──────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public JournalEntry getJournalEntry() { return journalEntry; }
    public void setJournalEntry(JournalEntry journalEntry) { this.journalEntry = journalEntry; }

    public JournalSide getSide() { return side; }
    public void setSide(JournalSide side) { this.side = side; }

    public AccountSubject getAccountSubject() { return accountSubject; }
    public void setAccountSubject(AccountSubject accountSubject) { this.accountSubject = accountSubject; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getBaseAmount() { return baseAmount; }
    public void setBaseAmount(BigDecimal baseAmount) { this.baseAmount = baseAmount; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public BusinessPartner getBusinessPartner() { return businessPartner; }
    public void setBusinessPartner(BusinessPartner businessPartner) { this.businessPartner = businessPartner; }

    public String getDetailDescription() { return detailDescription; }
    public void setDetailDescription(String detailDescription) { this.detailDescription = detailDescription; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getAuditUser() { return auditUser; }
    public void setAuditUser(String auditUser) { this.auditUser = auditUser; }
}
