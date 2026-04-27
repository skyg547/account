package com.ho.account.income.domain;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Îß§Ï∂ú ?∏Î≥¥?¥Ïä§ ?îÌã∞??
 * Í≥†Í∞ù?êÍ≤å Î∞úÌñâ??Îß§Ï∂ú Î∞?Ï≤?µ¨ ?ïÎ≥¥Î•?Í¥ÄÎ¶¨Ìï©?àÎã§.
 */
@Entity
@Table(name = "sales_invoices")
public class SalesInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String invoiceNo; // ?∏Î≥¥?¥Ïä§ Î≤àÌò∏

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_code", referencedColumnName = "businessPartnerCode", nullable = false)
    private BusinessPartner customer; // Í≥†Í∞ù (Í±∞ÎûòÏ≤?

    @Column(nullable = false)
    private LocalDate issueDate; // Î∞úÌñâ??

    @Column(nullable = false)
    private LocalDate dueDate; // ÎßåÍ∏∞??(?òÍ∏à ?àÏ†ï??

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount; // Ï¥ùÏï° (Í≥µÍ∏âÍ∞Ä??+ ?∏Ïï°)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount; // ?∏Ïï°

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount; // Í≥µÍ∏âÍ∞Ä??

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private SalesInvoiceStatus status; // ?∏Î≥¥?¥Ïä§ ?ÅÌÉú (ISSUED, PAID, PARTIAL_PAID, OVERDUE, CANCELLED)


    @Column(length = 500)
    private String description; // ?§Î™Ö

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

    // Getter Î∞?Setter
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
