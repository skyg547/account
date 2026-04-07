package com.ho.account.journal.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "journal_rule_details")
public class JournalRuleDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private JournalRule journalRule;

    @Column(nullable = false, length = 10)
    private String drcrType; // 차변 또는 대변

    @Column(nullable = false, length = 100)
    private String accountSubjectCodeExpression; // 예: "10100", "${transaction.accountCode}"

    @Column(nullable = false, length = 100)
    private String amountExpression; // 예: "1000", "${transaction.amount}", "${transaction.amount} * 0.1"

    @Column(length = 255)
    private String descriptionExpression; // 예: "매출", "${transaction.description}"

    @Column(length = 50)
    private String businessPartnerCodeExpression; // 예: "BP001", "${transaction.businessPartnerCode}"

    @Column(length = 50)
    private String departmentCodeExpression; // 예: "D001", "${transaction.departmentCode}"

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.auditUser == null)
            this.auditUser = "SYSTEM";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getter 및 Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public JournalRule getJournalRule() {
        return journalRule;
    }

    public void setJournalRule(JournalRule journalRule) {
        this.journalRule = journalRule;
    }

    public String getDrcrType() {
        return drcrType;
    }

    public void setDrcrType(String drcrType) {
        this.drcrType = drcrType;
    }

    public String getAccountSubjectCodeExpression() {
        return accountSubjectCodeExpression;
    }

    public void setAccountSubjectCodeExpression(String accountSubjectCodeExpression) {
        this.accountSubjectCodeExpression = accountSubjectCodeExpression;
    }

    public String getAmountExpression() {
        return amountExpression;
    }

    public void setAmountExpression(String amountExpression) {
        this.amountExpression = amountExpression;
    }

    public String getDescriptionExpression() {
        return descriptionExpression;
    }

    public void setDescriptionExpression(String descriptionExpression) {
        this.descriptionExpression = descriptionExpression;
    }

    public String getBusinessPartnerCodeExpression() {
        return businessPartnerCodeExpression;
    }

    public void setBusinessPartnerCodeExpression(String businessPartnerCodeExpression) {
        this.businessPartnerCodeExpression = businessPartnerCodeExpression;
    }

    public String getDepartmentCodeExpression() {
        return departmentCodeExpression;
    }

    public void setDepartmentCodeExpression(String departmentCodeExpression) {
        this.departmentCodeExpression = departmentCodeExpression;
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
}
