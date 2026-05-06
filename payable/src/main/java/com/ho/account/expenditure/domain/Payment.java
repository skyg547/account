package com.ho.account.expenditure.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [Payment] 도메인 엔티티.
 * 공급업체에게 실제로 지급된 내역을 관리합니다.
 * 타 모듈과는 ID/Code 기반으로 참조하여 결합도를 낮춥니다.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate paymentDate; // 지급일

    /**
     * 지급 대상 공급업체 코드.
     */
    @Column(name = "vendor_code", nullable = false, length = 20)
    private String vendorCode;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount; // 지급액

    @Column(length = 100)
    private String bankAccount; // 지급된 대상 계좌

    @Column(length = 100)
    private String referenceNo; // 외부 추적 번호

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status; // 지급 상태

    /**
     * 연관된 회계 전표 ID.
     */
    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_run_id")
    private PaymentRun paymentRun; // 연관된 지급 실행 그룹

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = PaymentStatus.INITIATED;
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
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String vendorCode) { this.vendorCode = vendorCode; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }

    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public Long getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(Long journalEntryId) { this.journalEntryId = journalEntryId; }

    public PaymentRun getPaymentRun() { return paymentRun; }
    public void setPaymentRun(PaymentRun paymentRun) { this.paymentRun = paymentRun; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
