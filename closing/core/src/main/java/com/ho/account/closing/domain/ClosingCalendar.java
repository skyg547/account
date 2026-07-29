package com.ho.account.closing.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 결산 캘린더(Closing Calendar) 엔티티.
 *
 * <p>특정 회계연도와 회계기간의 결산 진행 상태를 관리합니다.
 * 초보자 관점에서는 "이번 달 마감표"라고 이해하면 됩니다.
 */
@Entity
@Table(name = "closing_calendars", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "fiscal_year", "fiscal_period" })
})
public class ClosingCalendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fiscal_year", nullable = false, length = 4)
    private String fiscalYear;

    @Column(name = "fiscal_period", nullable = false, length = 20) // MM, Q1, H1, YEAR 등 기간 코드
    private String fiscalPeriod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ClosingCalendarStatus status; // OPEN, IN_PROGRESS, CLOSED, PERMANENTLY_CLOSED

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

    @Transient
    private String name;

    public enum ClosingCalendarStatus {
        OPEN, IN_PROGRESS, CLOSED, PERMANENTLY_CLOSED
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null)
            this.status = ClosingCalendarStatus.OPEN;
        if (this.auditUser == null)
            this.auditUser = "SYSTEM";
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

    public String getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(String fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public String getFiscalPeriod() {
        return fiscalPeriod;
    }

    public void setFiscalPeriod(String fiscalPeriod) {
        this.fiscalPeriod = fiscalPeriod;
    }

    public ClosingCalendarStatus getStatus() {
        return status;
    }

    public void setStatus(ClosingCalendarStatus status) {
        this.status = status;
    }

    public String getCloseInitiatedBy() {
        return closeInitiatedBy;
    }

    public void setCloseInitiatedBy(String closeInitiatedBy) {
        this.closeInitiatedBy = closeInitiatedBy;
    }

    public LocalDateTime getCloseInitiatedAt() {
        return closeInitiatedAt;
    }

    public void setCloseInitiatedAt(LocalDateTime closeInitiatedAt) {
        this.closeInitiatedAt = closeInitiatedAt;
    }

    public String getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(String closedBy) {
        this.closedBy = closedBy;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public String getReopenedBy() {
        return reopenedBy;
    }

    public void setReopenedBy(String reopenedBy) {
        this.reopenedBy = reopenedBy;
    }

    public LocalDateTime getReopenedAt() {
        return reopenedAt;
    }

    public void setReopenedAt(LocalDateTime reopenedAt) {
        this.reopenedAt = reopenedAt;
    }

    public boolean isCurrentPeriod() {
        return isCurrentPeriod;
    }

    public void setCurrentPeriod(boolean currentPeriod) {
        isCurrentPeriod = currentPeriod;
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
     * 결산 완료 가능 여부를 확인합니다.
     *
     * @param tasks 현재 회기의 결산 태스크 목록
     * @param gates 현재 회기의 결산 게이트 목록
     * @return 완료 가능 여부
     */
    public boolean isReadyToClose(java.util.List<ClosingTask> tasks, java.util.List<ClosingGate> gates) {
        java.util.List<ClosingTask> safeTasks = tasks != null ? tasks : java.util.List.of();
        java.util.List<ClosingGate> safeGates = gates != null ? gates : java.util.List.of();
        if (safeTasks.stream().noneMatch(ClosingTask::isMandatory) || safeGates.isEmpty()) {
            return false;
        }

        // 1. 필수 태스크 완료 여부 확인
        boolean allMandatoryTasksCompleted = safeTasks.stream()
                .filter(ClosingTask::isMandatory)
                .allMatch(t -> t.getStatus() == ClosingTask.ClosingTaskStatus.COMPLETED);

        if (!allMandatoryTasksCompleted) return false;

        // 2. 모든 게이트 통과 여부 확인
        boolean allGatesPassed = safeGates.stream()
                .allMatch(g -> g.getStatus() == ClosingGate.ClosingGateStatus.PASSED);

        return allGatesPassed;
    }

    /**
     * 결산 완료 가능 여부를 검증하고 실패 사유를 업무 메시지로 구분합니다.
     *
     * @param tasks 현재 회기의 결산 태스크 목록
     * @param gates 현재 회기의 결산 게이트 목록
     */
    public void validateReadyToClose(java.util.List<ClosingTask> tasks, java.util.List<ClosingGate> gates) {
        java.util.List<ClosingTask> safeTasks = tasks != null ? tasks : java.util.List.of();
        java.util.List<ClosingGate> safeGates = gates != null ? gates : java.util.List.of();
        if (safeTasks.stream().noneMatch(ClosingTask::isMandatory)) {
            throw new IllegalStateException("At least one mandatory closing task must be defined.");
        }

        boolean allMandatoryTasksCompleted = safeTasks.stream()
                .filter(ClosingTask::isMandatory)
                .allMatch(t -> t.getStatus() == ClosingTask.ClosingTaskStatus.COMPLETED);
        if (!allMandatoryTasksCompleted) {
            throw new IllegalStateException("Not all mandatory tasks are completed.");
        }

        if (safeGates.isEmpty()) {
            throw new IllegalStateException("At least one closing gate must be defined.");
        }
        boolean allGatesPassed = safeGates.stream()
                .allMatch(g -> g.getStatus() == ClosingGate.ClosingGateStatus.PASSED);
        if (!allGatesPassed) {
            throw new IllegalStateException("Not all closing gates are passed.");
        }
    }

    /**
     * 결산을 최종 확정 상태로 변경합니다.
     *
     * @param user 처리자
     * @throws IllegalStateException 완료 가능한 상태가 아닐 경우
     */
    public void close(String user) {
        requireActor(user);
        if (this.status != ClosingCalendarStatus.IN_PROGRESS) {
            throw new IllegalStateException("Closing calendar must be IN_PROGRESS before it can be closed.");
        }
        this.status = ClosingCalendarStatus.CLOSED;
        this.closedBy = user.trim();
        this.closedAt = LocalDateTime.now();
        this.auditUser = user.trim();
    }

    public void start(String user) {
        requireActor(user);
        if (this.status != ClosingCalendarStatus.OPEN) {
            throw new IllegalStateException("Only an OPEN closing calendar can start.");
        }
        this.status = ClosingCalendarStatus.IN_PROGRESS;
        this.closeInitiatedBy = user.trim();
        this.closeInitiatedAt = LocalDateTime.now();
        this.auditUser = user.trim();
    }

    public void reopen(String user) {
        requireActor(user);
        if (this.status != ClosingCalendarStatus.CLOSED) {
            throw new IllegalStateException("Only a CLOSED closing calendar can be reopened.");
        }
        this.status = ClosingCalendarStatus.OPEN;
        this.reopenedBy = user.trim();
        this.reopenedAt = LocalDateTime.now();
        this.auditUser = user.trim();
    }

    private void requireActor(String user) {
        if (user == null || user.isBlank()) {
            throw new IllegalArgumentException("user must not be blank");
        }
    }

    @Deprecated
    public String getName() {
        return name;
    }

    @Deprecated
    public void setName(String name) {
        this.name = name;
    }
}
