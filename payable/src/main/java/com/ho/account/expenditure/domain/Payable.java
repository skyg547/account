package com.ho.account.expenditure.domain;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 留ㅼ엯梨꾨Т ?ㅽ뵂 ?꾩씠???뷀떚??
 * 怨듦툒?낆껜??吏湲됲빐????媛쒕퀎 ??ぉ???섑??낅땲??
 */
@Entity
@Table(name = "payables")
public class Payable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Transient
    private PurchaseInvoice purchaseInvoice; // 愿??留ㅼ엯 ?몃낫?댁뒪

    // To handle composite key of PurchaseInvoice, we need to manually map the columns
    @Column(name = "purchase_invoice_invoice_no", nullable = false)
    private String purchaseInvoiceNo;

    @Column(name = "purchase_invoice_vendor_code", nullable = false)
    private String purchaseInvoiceVendorCode;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_code", referencedColumnName = "businessPartnerCode", nullable = false)
    private BusinessPartner vendor; // 梨꾨Т ???怨듦툒?낆껜 (嫄곕옒泥?

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal originalAmount; // 理쒖큹 梨꾨Т 湲덉븸

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingAmount; // 誘몄?湲?湲덉븸

    @Column(nullable = false)
    private LocalDate dueDate; // 留뚭린??(吏湲??덉젙??

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private PayableStatus status; // 梨꾨Т ?곹깭 (OPEN, PARTIAL_PAID, PAID, OVERDUE)

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry; // 留ㅼ엯梨꾨Т ?몄떇 ?꾪몴????곌껐 (?좏깮??

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
        applyPayment(amount); // 상계도 결국 잔액을 차감하는 행위이므로 동일 로직 사용
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
