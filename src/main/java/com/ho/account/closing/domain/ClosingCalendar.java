package com.ho.account.closing.domain;

import com.ho.account.basic.domain.FiscalPeriod;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 결산 캘린더 (Closing Calendar) 엔티티
 * 특정 회계연도 및 회계기간의 결산 일정을 관리합니다.
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

    @Column(name = "fiscal_period", nullable = false, length = 20) // MM, Q1, H1, YEAR 등
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

    // Getter 및 Setter
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
}
