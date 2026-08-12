package com.ho.account.expenditure.infrastructure.persistence.entity;

import com.ho.account.expenditure.domain.PaymentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Payment JPA 영속성 엔티티.
 * DB `payments` 테이블과 매핑되며, 도메인 POJO(Payment)와 분리하여 관리합니다.
 */
@Entity
@Table(name = "payments")
public class PaymentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Column(name = "vendor_code", nullable = false, length = 20)
    private String vendorCode;

    @Column(name = "payable_id")
    private Long payableId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 100)
    private String bankAccount;

    @Column(length = 100)
    private String referenceNo;

    @Column(nullable = false)
    private int executionAttempts;

    @Column(length = 500)
    private String failureReason;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_run_id")
    private PaymentRunJpaEntity paymentRun;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = PaymentStatus.INITIATED;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String vendorCode) { this.vendorCode = vendorCode; }

    public Long getPayableId() { return payableId; }
    public void setPayableId(Long payableId) { this.payableId = payableId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }

    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }

    public int getExecutionAttempts() { return executionAttempts; }
    public void setExecutionAttempts(int executionAttempts) { this.executionAttempts = executionAttempts; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public Long getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(Long journalEntryId) { this.journalEntryId = journalEntryId; }

    public PaymentRunJpaEntity getPaymentRun() { return paymentRun; }
    public void setPaymentRun(PaymentRunJpaEntity paymentRun) { this.paymentRun = paymentRun; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
