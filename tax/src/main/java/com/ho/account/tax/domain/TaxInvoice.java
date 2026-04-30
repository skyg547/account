package com.ho.account.tax.domain;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tax_invoices")
public class TaxInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String issueId; // ?뱀씤踰덊샇

    @Column(nullable = false)
    private String type; // SALES(留ㅼ텧), PURCHASE(留ㅼ엯)

    @Column(nullable = false)
    private LocalDate issueDate; // ?묒꽦?쇱옄

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_partner_code", referencedColumnName = "businessPartnerCode")
    private BusinessPartner businessPartner; // 怨듦툒諛쏅뒗??留ㅼ텧) ?먮뒗 怨듦툒??留ㅼ엯)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal supplyAmount; // 怨듦툒媛??

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount; // ?몄븸

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount; // ?⑷퀎湲덉븸

    // ?꾪몴 ?곌껐 (?좏깮)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry;

    public void validateAmounts() {
        if (supplyAmount == null || taxAmount == null || totalAmount == null) {
            throw new IllegalArgumentException("공급가액, 세액, 합계금액은 필수입니다.");
        }
        if (supplyAmount.add(taxAmount).compareTo(totalAmount) != 0) {
            throw new IllegalArgumentException("공급가액과 세액의 합이 합계금액과 일치하지 않습니다.");
        }
    }

    public boolean isPurchaseType() {
        return "PURCHASE".equals(this.type);
    }

    public void updateInfo(String issueId, LocalDate issueDate, BusinessPartner businessPartner, 
                          BigDecimal supplyAmount, BigDecimal taxAmount, BigDecimal totalAmount) {
        this.issueId = issueId;
        this.issueDate = issueDate;
        this.businessPartner = businessPartner;
        this.supplyAmount = supplyAmount;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        validateAmounts();
    }

    // Getter 및 Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIssueId() { return issueId; }
    public void setIssueId(String issueId) { this.issueId = issueId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public BusinessPartner getBusinessPartner() { return businessPartner; }
    public void setBusinessPartner(BusinessPartner businessPartner) { this.businessPartner = businessPartner; }

    public BigDecimal getSupplyAmount() { return supplyAmount; }
    public void setSupplyAmount(BigDecimal supplyAmount) { this.supplyAmount = supplyAmount; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public JournalEntry getJournalEntry() { return journalEntry; }
    public void setJournalEntry(JournalEntry journalEntry) { this.journalEntry = journalEntry; }
}
