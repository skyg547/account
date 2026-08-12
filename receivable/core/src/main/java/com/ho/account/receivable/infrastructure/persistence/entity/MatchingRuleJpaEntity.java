package com.ho.account.receivable.infrastructure.persistence.entity;

import com.ho.account.receivable.domain.MatchingCriteria;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MatchingRule JPA 영속성 엔티티.
 * DB `matching_rules` 테이블과 매핑되며, 도메인 POJO(MatchingRule)와 분리하여 관리합니다.
 */
@Entity
@Table(name = "matching_rules")
public class MatchingRuleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String ruleName;

    @Column(nullable = false)
    private Integer priority;

    @Column(length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private MatchingCriteria matchCriteria;

    @Column(precision = 19, scale = 2)
    private BigDecimal toleranceAmount;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public MatchingCriteria getMatchCriteria() { return matchCriteria; }
    public void setMatchCriteria(MatchingCriteria matchCriteria) { this.matchCriteria = matchCriteria; }

    public BigDecimal getToleranceAmount() { return toleranceAmount; }
    public void setToleranceAmount(BigDecimal toleranceAmount) { this.toleranceAmount = toleranceAmount; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
