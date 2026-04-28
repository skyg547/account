package com.ho.account.loan.domain;

import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ?異??대깽??(Loan Event) ?뷀떚??
 * ?異?怨꾩빟???앹븷 二쇨린 ?숈븞 諛쒖깮?섎뒗 二쇱슂 ?대깽??以묐룄?곹솚, 議곌굔 蹂寃???瑜?湲곕줉?⑸땲??
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
    private EventType eventType; // EARLY_REPAYMENT, CONDITION_CHANGE, RESCHEDULE ??

    @Column(nullable = false)
    private LocalDate eventDate; // ?대깽??諛쒖깮??

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry relatedJournalEntry; // ?대깽??愿??遺꾧컻 ?꾪몴

    // ?ш퀎?곗씠 ?꾩슂???대깽?몄쓽 寃쎌슦 RecalculationRun怨??곌껐
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

    // Getter 諛?Setter
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
