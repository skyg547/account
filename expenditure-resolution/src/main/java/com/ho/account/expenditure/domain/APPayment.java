package com.ho.account.expenditure.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ap_payments")
public class APPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expenditure_resolution_id", nullable = false)
    private ExpenditureResolution expenditureResolution;

    @Column(name = "tax_invoice_id")
    private Long taxInvoiceId;

    @Column(nullable = false)
    private LocalDateTime paymentDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unappliedAmount;

    @Column(length = 50)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private APPaymentStatus status;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.auditUser == null) this.auditUser = "SYSTEM";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 도메인 상태 전이 메서드
    public void complete() {
        if (this.status != APPaymentStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태의 지급만 완료 처리할 수 있습니다.");
        }
        this.status = APPaymentStatus.COMPLETED;
        this.unappliedAmount = BigDecimal.ZERO;
    }

    public void fail() {
        if (this.status != APPaymentStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태의 지급만 실패 처리할 수 있습니다.");
        }
        this.status = APPaymentStatus.FAILED;
    }

    public void partiallyApply(BigDecimal appliedAmount) {
        if (appliedAmount == null || appliedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("적용 금액은 0보다 커야 합니다.");
        }
        if (appliedAmount.compareTo(this.unappliedAmount) > 0) {
            throw new IllegalStateException("적용 금액이 미적용 잔액을 초과합니다.");
        }
        this.unappliedAmount = this.unappliedAmount.subtract(appliedAmount);
        this.status = this.unappliedAmount.compareTo(BigDecimal.ZERO) == 0
                ? APPaymentStatus.COMPLETED
                : APPaymentStatus.PARTIALLY_APPLIED;
    }

    // Getter / Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ExpenditureResolution getExpenditureResolution() { return expenditureResolution; }
    public void setExpenditureResolution(ExpenditureResolution expenditureResolution) {
        this.expenditureResolution = expenditureResolution;
    }

    public Long getTaxInvoiceId() { return taxInvoiceId; }
    public void setTaxInvoiceId(Long taxInvoiceId) { this.taxInvoiceId = taxInvoiceId; }

    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getUnappliedAmount() { return unappliedAmount; }
    public void setUnappliedAmount(BigDecimal unappliedAmount) { this.unappliedAmount = unappliedAmount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public APPaymentStatus getStatus() { return status; }
    public void setStatus(APPaymentStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public String getAuditUser() { return auditUser; }
    public void setAuditUser(String auditUser) { this.auditUser = auditUser; }
}
