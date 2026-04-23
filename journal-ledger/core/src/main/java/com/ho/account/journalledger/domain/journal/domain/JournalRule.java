package com.ho.account.journalledger.domain.journal.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ?∫ÍæßÏª?Ê¥πÏíñ??Journal Rule) ?Î∑Ä???
 * Â´ÑÍ≥ï???Ï¢èÏÇéËπÇÍæ®Ï§??Î®?£û ?∫ÍæßÏª?Ôß£ÏÑé?ÅÁëú??Íæ™Î∏≥ Ê¥πÏíñ????ø¬Ä?±—ãÎ∏ø.
 */
@Entity
@Table(name = "journal_rules")
public class JournalRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ??Ä? ??∏ÌÄ??

    @Column(nullable = false, unique = true, length = 50)
    private String ruleCode; // Ê¥πÏíñ???ÑÎ∂æÎ±?

    @Column(nullable = false, length = 100)
    private String ruleName; // Ê¥πÏíñ?ÉÔßè?

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private LocalDate validFrom;

    private LocalDate validTo; // ?Íæ©Ïò± ??ñÍΩ¶ Ê¥πÏíñ???Ä??null ??âÏäú

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private int priority; // ??¨ÏòÑÂ™õ¬Ä ?????éÏ§â ?Í≥óÍΩë??ñÏêûÂ™õ¬Ä ?ÎØ™Ïì¨

    @OneToMany(mappedBy = "journalRule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalRuleCondition> conditions = new ArrayList<>();

    @OneToMany(mappedBy = "journalRule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalRuleDetail> ruleDetails = new ArrayList<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    private String createdBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.validFrom == null)
            this.validFrom = LocalDate.now();
        if (this.version == 0)
            this.version = 1;
        if (this.priority == 0)
            this.priority = 999; // Êπ≤Í≥ï????? ?Í≥óÍΩë??ñÏêû
        if (this.ruleCode == null)
            throw new IllegalArgumentException("Rule code cannot be null");
        if (this.ruleName == null)
            throw new IllegalArgumentException("Rule name cannot be null");
        if (this.auditUser == null)
            this.auditUser = this.createdBy != null ? this.createdBy : "SYSTEM";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getter Ë´?Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public List<JournalRuleCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<JournalRuleCondition> conditions) {
        this.conditions = conditions;
    }

    public List<JournalRuleDetail> getRuleDetails() {
        return ruleDetails;
    }

    public void setRuleDetails(List<JournalRuleDetail> ruleDetails) {
        this.ruleDetails = ruleDetails;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAuditUser() {
        return auditUser;
    }

    public void setAuditUser(String auditUser) {
        this.auditUser = auditUser;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    // ?Í≥??ø¬Ä??????ÔßéÎ∂øÍΩ??
    public void addCondition(JournalRuleCondition condition) {
        conditions.add(condition);
        condition.setJournalRule(this);
    }

    public void addRuleDetail(JournalRuleDetail ruleDetail) {
        ruleDetails.add(ruleDetail);
        ruleDetail.setJournalRule(this);
    }
}
