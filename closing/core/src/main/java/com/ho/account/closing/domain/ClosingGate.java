package com.ho.account.closing.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 결산 게이트(Closing Gate) 엔티티.
 *
 * <p>결산 진행 중 중요한 체크포인트를 표현합니다.
 * 초보자 관점에서는 "다음 단계로 넘어가기 전에 반드시 통과해야 하는 문"입니다.
 */

public class ClosingGate {

    private Long id;

    private ClosingCalendar closingCalendar;

    private String name; // 게이트명 (예: "PRE-CLOSING 완료", "조정 전표 검토 완료")

    private String description;

    private ClosingGateStatus status; // PENDING, PASSED, FAILED

    // 게이트 통과 조건(JSON). 예: 모든 필수 ClosingTask가 완료 상태인지 확인하는 조건.

    private String checkConditionJson;

    private String passedBy;

    private LocalDateTime passedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String auditUser;

    private String gateCode;

    public enum ClosingGateStatus {
        PENDING, PASSED, FAILED
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

    public ClosingGateStatus getStatus() {
        return status;
    }

    public void setStatus(ClosingGateStatus status) {
        this.status = status;
    }

    public String getCheckConditionJson() {
        return checkConditionJson;
    }

    public void setCheckConditionJson(String checkConditionJson) {
        this.checkConditionJson = checkConditionJson;
    }

    public String getPassedBy() {
        return passedBy;
    }

    public void setPassedBy(String passedBy) {
        this.passedBy = passedBy;
    }

    public LocalDateTime getPassedAt() {
        return passedAt;
    }

    public void setPassedAt(LocalDateTime passedAt) {
        this.passedAt = passedAt;
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

    public void pass(String user, List<ClosingTask> tasks) {
        if (user == null || user.isBlank()) {
            throw new IllegalArgumentException("user must not be blank");
        }
        if (this.status != ClosingGateStatus.PENDING) {
            throw new IllegalStateException("Only a PENDING closing gate can pass.");
        }
        List<ClosingTask> safeTasks = tasks == null ? List.of() : tasks;
        boolean mandatoryTasksCompleted = safeTasks.stream()
                .filter(ClosingTask::isMandatory)
                .allMatch(task -> task.getStatus() == ClosingTask.ClosingTaskStatus.COMPLETED);
        if (safeTasks.stream().noneMatch(ClosingTask::isMandatory) || !mandatoryTasksCompleted) {
            throw new IllegalStateException("All mandatory closing tasks must be completed before passing a gate.");
        }
        this.status = ClosingGateStatus.PASSED;
        this.passedBy = user.trim();
        this.passedAt = LocalDateTime.now();
        this.auditUser = user.trim();
    }

    @Deprecated
    public String getGateCode() {
        return gateCode;
    }

    @Deprecated
    public void setGateCode(String gateCode) {
        this.gateCode = gateCode;
    }
}
