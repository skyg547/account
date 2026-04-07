package com.ho.account.expenditure.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ap_payments") // Renamed table for clarity
public class APPayment { // Renamed class

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expenditure_resolution_id", nullable = false)
    private ExpenditureResolution expenditureResolution;

    @Column(name = "tax_invoice_id")
    private Long taxInvoiceId;

    @Column(nullable = false)
    private LocalDateTime paymentDate; // 실제 지급 일시

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount; // Gross payment amount

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unappliedAmount; // Amount not yet applied to invoices/resolutions

    @Column(length = 50)
    private String paymentMethod; // TRANSFER(이체), CASH(현금), CARD(카드)

    @Column(length = 20)
    private String status; // COMPLETED, FAILED, PENDING, PARTIALLY_APPLIED

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

    // Getter 및 Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExpenditureResolution getExpenditureResolution() {
        return expenditureResolution;
    }

    public void setExpenditureResolution(ExpenditureResolution expenditureResolution) {
        this.expenditureResolution = expenditureResolution;
    }

    public Long getTaxInvoiceId() {
        return taxInvoiceId;
    }

    public void setTaxInvoiceId(Long taxInvoiceId) {
        this.taxInvoiceId = taxInvoiceId;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getUnappliedAmount() {
        return unappliedAmount;
    }

    public void setUnappliedAmount(BigDecimal unappliedAmount) {
        this.unappliedAmount = unappliedAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getAuditUser() {
        return auditUser;
    }

    public void setAuditUser(String auditUser) {
        this.auditUser = auditUser;
    }
}
