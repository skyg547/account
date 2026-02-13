package com.ho.account.common.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 송장-지급 매칭 (Invoice Matching / Clearing) 엔티티
 * AP/AR 공용으로 사용하거나 구분하여 사용 가능. 여기서는 통합 관리.
 */
@Entity
@Table(name = "invoice_matchings")
public class InvoiceMatching {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String matchType; // AP, AR

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal matchedAmount; // 매칭(반제) 금액

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(length = 50)
    private String auditUser;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.auditUser == null)
            this.auditUser = "SYSTEM";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMatchType() {
        return matchType;
    }

    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public BigDecimal getMatchedAmount() {
        return matchedAmount;
    }

    public void setMatchedAmount(BigDecimal matchedAmount) {
        this.matchedAmount = matchedAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getAuditUser() {
        return auditUser;
    }

    public void setAuditUser(String auditUser) {
        this.auditUser = auditUser;
    }
}
