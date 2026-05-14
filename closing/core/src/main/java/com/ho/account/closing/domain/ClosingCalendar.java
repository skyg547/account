package com.ho.account.closing.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 寃곗궛 罹섎┛??(Closing Calendar) ?뷀떚??
 * ?뱀젙 ?뚭퀎?곕룄 諛??뚭퀎湲곌컙??寃곗궛 ?쇱젙??愿由ы빀?덈떎.
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

    @Column(name = "fiscal_period", nullable = false, length = 20) // MM, Q1, H1, YEAR ??
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
    private boolean isCurrentPeriod; // ?꾩옱 ?쒖꽦 寃곗궛 湲곌컙 ?щ?

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

    // Getter 諛?Setter
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

        boolean allMandatoryTasksCompleted = safeTasks.stream()
                .filter(ClosingTask::isMandatory)
                .allMatch(t -> t.getStatus() == ClosingTask.ClosingTaskStatus.COMPLETED);
        if (!allMandatoryTasksCompleted) {
            throw new IllegalStateException("Not all mandatory tasks are completed.");
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
        if (this.status == ClosingCalendarStatus.CLOSED || this.status == ClosingCalendarStatus.PERMANENTLY_CLOSED) {
            throw new IllegalStateException("이미 마감된 회기입니다.");
        }
        this.status = ClosingCalendarStatus.CLOSED;
        this.closedBy = user;
        this.closedAt = LocalDateTime.now();
        this.auditUser = user;
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
