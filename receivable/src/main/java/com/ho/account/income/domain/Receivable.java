package com.ho.account.income.domain;

import com.ho.account.basic.domain.BusinessPartner;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 매출채권 오픈 아이템 엔티티.
 * 고객으로부터 수금해야 할 개별 항목을 나타냅니다.
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_code", referencedColumnName = "businessPartnerCode", nullable = false)
    private BusinessPartner customer; // 채권 대상 고객 (거래처)

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

    public BusinessPartner getCustomer() {
        return customer;
    }

    public void setCustomer(BusinessPartner customer) {
        this.customer = customer;
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
