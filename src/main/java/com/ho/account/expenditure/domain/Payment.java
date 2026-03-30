package com.ho.account.expenditure.domain;

import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.journal.domain.JournalEntry;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 지급 엔티티.
 * 공급업체에게 지급된 금액 정보를 관리합니다.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate paymentDate; // 지급일

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_code", referencedColumnName = "businessPartnerCode", nullable = false)
    private BusinessPartner vendor; // 지급 대상 공급업체 (거래처)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount; // 지급액

    @Column(length = 100)
    private String bankAccount; // 지급된 은행 계좌 (이름 또는 번호)

    @Column(length = 100)
    private String referenceNo; // 내부 추적 또는 은행 참조 번호

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status; // 지급 상태 (INITIATED, APPROVED, COMPLETED, FAILED, CANCELLED)

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry; // 지급 처리 전표와의 연결

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_run_id")
    private PaymentRun paymentRun; // 관련 지급 실행 (PaymentRun)

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = PaymentStatus.INITIATED; // 초기 상태는 INITIATED
        }
    }

    // Getters and Setters
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
