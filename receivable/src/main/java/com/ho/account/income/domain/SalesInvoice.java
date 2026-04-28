package com.ho.account.income.domain;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 留ㅼ텧 ?몃낫?댁뒪 ?뷀떚??
 * 怨좉컼?먭쾶 諛쒗뻾??留ㅼ텧 諛?泥?뎄 ?뺣낫瑜?愿由ы빀?덈떎.
 */
@Entity
@Table(name = "sales_invoices")
public class SalesInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String invoiceNo; // ?몃낫?댁뒪 踰덊샇

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_code", referencedColumnName = "businessPartnerCode", nullable = false)
    private BusinessPartner customer; // 怨좉컼 (嫄곕옒泥?

    @Column(nullable = false)
    private LocalDate issueDate; // 諛쒗뻾??

    @Column(nullable = false)
    private LocalDate dueDate; // 留뚭린??(?섍툑 ?덉젙??

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount; // 珥앹븸 (怨듦툒媛??+ ?몄븸)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount; // ?몄븸

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount; // 怨듦툒媛??

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private SalesInvoiceStatus status; // ?몃낫?댁뒪 ?곹깭 (ISSUED, PAID, PARTIAL_PAID, OVERDUE, CANCELLED)


    @Column(length = 500)
    private String description; // ?ㅻ챸

    @Column(nullable = false, updatable = false)
    private String createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = SalesInvoiceStatus.ISSUED;
        }
    }

    // Getter 諛?Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public BusinessPartner getCustomer() {
        return customer;
    }

    public void setCustomer(BusinessPartner customer) {
        this.customer = customer;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public SalesInvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(SalesInvoiceStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
