package com.ho.account.receivable.infrastructure.persistence.entity;

import com.ho.account.receivable.domain.ReceivableStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Receivable JPA 영속성 엔티티.
 * DB `receivables` 테이블과 매핑되며, 도메인 POJO(Receivable)와 분리하여 관리합니다.
 */
@Entity
@Table(name = "receivables")
public class ReceivableJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_invoice_id", nullable = false, unique = true)
    private SalesInvoiceJpaEntity salesInvoice;

    @Column(name = "customer_code", nullable = false, length = 50)
    private String customerCode;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal originalAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingAmount;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private ReceivableStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ReceivableStatus.OPEN;
        }
        if (outstandingAmount == null) {
            outstandingAmount = originalAmount;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SalesInvoiceJpaEntity getSalesInvoice() { return salesInvoice; }
    public void setSalesInvoice(SalesInvoiceJpaEntity salesInvoice) { this.salesInvoice = salesInvoice; }

    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }

    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }

    public BigDecimal getOutstandingAmount() { return outstandingAmount; }
    public void setOutstandingAmount(BigDecimal outstandingAmount) { this.outstandingAmount = outstandingAmount; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public ReceivableStatus getStatus() { return status; }
    public void setStatus(ReceivableStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
