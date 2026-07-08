package com.ho.account.receivable.api.dto;

import com.ho.account.receivable.domain.MatchingCriteria;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class MatchingRuleRequest {

    private Long id; // for updates

    @NotBlank(message = "규칙 명칭은 필수입니다.")
    private String ruleName;

    @NotNull(message = "우선순위는 필수입니다.")
    @Min(value = 1, message = "우선순위는 1 이상이어야 합니다.")
    private Integer priority;

    @NotNull(message = "매칭 기준은 필수입니다.")
    private MatchingCriteria matchCriteria;

    private BigDecimal toleranceAmount;

    @NotNull(message = "규칙 활성화 여부는 필수입니다.")
    private boolean isActive;

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
}
