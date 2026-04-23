package com.ho.account.journalledger.domain.journal.domain;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Department;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ?꾪몴 ?곸꽭(Journal Detail) ?뷀???
 * ?�꾧�??꾪몴??媛쒕????�씤(李⑤?/??蹂)???�?�ы븯�? ?�꾩?�怨쇰?? 湲덉�? 洹??????깆쓣 ??�??
 */
@Entity
@Table(name = "journal_details", indexes = {
    @Index(name = "idx_journal_detail_journal_entry_id", columnList = "journal_entry_id"),
    @Index(name = "idx_journal_detail_account_subject_id", columnList = "account_code"),
    @Index(name = "idx_journal_detail_department_id", columnList = "dept_code"),
    @Index(name = "idx_journal_detail_business_partner_id", columnList = "business_partner_code"),
    @Index(name = "idx_journal_detail_drcr_type", columnList = "drcrType")
})
public class JournalDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    @Column(nullable = false, length = 10)
    private String drcrType; // DEBIT(李⑤?), CREDIT(??蹂)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_code", nullable = false)
    private AccountSubject accountSubject;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount; // 嫄곕?????�� 湲덉�?

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal baseAmount = BigDecimal.ZERO; // 湲곗? ???�� 湲덉�?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_code", referencedColumnName = "code")
    private Department department; // 洹?????

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_partner_code", referencedColumnName = "businessPartnerCode")
    private BusinessPartner businessPartner; // 嫄곕?�泥?

    @Column(length = 200)
    private String detailDescription; // ??�씤 ?곸슂

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

    // Getter �?Setter
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
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

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(BigDecimal baseAmount) {
        this.baseAmount = baseAmount;
    }
}
