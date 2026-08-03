package com.ho.account.receivable.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [DDD(도메인 주도 설계) - Aggregate Root]
 * 매출채권(Receivable) 엔티티 — 고객에게 청구한 금액 중 아직 회수되지 않은 잔액을 관리합니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 매출채권은 쉽게 말해 '받을 권리가 있는 외상값'입니다. 
 * 고객에게 물건을 주고 청구서를 보냈을 때 "나중에 이만큼 돈을 받을 거야"라고 장부에 기록해두는 자산입니다. 
 * 이 클래스는 처음에 얼마를 받아야 했는지(원금)와, 고객이 중간에 조금씩 갚아서 현재 얼마가 남았는지(잔액)를 정확히 추적합니다.
 * Rich Domain Model 원칙에 따라, 수납 처리나 연체 판정 등의 비즈니스 규칙을 도메인 모델 내부에서 직접 관리합니다.
 */
@Entity
@Table(name = "receivables")
public class Receivable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_invoice_id", nullable = false, unique = true)
    private SalesInvoice salesInvoice; // 관련 매출 인보이스

    @Column(name = "customer_code", nullable = false, length = 50)
    private String customerCode; // 채권 대상 고객 코드 (거래처)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal originalAmount; // 최초 채권 금액

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingAmount; // 미수 금액

    @Column(nullable = false)
    private LocalDate dueDate; // 만기일 (수금 예정일)

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private ReceivableStatus status; // 채권 상태 (OPEN, PARTIAL_PAID, PAID, OVERDUE)


    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ReceivableStatus.OPEN;
        }
        if (outstandingAmount == null) {
            outstandingAmount = originalAmount;
        }
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
