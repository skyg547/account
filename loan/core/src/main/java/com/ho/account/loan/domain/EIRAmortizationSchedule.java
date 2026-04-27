package com.ho.account.loan.domain;

import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * EIR ?ÅÍ∞Å ?§Ï?Ï§?(EIR Amortization Schedule) ?îÌã∞??
 * ?†Ìö®?¥Ïûê??EIR) Î∞©Î≤ï???∞Îùº Í≥ÑÏÇ∞???ÄÏ∂úÏùò Í∏∞Í∞ÑÎ≥??ÅÍ∞Å ?§Ï?Ï§ÑÏùÑ Í∏∞Î°ù?©Îãà??
 */
@Entity
@Table(name = "eir_amortization_schedules")
public class EIRAmortizationSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(nullable = false)
    private LocalDate scheduleDate; // ?¥Îãπ ?§Ï?Ï§ÑÏùò Í∏∞Ï???(?ºÎ≥Ñ/?îÎ≥Ñ ??

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal beginningBalance; // Í∏∞Ï¥à ?îÏï°

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal interestIncome; // ?¥Ïûê ?òÏùµ (EIR ?ÅÏö©)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal principalRepayment; // ?êÍ∏à ?ÅÌôò??

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal endingBalance; // Í∏∞Îßê ?îÏï°

    @Column(precision = 19, scale = 2)
    private BigDecimal deferredItemAmortization; // ?¥Ïó∞ ??™© ?ÅÍ∞Å??(?òÏàòÎ£? ÎπÑÏö© ??

    @Column(precision = 19, scale = 2)
    private BigDecimal cashFlow; // ?¥Îãπ Í∏∞Í∞Ñ ?ÑÍ∏à ?êÎ¶Ñ

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "amortization_journal_entry_id")
    private JournalEntry amortizationJournalEntry; // ?ÅÍ∞Å Í¥Ä??Î∂ÑÍ∞ú ?ÑÌëú

    @Column(nullable = false)
    private boolean isRecalculated = false; // ?¨Í≥Ñ?∞Îêú ?§Ï?Ï§???™©?∏Ï? ?¨Î?

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

    // Getter Î∞?Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Loan getLoan() {
        return loan;
    }

    public void setLoan(Loan loan) {
        this.loan = loan;
    }

    public LocalDate getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(LocalDate scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public BigDecimal getBeginningBalance() {
        return beginningBalance;
    }

    public void setBeginningBalance(BigDecimal beginningBalance) {
        this.beginningBalance = beginningBalance;
    }

    public BigDecimal getInterestIncome() {
        return interestIncome;
    }

    public void setInterestIncome(BigDecimal interestIncome) {
        this.interestIncome = interestIncome;
    }

    public BigDecimal getPrincipalRepayment() {
        return principalRepayment;
    }

    public void setPrincipalRepayment(BigDecimal principalRepayment) {
        this.principalRepayment = principalRepayment;
    }

    public BigDecimal getEndingBalance() {
        return endingBalance;
    }

    public void setEndingBalance(BigDecimal endingBalance) {
        this.endingBalance = endingBalance;
    }

    public BigDecimal getDeferredItemAmortization() {
        return deferredItemAmortization;
    }

    public void setDeferredItemAmortization(BigDecimal deferredItemAmortization) {
        this.deferredItemAmortization = deferredItemAmortization;
    }

    public BigDecimal getCashFlow() {
        return cashFlow;
    }

    public void setCashFlow(BigDecimal cashFlow) {
        this.cashFlow = cashFlow;
    }

    public JournalEntry getAmortizationJournalEntry() {
        return amortizationJournalEntry;
    }

    public void setAmortizationJournalEntry(JournalEntry amortizationJournalEntry) {
        this.amortizationJournalEntry = amortizationJournalEntry;
    }

    public boolean isRecalculated() {
        return isRecalculated;
    }

    public void setRecalculated(boolean recalculated) {
        isRecalculated = recalculated;
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
