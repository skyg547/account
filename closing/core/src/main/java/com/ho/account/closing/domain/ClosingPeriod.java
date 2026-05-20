package com.ho.account.closing.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [DDD(도메인 주도 설계) - Aggregate Root]
 * 결산 기수(Closing Period) 엔티티. 특정 회계 기간의 상태(오픈, 마감 등)와 승인 워크플로우를 관리합니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 결산 기수는 쉽게 말해 '한 달치 장부(또는 1년치 장부)'를 의미합니다.
 * 회계에서는 한 달이 끝나면 장부를 닫아(Close) 더 이상 수정할 수 없게 막는데(마감잠금), 
 * 이 클래스가 그 장부의 '자물쇠' 역할을 합니다. 만약 특별한 이유로 과거 장부를 다시 열어야 한다면,
 * 엄격한 '재오픈 승인(Reopen Approval)' 과정을 거치도록 설계되어 있습니다.
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
        this.closedBy = createdBy; // createdBy瑜?理쒖큹 ?대떦?먮줈 媛??
    }

    // Getter 諛?Setter
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
