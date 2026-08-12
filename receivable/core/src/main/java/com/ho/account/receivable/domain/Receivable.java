package com.ho.account.receivable.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [DDD(도메인 주도 설계) - Aggregate Root] (Pure Java POJO)
 * 매출채권(Receivable) 엔티티 — 고객에게 청구한 금액 중 아직 회수되지 않은 잔액을 관리합니다.
 * 
 * 🐣 [Hexagonal Architecture & Pure Java POJO 교육적 주석]
 * 1. 도메인 계층 독립성 보장:
 *    매출채권 도메인 모델(Receivable)은 JPA 의존성을 제거하여 기술 프레임워크로부터 독립적입니다.
 *    수납 적용(applyCollection), 연체 판정(markAsOverdue) 등의 풍부한 비즈니스 로직을 pure Java 코드로 실행합니다.
 * 
 * 2. Data Mapper 패턴의 아키텍처 이점:
 *    ReceivableJpaEntity와 ReceivableMapper를 통해 ORM 프레임워크 매핑 구조를 도메인 객체와 분리합니다.
 */
public class Receivable {

    private Long id;
    private SalesInvoice salesInvoice;
    private String customerCode;
    private BigDecimal originalAmount;
    private BigDecimal outstandingAmount;
    private LocalDate dueDate;
    private ReceivableStatus status = ReceivableStatus.OPEN;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Receivable() {
    }

    /**
     * 수납 금액을 적용하여 잔액을 차감하고 상태를 업데이트합니다.
     */
    public void applyCollection(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("수납 금액은 0보다 커야 합니다.");
        }
        if (amount.compareTo(this.outstandingAmount) > 0) {
            throw new IllegalArgumentException("수납 금액이 채권 잔액보다 클 수 없습니다.");
        }

        this.outstandingAmount = this.outstandingAmount.subtract(amount);
        updateStatusByBalance();
    }

    private void updateStatusByBalance() {
        if (this.outstandingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            this.status = ReceivableStatus.PAID;
            this.outstandingAmount = BigDecimal.ZERO;
        } else {
            this.status = ReceivableStatus.PARTIAL_PAID;
        }
    }

    /**
     * 기일 경과 여부를 확인하여 상태를 OVERDUE로 변경합니다.
     */
    public void markAsOverdue() {
        if (this.status != ReceivableStatus.PAID) {
            this.status = ReceivableStatus.OVERDUE;
        }
    }

    // Getter 및 Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SalesInvoice getSalesInvoice() {
        return salesInvoice;
    }

    public void setSalesInvoice(SalesInvoice salesInvoice) {
        this.salesInvoice = salesInvoice;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public void setCustomerCode(String customerCode) {
        this.customerCode = customerCode;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
        if (this.outstandingAmount == null) {
            this.outstandingAmount = originalAmount;
        }
    }

    public BigDecimal getOutstandingAmount() {
        return outstandingAmount;
    }

    public void setOutstandingAmount(BigDecimal outstandingAmount) {
        this.outstandingAmount = outstandingAmount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public ReceivableStatus getStatus() {
        return status;
    }

    public void setStatus(ReceivableStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
