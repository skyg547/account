package com.ho.account.receivable.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 매출채권 오픈 아이템 엔티티.
 * 고객으로부터 수금해야 할 개별 채권 항목을 나타냅니다.
 */
@Entity
@Table(name = "receivables")
public class Receivable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_invoice_id")
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
