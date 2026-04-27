package com.ho.account.journalledger.domain.journal.domain;

import com.ho.account.masterdata.core.domain.model.Currency;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "journal_entries", indexes = {
        @Index(name = "idx_journal_entry_slip_no", columnList = "slipNo"),
        @Index(name = "idx_journal_entry_accounting_date", columnList = "accountingDate"),
        @Index(name = "idx_journal_entry_status", columnList = "status"),
        @Index(name = "idx_journal_entry_lineage", columnList = "lineageSourceType, lineageSourceId")
})
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String slipNo;

    @Column(nullable = false)
    private LocalDate slipDate;

    @Column(nullable = false)
    private LocalDate accountingDate;

    @Column(length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private JournalEntryStatus status;

    /**
     * 전표 승인 프로세스
     */
    public void approve(String approver) {
        if (this.status != JournalEntryStatus.DRAFT && this.status != JournalEntryStatus.REQUESTED) {
            throw new IllegalStateException("승인 가능한 상태가 아닙니다. 현재 상태: " + this.status);
        }
        validateBalance();
        this.status = JournalEntryStatus.APPROVED;
        this.auditUser = approver;
    }

    /**
     * 전표 반려 프로세스
     */
    public void reject(String approver, String reason) {
        this.status = JournalEntryStatus.REJECTED;
        this.rejectionReason = reason;
        this.auditUser = approver;
    }

    /**
     * 전표 전기 (원장 반영 가능 상태로 전환)
     */
    public void post(String poster) {
        if (this.status != JournalEntryStatus.APPROVED) {
            throw new IllegalStateException("승인된 전표만 전기할 수 있습니다.");
        }
        this.status = JournalEntryStatus.POSTED;
        this.auditUser = poster;
    }

    /**
     * 차대변 합계 검증 (정합성 체크)
     */
    public void validateBalance() {
        if (details.isEmpty()) {
            throw new IllegalStateException("전표 상세 내역이 없습니다.");
        }
        
        BigDecimal debitSum = details.stream()
                .filter(d -> JournalSide.DEBIT.equals(d.getSide()))
                .map(JournalDetail::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal creditSum = details.stream()
                .filter(d -> JournalSide.CREDIT.equals(d.getSide()))
                .map(JournalDetail::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (debitSum.compareTo(creditSum) != 0) {
            throw new IllegalStateException(String.format("차대변 합계가 일치하지 않습니다. (차변: %s, 대변: %s)", debitSum, creditSum));
        }
    }

    @Column(length = 50)
    private String entryType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code")
    private Currency currency;

    @Column(precision = 19, scale = 8)
    private BigDecimal exchangeRate;

    @Column(length = 500)
    private String rejectionReason;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalDetail> details = new ArrayList<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String createdBy;

    @Column(length = 50)
    private String auditUser;

    @Column(length = 50)
    private String lineageSourceType;

    @Column(length = 100)
    private String lineageSourceId;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = JournalEntryStatus.DRAFT;
        }
        if (this.accountingDate == null) {
            this.accountingDate = this.slipDate;
        }
        if (this.entryType == null) {
            this.entryType = "NORMAL";
        }
        if (this.auditUser == null) {
            this.auditUser = this.createdBy != null ? this.createdBy : "SYSTEM";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addDetail(JournalDetail detail) {
        details.add(detail);
        detail.setJournalEntry(this);
    }

    public void removeDetail(JournalDetail detail) {
        details.remove(detail);
        detail.setJournalEntry(null);
    }

    public void clearDetails() {
        this.details.clear();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSlipNo() {
        return slipNo;
    }

    public void setSlipNo(String slipNo) {
        this.slipNo = slipNo;
    }

    public LocalDate getSlipDate() {
        return slipDate;
    }

    public void setSlipDate(LocalDate slipDate) {
        this.slipDate = slipDate;
    }

    public LocalDate getAccountingDate() {
        return accountingDate;
    }

    public void setAccountingDate(LocalDate accountingDate) {
        this.accountingDate = accountingDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JournalEntryStatus getStatus() {
        return status;
    }

    public void setStatus(JournalEntryStatus status) {
        this.status = status;
    }

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public List<JournalDetail> getDetails() {
        return details;
    }

    public void setDetails(List<JournalDetail> details) {
        this.details = details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getAuditUser() {
        return auditUser;
    }

    public void setAuditUser(String auditUser) {
        this.auditUser = auditUser;
    }

    public String getLineageSourceType() {
        return lineageSourceType;
    }

    public void setLineageSourceType(String lineageSourceType) {
        this.lineageSourceType = lineageSourceType;
    }

    public String getLineageSourceId() {
        return lineageSourceId;
    }

    public void setLineageSourceId(String lineageSourceId) {
        this.lineageSourceId = lineageSourceId;
    }
}
