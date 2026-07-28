package com.ho.account.loan.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 대출 이벤트(Loan Event) 엔티티.
 *
 * <p>대출 계약의 생명주기 동안 발생하는 중도상환, 조건 변경, 리스케줄 등 주요 이벤트를 기록합니다.
 * 전표는 별도 journal-ledger Aggregate가 소유하므로 이벤트에는 ID와 전표번호만 값으로 남깁니다.
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

    @Column(name = "journal_entry_id")
    private Long relatedJournalEntryId;

    @Column(name = "journal_entry_slip_no", length = 50)
    private String relatedJournalEntrySlipNo;

    // 재계산이 필요한 이벤트인 경우 RecalculationRun과 연결합니다.
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

    // Getter and Setter
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

    public Long getRelatedJournalEntryId() {
        return relatedJournalEntryId;
    }

    public void setRelatedJournalEntryId(Long relatedJournalEntryId) {
        this.relatedJournalEntryId = relatedJournalEntryId;
    }

    public String getRelatedJournalEntrySlipNo() {
        return relatedJournalEntrySlipNo;
    }

    public void setRelatedJournalEntrySlipNo(String relatedJournalEntrySlipNo) {
        this.relatedJournalEntrySlipNo = relatedJournalEntrySlipNo;
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

    public static LoanEvent record(
            Loan loan,
            EventType eventType,
            LocalDate eventDate,
            String description,
            RecalculationRun recalculationRun,
            String actor) {
        if (loan == null || loan.getId() == null) {
            throw new IllegalArgumentException("A persisted loan is required.");
        }
        LoanEvent event = new LoanEvent();
        event.loan = loan;
        event.eventType = java.util.Objects.requireNonNull(eventType, "eventType is required.");
        event.eventDate = java.util.Objects.requireNonNull(eventDate, "eventDate is required.");
        event.description = normalizeNullableText(description, 1000);
        event.recalculationRun = recalculationRun;
        if (recalculationRun != null) {
            event.relatedJournalEntryId = recalculationRun.getAdjustmentJournalEntryId();
            event.relatedJournalEntrySlipNo = recalculationRun.getAdjustmentJournalEntrySlipNo();
        }
        event.auditUser = requireText(actor, "actor", 50);
        return event;
    }

    private static String normalizeNullableText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException("description must not exceed " + maxLength + " characters.");
        }
        return normalized;
    }

    private static String requireText(String value, String field, int maxLength) {
        String normalized = normalizeNullableText(value, maxLength);
        if (normalized == null) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return normalized;
    }
}
