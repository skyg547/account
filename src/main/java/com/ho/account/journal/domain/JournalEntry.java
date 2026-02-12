import com.ho.account.journal.domain.JournalEntryStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 전표(Journal Entry) 헤더 정보를 담는 엔티티입니다.
 * 회계 거래의 기본 단위로, 날짜, 번호, 적요, 상태 등을 관리합니다.
 */
@Entity
@Table(name = "journal_entries")
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

    @Column(length = 500)
    private String rejectionReason;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalDetail> details = new ArrayList<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = JournalEntryStatus.DRAFT;
        if (accountingDate == null) accountingDate = slipDate;
        if (entryType == null) entryType = "NORMAL"; // 기본값: 일반 전표
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

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSlipNo() { return slipNo; }
    public void setSlipNo(String slipNo) { this.slipNo = slipNo; }

    public LocalDate getSlipDate() { return slipDate; }
    public void setSlipDate(LocalDate slipDate) { this.slipDate = slipDate; }

    public LocalDate getAccountingDate() { return accountingDate; }
    public void setAccountingDate(LocalDate accountingDate) { this.accountingDate = accountingDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public JournalEntryStatus getStatus() { return status; }
    public void setStatus(JournalEntryStatus status) { this.status = status; }

    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public List<JournalDetail> getDetails() { return details; }
    public void setDetails(List<JournalDetail> details) { this.details = details; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
