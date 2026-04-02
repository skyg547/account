package com.ho.account.closing.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a financial closing period, tracking its status and associated approval workflows.
 */
@Entity
@Table(name = "closing_period")
public class ClosingPeriod {

    @Id
    @Column(length = 6)
    private Long id; // YYYYMM or YYYY (for annual)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PeriodType periodType;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClosingStatusEnum status = ClosingStatusEnum.OPEN;

    private LocalDateTime closedAt;
    private String closedBy;

    private LocalDateTime reopenedAt;
    private String reopenedBy;
    private String reopenReason;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus approvalStatus; // For re-opening
    private String approver;
    private LocalDateTime approvalDate;

    public ClosingPeriod() {}

    // Constructor for initial period creation
    public ClosingPeriod(Long id, PeriodType periodType, LocalDate startDate, LocalDate endDate, String createdBy) {
        this.id = id;
        this.periodType = periodType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = ClosingStatusEnum.OPEN; // Default status
        this.closedBy = createdBy; // createdBy를 최초 담당자로 가정
    }

    // Getter 및 Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PeriodType getPeriodType() { return periodType; }
    public void setPeriodType(PeriodType periodType) { this.periodType = periodType; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public ClosingStatusEnum getStatus() { return status; }
    public void setStatus(ClosingStatusEnum status) {
        this.status = status;
        if (status == ClosingStatusEnum.CLOSED) {
            this.closedAt = LocalDateTime.now();
        } else if (status == ClosingStatusEnum.REOPENED) {
            this.reopenedAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getClosedAt() { return closedAt; }
    // No setter for closedAt, it's set when status changes

    public String getClosedBy() { return closedBy; }
    public void setClosedBy(String closedBy) { this.closedBy = closedBy; }

    public LocalDateTime getReopenedAt() { return reopenedAt; }
    // No setter for reopenedAt, it's set when status changes

    public String getReopenedBy() { return reopenedBy; }
    public void setReopenedBy(String reopenedBy) { this.reopenedBy = reopenedBy; }

    public String getReopenReason() { return reopenReason; }
    public void setReopenReason(String reopenReason) { this.reopenReason = reopenReason; }

    public ApprovalStatus getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(ApprovalStatus approvalStatus) { this.approvalStatus = approvalStatus; }

    public String getApprover() { return approver; }
    public void setApprover(String approver) { this.approver = approver; }

    public LocalDateTime getApprovalDate() { return approvalDate; }
    public void setApprovalDate(LocalDateTime approvalDate) { this.approvalDate = approvalDate; }
}
