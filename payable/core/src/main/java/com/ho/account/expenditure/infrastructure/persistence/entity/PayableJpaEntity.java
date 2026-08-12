package com.ho.account.expenditure.infrastructure.persistence.entity;

import com.ho.account.expenditure.domain.PayableStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Payable JPA 영속성 엔티티.
 * DB `payables` 테이블과 매핑되며, 도메인 POJO(Payable)와 분리하여 관리합니다.
 */
@Entity
@Table(name = "payables")
public class PayableJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_invoice_invoice_no", nullable = false, length = 50)
    private String purchaseInvoiceNo;

    @Column(name = "purchase_invoice_vendor_code", nullable = false, length = 20)
    private String purchaseInvoiceVendorCode;

    @Column(name = "vendor_code", nullable = false, length = 20)
    private String vendorCode;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal originalAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingAmount;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private PayableStatus status;

    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = PayableStatus.OPEN;
        }
        if (outstandingAmount == null) {
            outstandingAmount = originalAmount;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPurchaseInvoiceNo() { return purchaseInvoiceNo; }
    public void setPurchaseInvoiceNo(String purchaseInvoiceNo) { this.purchaseInvoiceNo = purchaseInvoiceNo; }

    public String getPurchaseInvoiceVendorCode() { return purchaseInvoiceVendorCode; }
    public void setPurchaseInvoiceVendorCode(String purchaseInvoiceVendorCode) { this.purchaseInvoiceVendorCode = purchaseInvoiceVendorCode; }

    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String vendorCode) { this.vendorCode = vendorCode; }

    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }

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
