package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.domain.ClosingCalendar;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Database representation of ClosingCalendar; business transitions live in the domain aggregate. */
@Entity(name = "ClosingCalendar")
@Table(name = "closing_calendars", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "fiscal_year", "fiscal_period" })
})
@Getter
@Setter
public class ClosingCalendarEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fiscal_year", nullable = false, length = 4)
    private String fiscalYear;

    @Column(name = "fiscal_period", nullable = false, length = 20) // MM, Q1, H1, YEAR 등 기간 코드
    private String fiscalPeriod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ClosingCalendar.ClosingCalendarStatus status; // OPEN, IN_PROGRESS, CLOSED, PERMANENTLY_CLOSED

    @Column(length = 50)
    private String closeInitiatedBy;

    private LocalDateTime closeInitiatedAt;

    @Column(length = 50)
    private String closedBy;

    private LocalDateTime closedAt;

    @Column(length = 50)
    private String reopenedBy;

    private LocalDateTime reopenedAt;

    @Column(nullable = false)
    private boolean isCurrentPeriod; // 현재 활성 결산 기간 여부

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    // Durable intent survives a failed/ambiguous remote Master call. Calendar admission stays closed
    // until the original target is confirmed; a dispatched operation is never automatically replayed.
    @Column(length = 36)
    private String transitionId;

    @Column(length = 6)
    private String transitionTarget;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ClosingCalendar.TransitionStage transitionStage;

    private Long transitionFiscalPeriodId;
    private Long transitionApprovalId;

    @Column(length = 50)
    private String transitionActor;

    private LocalDateTime transitionPreparedAt;

    @Column(length = 100)
    private String transitionEvidenceSetId;

    @Transient
    private String name;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null)
            this.status = ClosingCalendar.ClosingCalendarStatus.OPEN;
        if (this.auditUser == null)
            this.auditUser = "SYSTEM";
    }
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
