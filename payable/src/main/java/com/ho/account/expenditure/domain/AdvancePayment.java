package com.ho.account.expenditure.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [AdvancePayment] 도메인 엔티티.
 * 공급업체에게 미리 지급된 선급금 내역을 관리합니다.
 * 타 모듈과는 ID/Code 기반으로 참조하여 결합도를 낮춥니다.
 */
@Entity
@Table(name = "advance_payments")
public class AdvancePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 선급금 지급 대상 공급업체 코드.
     */
    @Column(name = "vendor_code", nullable = false, length = 20)
    private String vendorCode;

    @Column(nullable = false)
    private LocalDate paymentDate; // 선급금 지급일

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount; // 최초 선급금 금액

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingAmount; // 상계 가능한 잔액

    @Column(length = 500)
    private String description; // 설명

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private AdvancePaymentStatus status; // 선급금 상태 (ACTIVE, OFFSET, REFUNDED)

    /**
     * 연관된 회계 전표 ID.
     */
    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = AdvancePaymentStatus.ACTIVE;
        }
        if (outstandingAmount == null) {
            outstandingAmount = amount;
        }
    }

    /**
     * 채무와 상계 처리하여 선급금 잔액을 차감합니다.
     */
    public void applyOffset(BigDecimal offsetAmount) {
        if (offsetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("상계 금액은 0보다 커야 합니다.");
        }
        if (offsetAmount.compareTo(this.outstandingAmount) > 0) {
            throw new IllegalArgumentException("상계 금액이 선급금 잔액보다 클 수 없습니다.");
        }

        this.outstandingAmount = this.outstandingAmount.subtract(offsetAmount);
        
        if (this.outstandingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            this.status = AdvancePaymentStatus.OFFSET;
            this.outstandingAmount = BigDecimal.ZERO;
        } else {
            this.status = AdvancePaymentStatus.ACTIVE;
        }
    }

    // Getter 및 Setter
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
