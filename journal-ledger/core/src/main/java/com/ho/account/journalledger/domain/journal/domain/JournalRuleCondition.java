package com.ho.account.journalledger.domain.journal.domain;

import jakarta.persistence.*;

/**
 * 자동 분개 규칙 조건 (Journal Rule Condition).
 *
 * ─────────────────────────────────────────────────
 * [업무 설명]
 * JournalRule이 특정 이벤트에 적용될지 판단하기 위한 조건을 정의합니다.
 * 하나의 규칙에 여러 조건이 있을 수 있으며, 모든 조건이 충족(AND)되어야 규칙이 발동됩니다.
 *
 * 예시:
 *   [조건1] field="transactionType" operator=EQUALS value="PURCHASE"
 *   [조건2] field="departmentCode"  operator=STARTS_WITH value="D0"
 *   → 거래유형이 PURCHASE이고 부서코드가 D0으로 시작하는 이벤트에만 적용
 *
 * 주요 필드:
 *   - field    : 이벤트 데이터(Map)에서 비교할 키 이름
 *                예: "transactionType", "productCode", "departmentCode", "amount"
 *   - operator : 비교 방식 (EQUALS, STARTS_WITH, GREATER_THAN 등)
 *   - value    : 비교 기준값 (문자열로 저장, 숫자 비교 시 파싱 필요)
 *
 * ─────────────────────────────────────────────────
 * [개발 설명]
 * - journal_rule_conditions 테이블에 매핑됩니다.
 * - JournalRule과 @ManyToOne 관계입니다.
 * - JournalRuleEngine에서 이벤트 데이터(Map<String, Object>)와 이 조건을 비교하여
 *   규칙 적용 여부를 결정합니다.
 * - 현재 JournalRuleEngine 구현은 stub 상태입니다.
 * ─────────────────────────────────────────────────
 */
@Entity
@Table(name = "journal_rule_conditions")
public class JournalRuleCondition {

    /** 시스템 내부 PK (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 소속 분개 규칙.
     * 이 조건이 어느 JournalRule에 속하는지를 나타냅니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private JournalRule journalRule;

    /**
     * 이벤트 데이터에서 비교할 필드명.
     * JournalRuleEngine이 받는 eventData Map의 key와 일치해야 합니다.
     * 예: "transactionType", "productCode", "departmentCode", "amount"
     */
    @Column(nullable = false, length = 50)
    private String field;

    /**
     * 비교 연산자.
     * ConditionOperator 열거형을 참고하세요.
     * 예: EQUALS, STARTS_WITH, GREATER_THAN 등
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConditionOperator operator;

    /**
     * 비교 기준값 (문자열 형태로 저장).
     * 문자열 비교: 그대로 사용.
     * 숫자 비교(GREATER_THAN 등): JournalRuleEngine에서 BigDecimal로 파싱하여 비교합니다.
     * 예: "PURCHASE", "D0", "1000000"
     */
    @Column(name = "condition_value", nullable = false, length = 255)
    private String value;

    // ─── Getter / Setter ──────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public JournalRule getJournalRule() { return journalRule; }
    public void setJournalRule(JournalRule journalRule) { this.journalRule = journalRule; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public ConditionOperator getOperator() { return operator; }
    public void setOperator(ConditionOperator operator) { this.operator = operator; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
