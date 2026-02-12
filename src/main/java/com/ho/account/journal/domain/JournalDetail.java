package com.ho.account.journal.domain;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Department;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "journal_details")
public class JournalDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    @Column(nullable = false, length = 10)
    private String drcrType; // DEBIT(차변), CREDIT(대변)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_code", nullable = false)
    private AccountSubject accountSubject;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_code", referencedColumnName = "code")
    private Department department; // 귀속부서

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_partner_code", referencedColumnName = "businessPartnerCode")
    private BusinessPartner businessPartner; // 거래처

    @Column(length = 200)
    private String detailDescription; // 라인 적요

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public JournalEntry getJournalEntry() {
        return journalEntry;
    }

    public void setJournalEntry(JournalEntry journalEntry) {
        this.journalEntry = journalEntry;
    }

    public String getDrcrType() {
        return drcrType;
    }

    public void setDrcrType(String drcrType) {
        this.drcrType = drcrType;
    }

    public AccountSubject getAccountSubject() {
        return accountSubject;
    }

    public void setAccountSubject(AccountSubject accountSubject) {
        this.accountSubject = accountSubject;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public BusinessPartner getBusinessPartner() {
        return businessPartner;
    }

    public void setBusinessPartner(BusinessPartner businessPartner) {
        this.businessPartner = businessPartner;
    }

    public String getDetailDescription() {
        return detailDescription;
    }

    public void setDetailDescription(String detailDescription) {
        this.detailDescription = detailDescription;
    }
}
