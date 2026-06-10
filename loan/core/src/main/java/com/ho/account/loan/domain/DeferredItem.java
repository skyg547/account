package com.ho.account.loan.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ?댁뿰 ??ぉ (Deferred Item) ?뷀떚??
 * ?異쒓낵 愿?⑤맂 媛쒕퀎 ?댁뿰 ?섏닔猷? 鍮꾩슜 ?깆쓣 愿由ы븯硫??곴컖 ?ㅼ?以꾩쓽 湲곗큹媛 ?⑸땲??
 */
@Entity
@Table(name = "deferred_items")
public class DeferredItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deferred_item_type_id", nullable = false)
    private DeferredItemType deferredItemType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount; // 珥??댁뿰 湲덉븸

    @Column(nullable = false)
    private LocalDate deferralDate; // ?댁뿰 諛쒖깮??

    @Column(nullable = false)
    private LocalDate amortizationStartDate; // ?곴컖 ?쒖옉??

    @Column(nullable = false)
    private LocalDate amortizationEndDate; // ?곴컖 醫낅즺??

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal remainingAmount; // ?붿뿬 ?댁뿰 湲덉븸

    @Column(name = "initial_journal_entry_id")
    private Long initialJournalEntryId;

    @Column(name = "initial_journal_entry_slip_no", length = 30)
    private String initialJournalEntrySlipNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeferredItemStatus status; // DEFERRED, AMORTIZING, FULLY_AMORTIZED

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    public enum DeferredItemStatus {
        DEFERRED, AMORTIZING, FULLY_AMORTIZED, CANCELLED
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null)
            this.status = DeferredItemStatus.DEFERRED;
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

    public DeferredItemType getDeferredItemType() {
        return deferredItemType;
    }

    public void setDeferredItemType(DeferredItemType deferredItemType) {
        this.deferredItemType = deferredItemType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getDeferralDate() {
        return deferralDate;
    }

    public void setDeferralDate(LocalDate deferralDate) {
        this.deferralDate = deferralDate;
    }

    public LocalDate getAmortizationStartDate() {
        return amortizationStartDate;
    }

    public void setAmortizationStartDate(LocalDate amortizationStartDate) {
        this.amortizationStartDate = amortizationStartDate;
    }

    public LocalDate getAmortizationEndDate() {
        return amortizationEndDate;
    }

    public void setAmortizationEndDate(LocalDate amortizationEndDate) {
        this.amortizationEndDate = amortizationEndDate;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public Long getInitialJournalEntryId() {
        return initialJournalEntryId;
    }

    public void setInitialJournalEntryId(Long initialJournalEntryId) {
        this.initialJournalEntryId = initialJournalEntryId;
    }

    public String getInitialJournalEntrySlipNo() {
        return initialJournalEntrySlipNo;
    }

    public void setInitialJournalEntrySlipNo(String initialJournalEntrySlipNo) {
        this.initialJournalEntrySlipNo = initialJournalEntrySlipNo;
    }

    public DeferredItemStatus getStatus() {
        return status;
    }

    public void setStatus(DeferredItemStatus status) {
        this.status = status;
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
