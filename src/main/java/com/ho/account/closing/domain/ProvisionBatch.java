package com.ho.account.closing.domain;

import com.ho.account.basic.domain.FiscalPeriod;
import com.ho.account.journal.domain.JournalEntry;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 충당/손상 배치 (Provision/Impairment Batch) 엔티티
 * 부채 및 자산의 충당금/손상차손 등을 계산하고 기록하는 배치 실행 이력을 관리합니다.
 */
@Entity
@Table(name = "provision_batches")
public class ProvisionBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_period_id", nullable = false)
    private FiscalPeriod fiscalPeriod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProvisionType provisionType; // BAD_DEBT, IMPAIRMENT, ECL (Expected Credit Loss)

    @Column(nullable = false)
    private LocalDateTime runDateTime; // 배치 실행 일시

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProvisionBatchStatus status; // RUNNING, COMPLETED, FAILED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_journal_entry_id")
    private JournalEntry generatedJournalEntry; // 생성된 조정 분개 전표

    @Column(length = 200)
    private String reportLink; // 생성된 리포트 링크 (예: PDF, 스프레드시트)

    @Column(length = 50)
    private String runBy; // 배치 실행자

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    public enum ProvisionType {
        BAD_DEBT, IMPAIRMENT, ECL, WARRANTY, RESTRUCTURING
    }

    public enum ProvisionBatchStatus {
        RUNNING, COMPLETED, FAILED, PENDING_APPROVAL
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null)
            this.status = ProvisionBatchStatus.RUNNING;
        if (this.auditUser == null)
            this.auditUser = "SYSTEM";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FiscalPeriod getFiscalPeriod() {
        return fiscalPeriod;
    }

    public void setFiscalPeriod(FiscalPeriod fiscalPeriod) {
        this.fiscalPeriod = fiscalPeriod;
    }

    public ProvisionType getProvisionType() {
        return provisionType;
    }

    public void setProvisionType(ProvisionType provisionType) {
        this.provisionType = provisionType;
    }

    public LocalDateTime getRunDateTime() {
        return runDateTime;
    }

    public void setRunDateTime(LocalDateTime runDateTime) {
        this.runDateTime = runDateTime;
    }

    public ProvisionBatchStatus getStatus() {
        return status;
    }

    public void setStatus(ProvisionBatchStatus status) {
        this.status = status;
    }

    public JournalEntry getGeneratedJournalEntry() {
        return generatedJournalEntry;
    }

    public void setGeneratedJournalEntry(JournalEntry generatedJournalEntry) {
        this.generatedJournalEntry = generatedJournalEntry;
    }

    public String getReportLink() {
        return reportLink;
    }

    public void setReportLink(String reportLink) {
        this.reportLink = reportLink;
    }

    public String getRunBy() {
        return runBy;
    }

    public void setRunBy(String runBy) {
        this.runBy = runBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAuditUser() {
        return auditUser;
    }

    public void setAuditUser(String auditUser) {
        this.auditUser = auditUser;
    }
}
