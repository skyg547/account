package com.ho.account.journalledger.domain.unsettled;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 誘멸껐 ??ぉ(Unsettled Item) ?뷀떚??
 * 諛섏젣(Settlement) 泥섎━媛 ?꾩슂????ぉ(?? ?몄긽留ㅼ엯湲? 誘몄?湲됯툑 ????愿由ы븳??
 */
@Entity
@Table(name = "unsettled_items")
public class UnsettledItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_detail_id", nullable = false)
    private JournalDetail journalDetail; // 諛쒖깮 ?꾪몴 ?곸꽭

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_code", nullable = false)
    private AccountSubject accountSubject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_partner_code", referencedColumnName = "businessPartnerCode")
    private BusinessPartner businessPartner;

    @Column(nullable = false)
    private LocalDate occurrenceDate; // 諛쒖깮??

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal originalAmount; // 諛쒖깮 湲덉븸

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal settledAmount = BigDecimal.ZERO; // 諛섏젣 湲덉븸

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal remainingAmount; // ?붿븸

    @Column(length = 20)
    private String status; // OPEN(誘멸껐), PARTIAL(遺遺꾨컲??, CLEARED(諛섏젣?꾨즺)

    @Column(nullable = false)
    private boolean resolved = false; // 諛섏젣 ?꾨즺 ?щ? (Repository 荑쇰━??

    @PrePersist
    protected void onCreate() {
        if (status == null) status = "OPEN";
        if (remainingAmount == null) remainingAmount = originalAmount;
        updateResolvedStatus();
    }

    private void updateResolvedStatus() {
        this.resolved = "CLEARED".equals(this.status);
    }

    /**
     * 諛섏젣 泥섎━ 濡쒖쭅
     * @param amount 諛섏젣??湲덉븸
     */
    public void settle(BigDecimal amount) {
        if (remainingAmount.compareTo(amount) < 0) {
            throw new IllegalArgumentException("諛섏젣 湲덉븸???붿븸蹂대떎 ?????놁뒿?덈떎.");
        }
        this.settledAmount = this.settledAmount.add(amount);
        this.remainingAmount = this.remainingAmount.subtract(amount);
        
        if (this.remainingAmount.compareTo(BigDecimal.ZERO) == 0) {
            this.status = "CLEARED";
        } else {
            this.status = "PARTIAL";
        }
        updateResolvedStatus();
    }

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public JournalDetail getJournalDetail() { return journalDetail; }
    public void setJournalDetail(JournalDetail journalDetail) { this.journalDetail = journalDetail; }

    public AccountSubject getAccountSubject() { return accountSubject; }
    public void setAccountSubject(AccountSubject accountSubject) { this.accountSubject = accountSubject; }

    public BusinessPartner getBusinessPartner() { return businessPartner; }
    public void setBusinessPartner(BusinessPartner businessPartner) { this.businessPartner = businessPartner; }

    public LocalDate getOccurrenceDate() { return occurrenceDate; }
    public void setOccurrenceDate(LocalDate occurrenceDate) { this.occurrenceDate = occurrenceDate; }

    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }

    public BigDecimal getSettledAmount() { return settledAmount; }
    public void setSettledAmount(BigDecimal settledAmount) { this.settledAmount = settledAmount; }

    public BigDecimal getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(BigDecimal remainingAmount) { this.remainingAmount = remainingAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
}
