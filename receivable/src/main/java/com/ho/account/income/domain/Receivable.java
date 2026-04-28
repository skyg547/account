package com.ho.account.income.domain;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 留ㅼ텧梨꾧텒 ?ㅽ뵂 ?꾩씠???뷀떚??
 * 怨좉컼?쇰줈遺???섍툑?댁빞 ??媛쒕퀎 ??ぉ???섑??낅땲??
 */
@Entity
@Table(name = "receivables")
public class Receivable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_invoice_id")
    private SalesInvoice salesInvoice; // 愿??留ㅼ텧 ?몃낫?댁뒪

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_code", referencedColumnName = "businessPartnerCode", nullable = false)
    private BusinessPartner customer; // 梨꾧텒 ???怨좉컼 (嫄곕옒泥?

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal originalAmount; // 理쒖큹 梨꾧텒 湲덉븸

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingAmount; // 誘몄닔 湲덉븸

    @Column(nullable = false)
    private LocalDate dueDate; // 留뚭린??(?섍툑 ?덉젙??

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private ReceivableStatus status; // 梨꾧텒 ?곹깭 (OPEN, PARTIAL_PAID, PAID, OVERDUE)


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

    // Getter 諛?Setter
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
