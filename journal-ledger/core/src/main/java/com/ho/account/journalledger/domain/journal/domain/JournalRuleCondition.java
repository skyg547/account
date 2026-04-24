package com.ho.account.journalledger.domain.journal.domain;

import jakarta.persistence.*;

/**
 * 전표 규칙 조건(Journal Rule Condition) 엔티티
 * 전표 규칙이 적용되기 위한 거래 데이터의 조건을 정의한다.
 */
@Entity
@Table(name = "journal_rule_conditions")
public class JournalRuleCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private JournalRule journalRule;

    @Column(nullable = false, length = 50)
    private String field; // 예: "transactionType", "productCode", "departmentCode"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConditionOperator operator; // 예: EQUALS, STARTS_WITH, CONTAINS

    @Column(nullable = false, length = 255)
    private String value; // 비교할 값

    // Getter 및 Setter
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
