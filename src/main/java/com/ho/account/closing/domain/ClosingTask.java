package com.ho.account.closing.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 결산 태스크 (Closing Task) 엔티티
 * 결산 캘린더에 포함된 개별 태스크(체크리스트 항목)를 정의합니다.
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

    @Column(nullable = false, length = 200)
    private String name; // 태스크명 (예: "은행 대사 완료", "외화 평가 실행")

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ClosingTaskCategory category; // PRE_CLOSING, CLOSING_ENTRY, POST_CLOSING

    private LocalDateTime dueDate; // 태스크 기한

    @Column(length = 50)
    private String assignedTo; // 담당자

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ClosingTaskStatus status; // PENDING, IN_PROGRESS, COMPLETED, FAILED

    // 태스크 완료 조건 (예: 특정 ReconciliationUnit의 ReconciliationRun이 SUCCESS 상태 등)
    @Column(columnDefinition = "TEXT")
    private String completionConditionJson;

    @Column(nullable = false)
    private boolean isMandatory; // 필수 태스크 여부

    @Column(nullable = false)
    private Integer taskOrder; // 태스크 순서

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

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

    public ClosingCalendar getClosingCalendar() {
        return closingCalendar;
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
}
