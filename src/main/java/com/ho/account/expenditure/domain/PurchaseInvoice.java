package com.ho.account.expenditure.domain;

import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.journal.domain.JournalEntry;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 매입 인보이스 엔티티.
 * 공급업체로부터 수취한 인보이스 정보를 관리합니다.
 */
@Entity
@Table(name = "purchase_invoices")
@IdClass(PurchaseInvoiceId.class) // 복합 키 사용
public class PurchaseInvoice {

    @Id
    @Column(nullable = false, length = 50)
    private String invoiceNo; // 인보이스 번호

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_code", referencedColumnName = "businessPartnerCode", nullable = false)
    private BusinessPartner vendor; // 공급업체 (거래처)

    @Column(nullable = false)
    private LocalDate issueDate; // 발행일

    @Column(nullable = false)
    private LocalDate dueDate; // 만기일 (지급 예정일)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount; // 총액 (공급가액 + 세액)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount; // 세액

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount; // 공급가액

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private PurchaseInvoiceStatus status; // 인보이스 상태 (RECEIVED, APPROVED, PAID, PARTIAL_PAID, OVERDUE, CANCELLED)

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry; // 매입 인식 전표와의 연결

    @Column(length = 500)
    private String description; // 설명

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
    }

    // Getters and Setters
    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public BusinessPartner getVendor() {
        return vendor;
    }

    public void setVendor(BusinessPartner vendor) {
        this.vendor = vendor;
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

    public PurchaseInvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(PurchaseInvoiceStatus status) {
        this.status = status;
    }

    public JournalEntry getJournalEntry() {
        return journalEntry;
    }

    public void setJournalEntry(JournalEntry journalEntry) {
        this.journalEntry = journalEntry;
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
