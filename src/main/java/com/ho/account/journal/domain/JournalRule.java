package com.ho.account.journal.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "journal_rules")
public class JournalRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String ruleCode;

    @Column(nullable = false, length = 100)
    private String ruleName;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private LocalDate validFrom;

    private LocalDate validTo; // Nullable for currently active rule

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private int priority; // Lower number means higher priority

    @OneToMany(mappedBy = "journalRule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalRuleCondition> conditions = new ArrayList<>();

    @OneToMany(mappedBy = "journalRule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalRuleDetail> ruleDetails = new ArrayList<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (validFrom == null) validFrom = LocalDate.now();
        if (version == 0) version = 1;
        if (priority == 0) priority = 999; // Default low priority
        if (ruleCode == null) throw new IllegalArgumentException("Rule code cannot be null");
        if (ruleName == null) throw new IllegalArgumentException("Rule name cannot be null");
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }

    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public List<JournalRuleCondition> getConditions() { return conditions; }
    public void setConditions(List<JournalRuleCondition> conditions) { this.conditions = conditions; }

    public List<JournalRuleDetail> getRuleDetails() { return ruleDetails; }
    public void setRuleDetails(List<JournalRuleDetail> ruleDetails) { this.ruleDetails = ruleDetails; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    // Helper methods for relationships
    public void addCondition(JournalRuleCondition condition) {
        conditions.add(condition);
        condition.setJournalRule(this);
    }

    public void addRuleDetail(JournalRuleDetail ruleDetail) {
        ruleDetails.add(ruleDetail);
        ruleDetail.setJournalRule(this);
    }
}
