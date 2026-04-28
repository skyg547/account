package com.ho.account.loan.domain;

import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * EIR ?곴컖 ?ㅼ?以?(EIR Amortization Schedule) ?뷀떚??
 * ?좏슚?댁옄??EIR) 諛⑸쾿???곕씪 怨꾩궛???異쒖쓽 湲곌컙蹂??곴컖 ?ㅼ?以꾩쓣 湲곕줉?⑸땲??
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
    private LocalDate scheduleDate; // ?대떦 ?ㅼ?以꾩쓽 湲곗???(?쇰퀎/?붾퀎 ??

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal beginningBalance; // 湲곗큹 ?붿븸

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal interestIncome; // ?댁옄 ?섏씡 (EIR ?곸슜)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal principalRepayment; // ?먭툑 ?곹솚??

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal endingBalance; // 湲곕쭚 ?붿븸

    @Column(precision = 19, scale = 2)
    private BigDecimal deferredItemAmortization; // ?댁뿰 ??ぉ ?곴컖??(?섏닔猷? 鍮꾩슜 ??

    @Column(precision = 19, scale = 2)
    private BigDecimal cashFlow; // ?대떦 湲곌컙 ?꾧툑 ?먮쫫

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "amortization_journal_entry_id")
    private JournalEntry amortizationJournalEntry; // ?곴컖 愿??遺꾧컻 ?꾪몴

    @Column(nullable = false)
    private boolean isRecalculated = false; // ?ш퀎?곕맂 ?ㅼ?以???ぉ?몄? ?щ?

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

    // Getter 諛?Setter
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
