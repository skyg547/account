package com.ho.account.expenditure.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [PurchaseInvoice] 도메인 엔티티.
 * 공급업체로부터 수취한 매입 인보이스 정보를 관리합니다.
 * 타 모듈(Master Data, Journal Ledger)과는 ID/Code 기반으로 참조하여 결합도를 낮춥니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 회사가 물건을 사고 거래처(공급업체)로부터 받은 '청구서(세금계산서)'를 의미합니다.
 * "언제(issueDate), 누구한테(vendorCode), 얼마(totalAmount)를 줘야 하는지"가 적혀 있습니다.
 * 이 청구서가 최종 승인(APPROVED)되면, 실제로 갚아야 할 외상값인 Payable(매입채무)이 자동 생성되며,
 * 동시에 회계 장부에 기록하기 위한 분개(JournalEntry)가 발생합니다.
 */
@Entity
@Table(
        name = "purchase_invoices",
        uniqueConstraints = @UniqueConstraint(name = "uk_purchase_invoice_business_key", columnNames = {"invoice_no", "vendor_code"})
)
public class PurchaseInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_no", nullable = false, length = 50)
    private String invoiceNo;

    /**
     * 공급업체(거래처) 코드.
     */
    @Column(name = "vendor_code", nullable = false, length = 20)
    private String vendorCode;

    @Column(nullable = false)
    private LocalDate issueDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private PurchaseInvoiceStatus status;

    /**
     * 연관된 회계 전표 ID.
     */
    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, updatable = false)
    private String createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = PurchaseInvoiceStatus.RECEIVED;
        }
        if (createdBy == null) {
            createdBy = "SYSTEM";
        }
    }

    // Getter 및 Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }

    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String vendorCode) { this.vendorCode = vendorCode; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }

    public PurchaseInvoiceStatus getStatus() { return status; }
    public void setStatus(PurchaseInvoiceStatus status) { this.status = status; }

    public Long getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(Long journalEntryId) { this.journalEntryId = journalEntryId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
