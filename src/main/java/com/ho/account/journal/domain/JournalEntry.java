package com.ho.account.journal.domain;

import com.ho.account.basic.domain.Currency;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
// 사용하지 않는 import 제거

/**
 * 분개 전표(Journal Entry) 엔티티
 * 회계 전표의 헤더 정보를 관리하며, 상태(DRAFT, APPROVED, POSTED 등) 관리와 승인 프로세스를 포함함.
 */
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
    private String slipNo; // 전표번호 (YYYYMMDD-SEQ)

    @Column(nullable = false)
    private LocalDate slipDate; // 전표일자 (작성일)

    @Column(nullable = false)
    private LocalDate accountingDate; // 회계일자 (실제 장부 반영일)

    @Column(length = 200)
    private String description; // 적요

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private JournalEntryStatus status; // DRAFT, REQUESTED, APPROVED, REJECTED

    @Column(length = 20)
    private String entryType; // NORMAL(일반), ADJUSTMENT(결산보정), TRANSFER(손익대체/이월)

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
    private String lineageSourceType; // 예: ERP_AP, BANKING_LOAN

    @Column(length = 100)
    private String lineageSourceId; // 원천 시스템 ID

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null)
            this.status = JournalEntryStatus.DRAFT;
        if (this.accountingDate == null)
            this.accountingDate = this.slipDate;
        if (this.entryType == null)
            this.entryType = "NORMAL";
        if (this.auditUser == null)
            this.auditUser = this.createdBy != null ? this.createdBy : "SYSTEM";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 연관관계 편의 메서드
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

    // Getter 및 Setter
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
}
