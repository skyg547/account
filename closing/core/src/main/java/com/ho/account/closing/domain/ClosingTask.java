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

    @Column(nullable = false, length = 200)
    private String name; // 태스크명 (예: "은행잔고 대조 완료", "외화 평가 실행")

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ClosingTaskCategory category; // PRE_CLOSING, CLOSING_ENTRY, POST_CLOSING

    private LocalDateTime dueDate; // ?쒖뒪??湲고븳

    @Column(length = 50)
    private String assignedTo; // ?대떦??

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ClosingTaskStatus status; // PENDING, IN_PROGRESS, COMPLETED, FAILED

    // ?쒖뒪???꾨즺 議곌굔 (?? ?뱀젙 ReconciliationUnit??ReconciliationRun??SUCCESS ?곹깭 ??
    @Column(columnDefinition = "TEXT")
    private String completionConditionJson;

    @Column(nullable = false)
    private boolean isMandatory; // ?꾩닔 ?쒖뒪???щ?

    @Column(nullable = false)
    private Integer taskOrder; // ?쒖뒪???쒖꽌

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

    /**
     * 태스크를 완료 상태로 변경합니다.
     *
     * @param user 처리자
     */
    public void complete(String user) {
        this.status = ClosingTaskStatus.COMPLETED;
        this.auditUser = user;
    }

    /**
     * 태스크를 진행 중 상태로 변경합니다.
     *
     * @param user 처리자
     */
    public void start(String user) {
        this.status = ClosingTaskStatus.IN_PROGRESS;
        this.auditUser = user;
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
