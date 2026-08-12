package com.ho.account.expenditure.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [DDD(도메인 주도 설계) - Aggregate Root] (Pure Java POJO)
 * 매입채무(Payable) 엔티티 — 거래처로부터 물건을 사고 아직 갚지 않은 빚을 관리합니다.
 * 
 * 🐣 [Hexagonal Architecture & Pure Java POJO 교육적 주석]
 * 1. 도메인 계층의 기술/영속성 프레임워크 독립성 (Hexagonal Architecture Core):
 *    도메인 모델 객체(Payable)는 데이터베이스나 JPA(Hibernate), Spring Data 등의 특정 기술 프레임워크 어노테이션에 의존하지 않는 Pure Java POJO입니다.
 *    이를 통해 핵심 비즈니스 로직(잔액 차감, 상계 처리, 상태 변경 규칙)을 특정 DB 기술이나 ORM 변경에 상관없이 독립적으로 유지합니다.
 * 
 * 2. Data Mapper 패턴의 아키텍처적 이점:
 *    도메인 모델의 캡슐화(Encapsulation) 및 객체지향적 비즈니스 메서드와, DB 스키마 표현에 최적화된 PayableJpaEntity 구조를 분리합니다.
 *    Infrastructure 계층의 PayableMapper가 Domain POJO <-> JPA Entity 간 상호 변환을 담당합니다.
 */
public class Payable {

    private Long id;
    private String purchaseInvoiceNo;
    private String purchaseInvoiceVendorCode;
    private String vendorCode;
    private BigDecimal originalAmount;
    private BigDecimal outstandingAmount;
    private LocalDate dueDate;
    private PayableStatus status = PayableStatus.OPEN;
    private Long journalEntryId;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Payable() {
    }

    /**
     * 지급 금액을 적용하여 잔액을 차감하고 상태를 업데이트합니다.
     */
    public void applyPayment(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("지급 금액은 0보다 커야 합니다.");
        }
        if (amount.compareTo(this.outstandingAmount) > 0) {
            throw new IllegalArgumentException("지급 금액이 채무 잔액보다 클 수 없습니다.");
        }

        this.outstandingAmount = this.outstandingAmount.subtract(amount);
        updateStatusByBalance();
    }

    /**
     * 선급금 상계 금액을 적용합니다.
     */
    public void applyOffset(BigDecimal amount) {
        applyPayment(amount);
    }

    private void updateStatusByBalance() {
        if (this.outstandingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            this.status = PayableStatus.PAID;
            this.outstandingAmount = BigDecimal.ZERO;
        } else {
            this.status = PayableStatus.PARTIAL_PAID;
        }
    }

    public void markAsOverdue() {
        if (this.status != PayableStatus.PAID) {
            this.status = PayableStatus.OVERDUE;
        }
    }

    // Getter 및 Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPurchaseInvoiceNo() { return purchaseInvoiceNo; }
    public void setPurchaseInvoiceNo(String purchaseInvoiceNo) { this.purchaseInvoiceNo = purchaseInvoiceNo; }

    public String getPurchaseInvoiceVendorCode() { return purchaseInvoiceVendorCode; }
    public void setPurchaseInvoiceVendorCode(String purchaseInvoiceVendorCode) { this.purchaseInvoiceVendorCode = purchaseInvoiceVendorCode; }

    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String vendorCode) { this.vendorCode = vendorCode; }

    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
        if (this.outstandingAmount == null) {
            this.outstandingAmount = originalAmount;
        }
    }

    public BigDecimal getOutstandingAmount() { return outstandingAmount; }
    public void setOutstandingAmount(BigDecimal outstandingAmount) { this.outstandingAmount = outstandingAmount; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public PayableStatus getStatus() { return status; }
    public void setStatus(PayableStatus status) { this.status = status; }

    public Long getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(Long journalEntryId) { this.journalEntryId = journalEntryId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
