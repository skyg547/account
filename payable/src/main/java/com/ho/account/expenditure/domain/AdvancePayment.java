package com.ho.account.expenditure.domain;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ? ê¸‰ê¸??”í‹°??
 * ê³µê¸‰?…ì²´??ë¯¸ë¦¬ ì§€ê¸‰ëœ ê¸ˆì•¡??ê´€ë¦¬í•˜ë©? ì¶”í›„ ë§¤ì…ì±„ë¬´?€ ?ê³„?????ˆìŠµ?ˆë‹¤.
 */
@Entity
@Table(name = "advance_payments")
public class AdvancePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_code", referencedColumnName = "businessPartnerCode", nullable = false)
    private BusinessPartner vendor; // ? ê¸‰ê¸?ì§€ê¸??€??ê³µê¸‰?…ì²´

    @Column(nullable = false)
    private LocalDate paymentDate; // ? ê¸‰ê¸?ì§€ê¸‰ì¼

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount; // ìµœì´ˆ ? ê¸‰ê¸?ê¸ˆì•¡

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingAmount; // ?ê³„ ê°€?¥í•œ ?”ì—¬ ê¸ˆì•¡

    @Column(length = 500)
    private String description; // ?¤ëª…

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private AdvancePaymentStatus status; // ? ê¸‰ê¸??íƒœ (ACTIVE, OFFSET, REFUNDED)

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry; // ? ê¸‰ê¸?ì§€ê¸??„í‘œ?€???°ê²°

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

    // Getter ë°?Setter
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
