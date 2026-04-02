package com.ho.account.expenditure.domain;

import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.journal.domain.JournalEntry;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 매입채무 오픈 아이템 엔티티.
 * 공급업체에 지급해야 할 개별 항목을 나타냅니다.
 */
@Entity
@Table(name = "payables")
public class Payable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_invoice_invoice_no", referencedColumnName = "invoiceNo", insertable = false, updatable = false)
    @JoinColumn(name = "purchase_invoice_vendor_code", referencedColumnName = "vendor_code", insertable = false, updatable = false)
    private PurchaseInvoice purchaseInvoice; // 관련 매입 인보이스

    // To handle composite key of PurchaseInvoice, we need to manually map the columns
    @Column(name = "purchase_invoice_invoice_no", nullable = false)
    private String purchaseInvoiceNo;

    @Column(name = "purchase_invoice_vendor_code", nullable = false)
    private String purchaseInvoiceVendorCode;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_code", referencedColumnName = "businessPartnerCode", nullable = false)
    private BusinessPartner vendor; // 채무 대상 공급업체 (거래처)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal originalAmount; // 최초 채무 금액

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingAmount; // 미지급 금액

    @Column(nullable = false)
    private LocalDate dueDate; // 만기일 (지급 예정일)

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private PayableStatus status; // 채무 상태 (OPEN, PARTIAL_PAID, PAID, OVERDUE)

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry; // 매입채무 인식 전표와의 연결 (선택적)

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = PayableStatus.OPEN;
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

    public PurchaseInvoice getPurchaseInvoice() {
        return purchaseInvoice;
    }

    public void setPurchaseInvoice(PurchaseInvoice purchaseInvoice) {
        this.purchaseInvoice = purchaseInvoice;
        if (purchaseInvoice != null) {
            this.purchaseInvoiceNo = purchaseInvoice.getInvoiceNo();
            this.purchaseInvoiceVendorCode = purchaseInvoice.getVendor().getBusinessPartnerCode();
        }
    }

    public String getPurchaseInvoiceNo() {
        return purchaseInvoiceNo;
    }

    public void setPurchaseInvoiceNo(String purchaseInvoiceNo) {
        this.purchaseInvoiceNo = purchaseInvoiceNo;
    }

    public String getPurchaseInvoiceVendorCode() {
        return purchaseInvoiceVendorCode;
    }

    public void setPurchaseInvoiceVendorCode(String purchaseInvoiceVendorCode) {
        this.purchaseInvoiceVendorCode = purchaseInvoiceVendorCode;
    }

    public BusinessPartner getVendor() {
        return vendor;
    }

    public void setVendor(BusinessPartner vendor) {
        this.vendor = vendor;
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

    public PayableStatus getStatus() {
        return status;
    }

    public void setStatus(PayableStatus status) {
        this.status = status;
    }

    public JournalEntry getJournalEntry() {
        return journalEntry;
    }

    public void setJournalEntry(JournalEntry journalEntry) {
        this.journalEntry = journalEntry;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
