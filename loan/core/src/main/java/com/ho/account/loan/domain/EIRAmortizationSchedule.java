package com.ho.account.loan.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * EIR 상각 스케줄(EIR Amortization Schedule) 엔티티.
 *
 * <p>유효이자율(EIR) 방식에 따라 계산한 대출의 기간별 이자수익,
 * 원금상환, 이연 항목 상각 금액을 기록합니다.
 *
 * <p>전표는 journal-ledger가 소유하는 별도 Aggregate입니다. Loan 도메인은 전표 엔티티를 직접 참조하지 않고,
 * 업무 추적에 필요한 전표 ID와 전표번호만 값으로 보관합니다.
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
    private LocalDate scheduleDate; // 해당 스케줄의 기준일(월말/일자 등)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal beginningBalance; // 기초 잔액

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal interestIncome; // 이자 수익(EIR 적용)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal principalRepayment; // 원금 상환액

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal endingBalance; // 기말 잔액

    @Column(precision = 19, scale = 2)
    private BigDecimal deferredItemAmortization; // 이연 항목 상각액(수수료/비용 등)

    @Column(precision = 19, scale = 2)
    private BigDecimal cashFlow; // 해당 기간 현금흐름

    @Column(name = "amortization_journal_entry_id")
    private Long amortizationJournalEntryId; // 상각 관련 전표 ID 값 참조

    @Column(name = "amortization_journal_entry_slip_no", length = 50)
    private String amortizationJournalEntrySlipNo; // 상각 관련 전표번호 값 참조

    @Column(nullable = false)
    private boolean isRecalculated = false; // 재계산된 스케줄 항목인지 여부

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

    // Getter and Setter
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

    public Long getAmortizationJournalEntryId() {
        return amortizationJournalEntryId;
    }

    public void setAmortizationJournalEntryId(Long amortizationJournalEntryId) {
        this.amortizationJournalEntryId = amortizationJournalEntryId;
    }

    public String getAmortizationJournalEntrySlipNo() {
        return amortizationJournalEntrySlipNo;
    }

    public void setAmortizationJournalEntrySlipNo(String amortizationJournalEntrySlipNo) {
        this.amortizationJournalEntrySlipNo = amortizationJournalEntrySlipNo;
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