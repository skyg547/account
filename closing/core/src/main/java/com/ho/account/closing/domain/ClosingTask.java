package com.ho.account.closing.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 寃곗궛 ?쒖뒪??(Closing Task) ?뷀떚??
 * 寃곗궛 罹섎┛?붿뿉 ?ы븿??媛쒕퀎 ?쒖뒪??泥댄겕由ъ뒪????ぉ)瑜??뺤쓽?⑸땲??
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
    private String name; // ?쒖뒪?щ챸 (?? "???????꾨즺", "?명솕 ?됯? ?ㅽ뻾")

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

    @Deprecated
    public String getTaskCode() {
        return taskCode;
    }

    @Deprecated
    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
    }
}
