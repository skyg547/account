package com.ho.account.journalledger.domain.journal.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 자동 분개 규칙 명세 (Journal Rule Detail) — 규칙 적용 시 생성할 전표 라인 정의.
 *
 * ─────────────────────────────────────────────────
 * [업무 설명]
 * JournalRule의 조건이 충족됐을 때 실제로 생성할 전표 라인(분개 명세)을 정의합니다.
 * 하나의 규칙에 여러 개의 명세 라인이 있을 수 있으며, 각 라인이 JournalDetail 한 줄이 됩니다.
 *
 * 예시) "매입 인식" 규칙의 명세:
 *   [명세1] DEBIT  | 계정: "50100"(매입 비용)  | 금액: "${transaction.amount}"
 *   [명세2] DEBIT  | 계정: "13500"(부가세 대급금) | 금액: "${transaction.amount} * 0.1"
 *   [명세3] CREDIT | 계정: "21100"(매입채무)   | 금액: "${transaction.totalAmount}"
 *
 * 표현식(Expression) 방식:
 *   - 고정값: "10100", "1000000"
 *   - 동적값: "${transaction.accountCode}", "${transaction.amount}"
 *   - 수식:   "${transaction.amount} * 0.1" (부가세 10%)
 *   JournalRuleEngine에서 이벤트 데이터로 표현식을 평가(eval)합니다.
 *
 * ─────────────────────────────────────────────────
 * [개발 설명]
 * - journal_rule_details 테이블에 매핑됩니다.
 * - JournalRule과 @ManyToOne 관계입니다.
 * - 차대변 값은 JournalSide 열거형으로 제한하고 DB에는 "DEBIT" 또는 "CREDIT" 문자열로 저장합니다.
 * - 표현식 평가는 JournalRuleEngine이 필수값 누락, 문자열 결합, 금액 수식을 구분하여 수행합니다.
 * ─────────────────────────────────────────────────
 */
@Entity
@Table(name = "journal_rule_details")
public class JournalRuleDetail {

    /** 시스템 내부 PK (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 소속 분개 규칙.
     * 이 명세 라인이 어느 JournalRule에 속하는지를 나타냅니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private JournalRule journalRule;

    /**
     * 차변/대변 구분.
     * "DEBIT" 또는 "CREDIT" 문자열로 저장됩니다.
     * JournalRuleEngine이 이 값을 JournalSide 열거형으로 변환하여 JournalDetail에 설정합니다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "drcr_type", nullable = false, length = 10)
    private JournalSide side;

    /**
     * 계정과목 코드 표현식.
     * 고정 코드: "10100" (현금 계정)
     * 동적 코드: "${transaction.accountCode}" (이벤트 데이터의 accountCode 필드 값 사용)
     * JournalRuleEngine에서 이벤트 데이터를 바탕으로 실제 계정 코드로 평가합니다.
     */
    @Column(nullable = false, length = 100)
    private String accountSubjectCodeExpression;

    /**
     * 금액 표현식.
     * 고정 금액:  "1000000"
     * 동적 금액:  "${transaction.amount}"
     * 수식:       "${transaction.amount} * 0.1" (예: 부가세 10% 계산)
     * JournalRuleEngine에서 이벤트 데이터를 바탕으로 BigDecimal로 평가합니다.
     */
    @Column(nullable = false, length = 100)
    private String amountExpression;

    /**
     * 적요 표현식 (선택).
     * null이면 전표 헤더의 description을 사용합니다.
     * 동적 적요: "${transaction.description}"
     */
    @Column(length = 255)
    private String descriptionExpression;

    /**
     * 거래처 코드 표현식 (선택).
     * 고정 코드:  "BP001"
     * 동적 코드:  "${transaction.businessPartnerCode}"
     * null이면 거래처 미설정.
     */
    @Column(length = 50)
    private String businessPartnerCodeExpression;

    /**
     * 부서 코드 표현식 (선택).
     * 고정 코드:  "D001"
     * 동적 코드:  "${transaction.departmentCode}"
     * null이면 부서 미설정.
     */
    @Column(length = 50)
    private String departmentCodeExpression;

    /** 최초 생성 일시 (수정 불가) */
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** 최종 수정 일시 */
    private LocalDateTime updatedAt;

    /** 처리자 */
    @Column(length = 50)
    private String auditUser;

    // ─── 생명주기 콜백 ──────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.auditUser == null) this.auditUser = "SYSTEM";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ─── Getter / Setter ──────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public JournalRule getJournalRule() { return journalRule; }
    public void setJournalRule(JournalRule journalRule) { this.journalRule = journalRule; }

    public JournalSide getSide() { return side; }
    public void setSide(JournalSide side) { this.side = side; }

    /**
     * 기존 규칙 등록 코드와의 호환용 문자열 접근자입니다.
     * 내부 상태는 JournalSide이므로 잘못된 차대변 값은 저장 전에 즉시 차단됩니다.
     */
    public String getDrcrType() { return side == null ? null : side.name(); }
    public void setDrcrType(String drcrType) {
        this.side = drcrType == null ? null : JournalSide.valueOf(drcrType.trim().toUpperCase());
    }

    public String getAccountSubjectCodeExpression() { return accountSubjectCodeExpression; }
    public void setAccountSubjectCodeExpression(String expr) { this.accountSubjectCodeExpression = expr; }

    public String getAmountExpression() { return amountExpression; }
    public void setAmountExpression(String expr) { this.amountExpression = expr; }

    public String getDescriptionExpression() { return descriptionExpression; }
    public void setDescriptionExpression(String expr) { this.descriptionExpression = expr; }

    public String getBusinessPartnerCodeExpression() { return businessPartnerCodeExpression; }
    public void setBusinessPartnerCodeExpression(String expr) { this.businessPartnerCodeExpression = expr; }

    public String getDepartmentCodeExpression() { return departmentCodeExpression; }
    public void setDepartmentCodeExpression(String expr) { this.departmentCodeExpression = expr; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getAuditUser() { return auditUser; }
    public void setAuditUser(String auditUser) { this.auditUser = auditUser; }
}
