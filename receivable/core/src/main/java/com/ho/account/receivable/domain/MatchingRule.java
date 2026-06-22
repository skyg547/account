package com.ho.account.receivable.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 수금과 매출채권을 자동 매칭하기 위한 규칙 엔티티.
 */
@Entity
@Table(name = "matching_rules")
public class MatchingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String ruleName; // 규칙 명칭

    @Column(nullable = false)
    private Integer priority; // 규칙 적용 우선순위 (낮은 숫자가 높은 우선순위)

    @Column(length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private MatchingCriteria matchCriteria; // 매칭 기준 (예: REFERENCE_NO_EXACT, AMOUNT_FUZZY)

    @Column(precision = 19, scale = 2)
    private BigDecimal toleranceAmount; // 허용 오차 금액 (AMOUNT_FUZZY 매칭 시)

    @Column(nullable = false)
    private boolean isActive = true; // 규칙 활성화 여부

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getter 및 Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public MatchingCriteria getMatchCriteria() {
        return matchCriteria;
    }

    public void setMatchCriteria(MatchingCriteria matchCriteria) {
        this.matchCriteria = matchCriteria;
    }

    public BigDecimal getToleranceAmount() {
        return toleranceAmount;
    }

    public void setToleranceAmount(BigDecimal toleranceAmount) {
        this.toleranceAmount = toleranceAmount;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
