package com.ho.account.expenditure.domain;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 吏湲??뷀떚??
 * 怨듦툒?낆껜?먭쾶 吏湲됰맂 湲덉븸 ?뺣낫瑜?愿由ы빀?덈떎.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate paymentDate; // 吏湲됱씪

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_code", referencedColumnName = "businessPartnerCode", nullable = false)
    private BusinessPartner vendor; // 吏湲????怨듦툒?낆껜 (嫄곕옒泥?

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount; // 吏湲됱븸

    @Column(length = 100)
    private String bankAccount; // 吏湲됰맂 ???怨꾩쥖 (?대쫫 ?먮뒗 踰덊샇)

    @Column(length = 100)
    private String referenceNo; // ?대? 異붿쟻 ?먮뒗 ???李몄“ 踰덊샇

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status; // 吏湲??곹깭 (INITIATED, APPROVED, COMPLETED, FAILED, CANCELLED)

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry; // 吏湲?泥섎━ ?꾪몴????곌껐

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_run_id")
    private PaymentRun paymentRun; // 愿??吏湲??ㅽ뻾 (PaymentRun)

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = PaymentStatus.INITIATED; // 초기 상태는 INITIATED
        }
    }

    public boolean canExecute() {
        return status == PaymentStatus.INITIATED || status == PaymentStatus.APPROVED;
    }

    public void markAsCompleted(String bankAccount) {
        if (!canExecute()) {
            throw new IllegalStateException("현재 상태에서는 지급 완료 처리를 할 수 없습니다: " + status);
        }
        this.status = PaymentStatus.COMPLETED;
        this.bankAccount = bankAccount;
    }

    public void markAsFailed() {
        this.status = PaymentStatus.FAILED;
    }

    // Getter 및 Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public BusinessPartner getVendor() {
        return vendor;
    }

    public void setVendor(BusinessPartner vendor) {
        this.vendor = vendor;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public JournalEntry getJournalEntry() {
        return journalEntry;
    }

    public void setJournalEntry(JournalEntry journalEntry) {
        this.journalEntry = journalEntry;
    }

    public PaymentRun getPaymentRun() {
        return paymentRun;
    }

    public void setPaymentRun(PaymentRun paymentRun) {
        this.paymentRun = paymentRun;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
