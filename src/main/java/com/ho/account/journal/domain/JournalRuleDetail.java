package com.ho.account.journal.domain;

import jakarta.persistence.*;
import java.math.BigDecimal; // Import for BigDecimal, though it's an expression string here

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
    private String drcrType; // DEBIT or CREDIT

    @Column(nullable = false, length = 100)
    private String accountSubjectCodeExpression; // e.g., "10100", "${transaction.accountCode}"

    @Column(nullable = false, length = 100)
    private String amountExpression; // e.g., "1000", "${transaction.amount}", "${transaction.amount} * 0.1"

    @Column(length = 255)
    private String descriptionExpression; // e.g., "매출", "${transaction.description}"

    @Column(length = 50)
    private String businessPartnerCodeExpression; // e.g., "BP001", "${transaction.businessPartnerCode}"

    @Column(length = 50)
    private String departmentCodeExpression; // e.g., "D001", "${transaction.departmentCode}"

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public JournalRule getJournalRule() { return journalRule; }
    public void setJournalRule(JournalRule journalRule) { this.journalRule = journalRule; }

    public String getDrcrType() { return drcrType; }
    public void setDrcrType(String drcrType) { this.drcrType = drcrType; }

    public String getAccountSubjectCodeExpression() { return accountSubjectCodeExpression; }
    public void setAccountSubjectCodeExpression(String accountSubjectCodeExpression) { this.accountSubjectCodeExpression = accountSubjectCodeExpression; }

    public String getAmountExpression() { return amountExpression; }
    public void setAmountExpression(String amountExpression) { this.amountExpression = amountExpression; }

    public String getDescriptionExpression() { return descriptionExpression; }
    public void setDescriptionExpression(String descriptionExpression) { this.descriptionExpression = descriptionExpression; }

    public String getBusinessPartnerCodeExpression() { return businessPartnerCodeExpression; }
    public void setBusinessPartnerCodeExpression(String businessPartnerCodeExpression) { this.businessPartnerCodeExpression = businessPartnerCodeExpression; }

    public String getDepartmentCodeExpression() { return departmentCodeExpression; }
    public void setDepartmentCodeExpression(String departmentCodeExpression) { this.departmentCodeExpression = departmentCodeExpression; }
}
