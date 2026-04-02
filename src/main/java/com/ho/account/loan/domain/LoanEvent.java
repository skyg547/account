package com.ho.account.loan.domain;

import com.ho.account.journal.domain.JournalEntry;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 대출 이벤트 (Loan Event) 엔티티
 * 대출 계약의 생애 주기 동안 발생하는 주요 이벤트(중도상환, 조건 변경 등)를 기록합니다.
 */
@Entity
@Table(name = "loan_events")
public class LoanEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EventType eventType; // EARLY_REPAYMENT, CONDITION_CHANGE, RESCHEDULE 등

    @Column(nullable = false)
    private LocalDate eventDate; // 이벤트 발생일

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry relatedJournalEntry; // 이벤트 관련 분개 전표

    // 재계산이 필요한 이벤트의 경우 RecalculationRun과 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recalculation_run_id")
    private RecalculationRun recalculationRun;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    public enum EventType {
        EARLY_REPAYMENT, CONDITION_CHANGE, RESCHEDULE, DEFAULT, RECOVERY, OTHER
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.auditUser == null)
            this.auditUser = "SYSTEM";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getter 및 Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Loan getLoan() {
        return loan;
    }

    public void setLoan(Loan loan) {
        this.loan = loan;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JournalEntry getRelatedJournalEntry() {
        return relatedJournalEntry;
    }

    public void setRelatedJournalEntry(JournalEntry relatedJournalEntry) {
        this.relatedJournalEntry = relatedJournalEntry;
    }

    public RecalculationRun getRecalculationRun() {
        return recalculationRun;
    }

    public void setRecalculationRun(RecalculationRun recalculationRun) {
        this.recalculationRun = recalculationRun;
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
