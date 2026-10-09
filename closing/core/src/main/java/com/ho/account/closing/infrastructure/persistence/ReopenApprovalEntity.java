package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.domain.ReopenApproval;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Database representation of ReopenApproval; business transitions live in the domain aggregate. */
@Entity(name = "ReopenApproval")
@Table(name = "reopen_approvals")
@Getter
@Setter
public class ReopenApprovalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fiscal_period_id", nullable = false)
    private Long fiscalPeriodId;

    @Transient
    private String fiscalYear;

    @Transient
    private String fiscalPeriod;

    @Column(length = 50)
    private String requestedBy;

    private LocalDateTime requestedAt;

    @Column(length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReopenApproval.ReopenApprovalStatus status; // PENDING, APPROVED, REJECTED

    @Column(length = 50)
    private String approvedBy;

    private LocalDateTime approvedAt;

    @Column(columnDefinition = "TEXT")
    private String impactAnalysisReport;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null)
            this.status = ReopenApproval.ReopenApprovalStatus.PENDING;
        if (this.auditUser == null)
            this.auditUser = "SYSTEM";
    }
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
