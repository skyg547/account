package com.ho.account.closing.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 결산 태스크(Closing Task) 엔티티 — 마감 전 수행해야 할 개별 작업 단위를 정의하고 상태를 관리합니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 결산 태스크는 '마감 전 체크리스트의 한 줄'과 같습니다. 
 * "은행 잔고 대조 완료", "부가세 신고 준비" 등 마감을 위해 반드시 끝내야 하는 숙제들입니다. 
 * 이 숙제가 모두 '완료' 상태가 되어야만 비로소 그달의 장부에 자물쇠를 채울 수 있습니다.
 */
@Entity
@Table(name = "closing_tasks")
public class ClosingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_id", nullable = false)
    private ClosingCalendar closingCalendar;

    @Column(name = "cycle_number", nullable = false)
    private int cycleNumber = 1;

    @Column(nullable = false, length = 200)
    private String name; // 태스크명 (예: "은행잔고 대조 완료", "외화 평가 실행")

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ClosingTaskCategory category; // PRE_CLOSING, CLOSING_ENTRY, POST_CLOSING

    private LocalDateTime dueDate; // 태스크 완료 기한

    @Column(length = 50)
    private String assignedTo; // 담당자

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ClosingTaskStatus status; // PENDING, IN_PROGRESS, COMPLETED, FAILED

    // 태스크 완료 조건(JSON). 예: 특정 대사 실행이 SUCCESS 상태인지 확인하는 조건.
    @Column(columnDefinition = "TEXT")
    private String completionConditionJson;

    @Column(nullable = false)
    private boolean isMandatory; // 필수 태스크 여부

    @Column(nullable = false)
    private Integer taskOrder; // 태스크 실행 순서

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    @Transient
    private String taskCode;

    public enum ClosingTaskCategory {
        PRE_CLOSING, CLOSING_ENTRY, POST_CLOSING, REPORTING
    }

    public enum ClosingTaskStatus {
        PENDING, IN_PROGRESS, COMPLETED, FAILED, SKIPPED
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null)
            this.status = ClosingTaskStatus.PENDING;
        if (this.auditUser == null)
            this.auditUser = "SYSTEM";
        if (this.cycleNumber <= 0)
            this.cycleNumber = 1;
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

    public ClosingCalendar getClosingCalendar() {
        return closingCalendar;
    }

    public int getCycleNumber() {
        return cycleNumber;
    }

    public void assignCycle(ClosingCalendar calendar) {
        if (calendar == null) {
            throw new IllegalArgumentException("calendar must not be null");
        }
        this.closingCalendar = calendar;
        this.cycleNumber = calendar.getCycleNumber();
    }

    /** Copies only the checklist definition; completion evidence belongs to the old cycle. */
    public ClosingTask nextCycle(ClosingCalendar calendar, String actor) {
        ClosingTask next = new ClosingTask();
        next.assignCycle(calendar);
        next.name = name;
        next.description = description;
        next.category = category;
        next.dueDate = dueDate;
        next.assignedTo = assignedTo;
        next.completionConditionJson = completionConditionJson;
        next.isMandatory = isMandatory;
        next.taskOrder = taskOrder;
        next.status = ClosingTaskStatus.PENDING;
        next.auditUser = actor;
        return next;
    }

    public void setClosingCalendar(ClosingCalendar closingCalendar) {
        this.closingCalendar = closingCalendar;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ClosingTaskCategory getCategory() {
        return category;
    }

    public void setCategory(ClosingTaskCategory category) {
        this.category = category;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public ClosingTaskStatus getStatus() {
        return status;
    }

    public void setStatus(ClosingTaskStatus status) {
        this.status = status;
    }

    public String getCompletionConditionJson() {
        return completionConditionJson;
    }

    public void setCompletionConditionJson(String completionConditionJson) {
        this.completionConditionJson = completionConditionJson;
    }

    public boolean isMandatory() {
        return isMandatory;
    }

    public void setMandatory(boolean mandatory) {
        isMandatory = mandatory;
    }

    public Integer getTaskOrder() {
        return taskOrder;
    }

    public void setTaskOrder(Integer taskOrder) {
        this.taskOrder = taskOrder;
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

    /**
     * 태스크를 완료 상태로 변경합니다.
     *
     * @param user 처리자
     */
    public void complete(String user) {
        requireActor(user);
        if (this.status != ClosingTaskStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only an IN_PROGRESS task can be completed.");
        }
        this.status = ClosingTaskStatus.COMPLETED;
        this.auditUser = user.trim();
    }

    /**
     * 태스크를 진행 중 상태로 변경합니다.
     *
     * @param user 처리자
     */
    public void start(String user) {
        requireActor(user);
        if (this.status != ClosingTaskStatus.PENDING && this.status != ClosingTaskStatus.FAILED) {
            throw new IllegalStateException("Only a PENDING or FAILED task can start.");
        }
        this.status = ClosingTaskStatus.IN_PROGRESS;
        this.auditUser = user.trim();
    }

    public void changeStatus(ClosingTaskStatus newStatus, String user) {
        if (newStatus == null) {
            throw new IllegalArgumentException("newStatus must not be null");
        }
        if (newStatus == ClosingTaskStatus.IN_PROGRESS) {
            start(user);
            return;
        }
        if (newStatus == ClosingTaskStatus.COMPLETED) {
            complete(user);
            return;
        }
        requireActor(user);
        if (newStatus == ClosingTaskStatus.FAILED && this.status == ClosingTaskStatus.IN_PROGRESS) {
            this.status = newStatus;
            this.auditUser = user.trim();
            return;
        }
        if (newStatus == ClosingTaskStatus.SKIPPED
                && this.status == ClosingTaskStatus.PENDING
                && !this.isMandatory) {
            this.status = newStatus;
            this.auditUser = user.trim();
            return;
        }
        throw new IllegalStateException("Invalid closing task transition: " + this.status + " -> " + newStatus);
    }

    private void requireActor(String user) {
        if (user == null || user.isBlank()) {
            throw new IllegalArgumentException("user must not be blank");
        }
    }

    @Deprecated
    public String getTaskCode() {
        return taskCode;
    }

    @Deprecated
    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
    }
}
