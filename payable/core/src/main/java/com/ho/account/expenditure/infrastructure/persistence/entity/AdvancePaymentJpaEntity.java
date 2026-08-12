package com.ho.account.expenditure.infrastructure.persistence.entity;

import com.ho.account.expenditure.domain.AdvancePaymentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AdvancePayment JPA 영속성 엔티티.
 * DB `advance_payments` 테이블과 매핑되며, 도메인 POJO(AdvancePayment)와 분리하여 관리합니다.
 */
@Entity
@Table(name = "advance_payments")
public class AdvancePaymentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vendor_code", nullable = false, length = 20)
    private String vendorCode;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingAmount;

    @Column(length = 500)
    private String description;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private AdvancePaymentStatus status;

    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = AdvancePaymentStatus.ACTIVE;
        }
        if (outstandingAmount == null) {
            outstandingAmount = amount;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String vendorCode) { this.vendorCode = vendorCode; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getOutstandingAmount() { return outstandingAmount; }
    public void setOutstandingAmount(BigDecimal outstandingAmount) { this.outstandingAmount = outstandingAmount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public AdvancePaymentStatus getStatus() { return status; }
    public void setStatus(AdvancePaymentStatus status) { this.status = status; }

    public Long getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(Long journalEntryId) { this.journalEntryId = journalEntryId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
