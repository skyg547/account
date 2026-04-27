package com.ho.account.closing.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 寃곗궛 寃뚯씠??(Closing Gate) ?뷀떚??
 * 寃곗궛 吏꾪뻾??以묒슂???④퀎瑜??섑??대ŉ, ?ㅼ쓬 ?④퀎濡?吏꾪뻾?섍린 ?꾪븳 議곌굔???뺤쓽?⑸땲??
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
    private String name; // 寃뚯씠?몃챸 (?? "PRE-CLOSING ?꾨즺", "議곗젙 ?꾪몴 ?꾨즺")

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ClosingGateStatus status; // PENDING, PASSED, FAILED

    // 寃뚯씠???듦낵 議곌굔 (?? 紐⑤뱺 ClosingTask媛 ?꾨즺 ?곹깭, ?뱀젙 ReconciliationRun???깃났 ?곹깭 ??
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

    @Transient
    private String gateCode;

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

    @Deprecated
    public String getGateCode() {
        return gateCode;
    }

    @Deprecated
    public void setGateCode(String gateCode) {
        this.gateCode = gateCode;
    }
}
