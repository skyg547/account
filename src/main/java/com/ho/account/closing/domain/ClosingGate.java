package com.ho.account.closing.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 결산 게이트 (Closing Gate) 엔티티
 * 결산 진행의 중요한 단계를 나타내며, 다음 단계로 진행하기 위한 조건을 정의합니다.
 */
@Entity
@Table(name = "closing_gates")
public class ClosingGate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_id", nullable = false)
    private ClosingCalendar closingCalendar;

    @Column(nullable = false, length = 100)
    private String name; // 게이트명 (예: "PRE-CLOSING 완료", "조정 전표 완료")

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ClosingGateStatus status; // PENDING, PASSED, FAILED

    // 게이트 통과 조건 (예: 모든 ClosingTask가 완료 상태, 특정 ReconciliationRun이 성공 상태 등)
    @Column(columnDefinition = "TEXT")
    private String checkConditionJson;

    @Column(length = 50)
    private String passedBy;

    private LocalDateTime passedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    public enum ClosingGateStatus {
        PENDING, PASSED, FAILED
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null)
            this.status = ClosingGateStatus.PENDING;
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
}
