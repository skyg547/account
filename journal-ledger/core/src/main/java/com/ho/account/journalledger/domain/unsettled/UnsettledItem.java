package com.ho.account.journalledger.domain.unsettled;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 미결 항목(Unsettled Item) 엔티티
 * 반제(Settlement) 처리가 필요한 항목(예: 외상매입금, 미지급금 등)을 관리한다.
 */
@Entity
@Table(name = "unsettled_items")
public class UnsettledItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_detail_id", nullable = false)
    private JournalDetail journalDetail; // 발생 전표 상세

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_code", nullable = false)
    private AccountSubject accountSubject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_partner_code", referencedColumnName = "businessPartnerCode")
    private BusinessPartner businessPartner;

    @Column(nullable = false)
    private LocalDate occurrenceDate; // 발생일

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal originalAmount; // 발생 금액

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal settledAmount = BigDecimal.ZERO; // 반제 금액

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal remainingAmount; // 잔액

    @Column(length = 20)
    private String status; // OPEN(미결), PARTIAL(부분반제), CLEARED(반제완료)

    @Column(nullable = false)
    private boolean resolved = false; // 반제 완료 여부 (Repository 쿼리용)

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
     * 반제 처리 로직
     * @param amount 반제할 금액
     */
    public void settle(BigDecimal amount) {
        if (remainingAmount.compareTo(amount) < 0) {
            throw new IllegalArgumentException("반제 금액이 잔액보다 클 수 없습니다.");
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
