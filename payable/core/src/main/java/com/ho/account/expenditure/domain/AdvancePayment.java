package com.ho.account.expenditure.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [AdvancePayment] 도메인 엔티티 (Pure Java POJO).
 * 공급업체에게 미리 지급된 선급금 내역을 관리합니다.
 * 타 모듈과는 ID/Code 기반으로 참조하여 결합도를 낮춥니다.
 *
 * 🐣 [Hexagonal Architecture & Pure Java POJO 교육적 주석]
 * 도메인 계층(AdvancePayment)은 기술 프레임워크(JPA, ORM 어노테이션)에 종속되지 않는 Pure Java POJO입니다.
 * 선급금 잔액 차감 및 상계 비즈니스 메서드(applyOffset)를 프레임워크 독립적으로 유지합니다.
 */
public class AdvancePayment {

    private Long id;
    private String vendorCode;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private BigDecimal outstandingAmount;
    private String description;
    private AdvancePaymentStatus status = AdvancePaymentStatus.ACTIVE;
    private Long journalEntryId;
    private LocalDateTime createdAt = LocalDateTime.now();

    public AdvancePayment() {
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
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
        if (this.outstandingAmount == null) {
            this.outstandingAmount = amount;
        }
    }

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
