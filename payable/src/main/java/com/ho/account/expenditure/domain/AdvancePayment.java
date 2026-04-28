package com.ho.account.expenditure.domain;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ?좉툒湲??뷀떚??
 * 怨듦툒?낆껜??誘몃━ 吏湲됰맂 湲덉븸??愿由ы븯硫? 異뷀썑 留ㅼ엯梨꾨Т? ?곴퀎?????덉뒿?덈떎.
 */
@Entity
@Table(name = "advance_payments")
public class AdvancePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_code", referencedColumnName = "businessPartnerCode", nullable = false)
    private BusinessPartner vendor; // ?좉툒湲?吏湲????怨듦툒?낆껜

    @Column(nullable = false)
    private LocalDate paymentDate; // ?좉툒湲?吏湲됱씪

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount; // 理쒖큹 ?좉툒湲?湲덉븸

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingAmount; // ?곴퀎 媛?ν븳 ?붿뿬 湲덉븸

    @Column(length = 500)
    private String description; // ?ㅻ챸

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private AdvancePaymentStatus status; // ?좉툒湲??곹깭 (ACTIVE, OFFSET, REFUNDED)

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry; // ?좉툒湲?吏湲??꾪몴????곌껐

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = AdvancePaymentStatus.ACTIVE;
        }
        if (outstandingAmount == null) {
            outstandingAmount = amount;
        }
    }

    // Getter 諛?Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BusinessPartner getVendor() {
        return vendor;
    }

    public void setVendor(BusinessPartner vendor) {
        this.vendor = vendor;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getOutstandingAmount() {
        return outstandingAmount;
    }

    public void setOutstandingAmount(BigDecimal outstandingAmount) {
        this.outstandingAmount = outstandingAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AdvancePaymentStatus getStatus() {
        return status;
    }

    public void setStatus(AdvancePaymentStatus status) {
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
