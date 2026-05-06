package com.ho.account.expenditure.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [Payable] 도메인 엔티티.
 * 공급업체에 지급해야 할 개별 채무 항목을 관리합니다.
 * 타 모듈(Master Data, Journal Ledger)과는 ID/Code 기반으로 참조하여 독립성을 보장합니다.
 */
@Entity
@Table(name = "payables")
public class Payable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 관련 매입 인보이스 번호.
     */
    @Column(name = "purchase_invoice_invoice_no", nullable = false, length = 50)
    private String purchaseInvoiceNo;

    /**
     * 매입 인보이스의 공급업체 코드.
     */
    @Column(name = "purchase_invoice_vendor_code", nullable = false, length = 20)
    private String purchaseInvoiceVendorCode;

    /**
     * 채무 대상 공급업체 코드.
     */
    @Column(name = "vendor_code", nullable = false, length = 20)
    private String vendorCode;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal originalAmount; // 최초 채무 금액

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingAmount; // 미지급 금액 (잔액)

    @Column(nullable = false)
    private LocalDate dueDate; // 만기일 (지급 예정일)

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private PayableStatus status; // 채무 상태 (OPEN, PARTIAL_PAID, PAID, OVERDUE)

    /**
     * 연관된 회계 전표 ID.
     */
    @Column(name = "journal_entry_id")
    private Long journalEntryId;

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

    /**
     * 지급 금액을 적용하여 잔액을 차감하고 상태를 업데이트합니다.
     */
    public void applyPayment(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("지급 금액은 0보다 커야 합니다.");
        }
        if (amount.compareTo(this.outstandingAmount) > 0) {
            throw new IllegalArgumentException("지급 금액이 채무 잔액보다 클 수 없습니다.");
        }

        this.outstandingAmount = this.outstandingAmount.subtract(amount);
        updateStatusByBalance();
    }

    /**
     * 선급금 상계 금액을 적용합니다.
     */
    public void applyOffset(BigDecimal amount) {
        applyPayment(amount);
    }

    private void updateStatusByBalance() {
        if (this.outstandingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            this.status = PayableStatus.PAID;
            this.outstandingAmount = BigDecimal.ZERO;
        } else {
            this.status = PayableStatus.PARTIAL_PAID;
        }
    }

    public void markAsOverdue() {
        if (this.status != PayableStatus.PAID) {
            this.status = PayableStatus.OVERDUE;
        }
    }

    // Getter 및 Setter
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
