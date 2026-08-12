package com.ho.account.receivable.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 수금과 매출채권을 자동 매칭하기 위한 규칙 엔티티 (Pure Java POJO).
 *
 * 🐣 [Hexagonal Architecture & Pure Java POJO 교육적 주석]
 * 도메인 모델(MatchingRule)은 기술 프레임워크 독립성을 보유합니다.
 */
public class MatchingRule {

    private Long id;
    private String ruleName;
    private Integer priority;
    private MatchingCriteria matchCriteria;
    private BigDecimal toleranceAmount;
    private boolean isActive = true;
    private LocalDateTime createdAt = LocalDateTime.now();

    public MatchingRule() {
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
        this.isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
