package com.ho.account.closing.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "closing_task")
public class ClosingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closing_period_id", nullable = false)
    private ClosingPeriod closingPeriod;

    @Column(nullable = false)
    private String taskName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskType taskType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.PENDING;

    private LocalDateTime completedAt;
    private String completedBy;

    @Column(nullable = false)
    private Integer sequenceOrder;

    private String failureReason;

    public ClosingTask() {}

    public ClosingTask(ClosingPeriod closingPeriod, String taskName, TaskType taskType, Integer sequenceOrder) {
        this.closingPeriod = closingPeriod;
        this.taskName = taskName;
        this.taskType = taskType;
        this.sequenceOrder = sequenceOrder;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ClosingPeriod getClosingPeriod() { return closingPeriod; }
    public void setClosingPeriod(ClosingPeriod closingPeriod) { this.closingPeriod = closingPeriod; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public TaskType getTaskType() { return taskType; }
    public void setTaskType(TaskType taskType) { this.taskType = taskType; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) {
        this.status = status;
        if (status == TaskStatus.COMPLETED) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public String getCompletedBy() { return completedBy; }
    public void setCompletedBy(String completedBy) { this.completedBy = completedBy; }

    public Integer getSequenceOrder() { return sequenceOrder; }
    public void setSequenceOrder(Integer sequenceOrder) { this.sequenceOrder = sequenceOrder; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
