package com.ho.account.journalledger.domain.ledger;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.domain.model.Currency;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ËπÇÎåÅ??Î®?ò£ ?Í≥∏ÍΩ≠ (SL Entry)
 * Â´ÑÍ≥ï?íÔß£?BusinessPartner) Ë´??∫¬Ä??Department) ?Í≥∏ÍΩ≠Â™õ¬Ä ??Î∏???Î®?ò£ ?Í≥∏ÍΩ≠ ??ÅÎø≠.
 */
@Entity
@Table(name = "sl_entries", indexes = {
    @Index(name = "idx_sl_entry_bp_acct_date", columnList = "business_partner_id, account_code, postingDate")
})
public class SlEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_detail_id", nullable = false)
    private JournalDetail journalDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_code", referencedColumnName = "code", nullable = false)
    private AccountSubject account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_partner_id")
    private BusinessPartner businessPartner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(nullable = false, length = 4)
    private String fiscalYear;

    @Column(nullable = false, length = 2)
    private String fiscalPeriod;

    @Column(nullable = false)
    private LocalDate postingDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal drAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal crAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", nullable = false)
    private Currency currency;

    @Column(precision = 19, scale = 8)
    private BigDecimal exchangeRate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal baseDrAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal baseCrAmount = BigDecimal.ZERO;

    @Column(length = 50)
    private String lineageSourceType;

    @Column(length = 100)
    private String lineageSourceId;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(length = 50)
    private String auditUser;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.auditUser == null)
            this.auditUser = "SYSTEM";
    }

    // Getter Ë´?Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public JournalDetail getJournalDetail() { return journalDetail; }
    public void setJournalDetail(JournalDetail journalDetail) { this.journalDetail = journalDetail; }
    
    public AccountSubject getAccount() { return account; }
    public void setAccount(AccountSubject account) { this.account = account; }

    public BusinessPartner getBusinessPartner() { return businessPartner; }
    public void setBusinessPartner(BusinessPartner businessPartner) { this.businessPartner = businessPartner; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    
    public String getFiscalYear() { return fiscalYear; }
    public void setFiscalYear(String fiscalYear) { this.fiscalYear = fiscalYear; }
    
    public String getFiscalPeriod() { return fiscalPeriod; }
    public void setFiscalPeriod(String fiscalPeriod) { this.fiscalPeriod = fiscalPeriod; }
    
    public LocalDate getPostingDate() { return postingDate; }
    public void setPostingDate(LocalDate postingDate) { this.postingDate = postingDate; }
    
    public BigDecimal getDrAmount() { return drAmount; }
    public void setDrAmount(BigDecimal drAmount) { this.drAmount = drAmount; }
    
    public BigDecimal getCrAmount() { return crAmount; }
    public void setCrAmount(BigDecimal crAmount) { this.crAmount = crAmount; }
    
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
    
    public BigDecimal getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(BigDecimal exchangeRate) { this.exchangeRate = exchangeRate; }
    
    public BigDecimal getBaseDrAmount() { return baseDrAmount; }
    public void setBaseDrAmount(BigDecimal baseDrAmount) { this.baseDrAmount = baseDrAmount; }
    
    public BigDecimal getBaseCrAmount() { return baseCrAmount; }
    public void setBaseCrAmount(BigDecimal baseCrAmount) { this.baseCrAmount = baseCrAmount; }
    
    public String getLineageSourceType() { return lineageSourceType; }
    public void setLineageSourceType(String lineageSourceType) { this.lineageSourceType = lineageSourceType; }
    
    public String getLineageSourceId() { return lineageSourceId; }
    public void setLineageSourceId(String lineageSourceId) { this.lineageSourceId = lineageSourceId; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    
    public String getAuditUser() { return auditUser; }
    public void setAuditUser(String auditUser) { this.auditUser = auditUser; }
}
