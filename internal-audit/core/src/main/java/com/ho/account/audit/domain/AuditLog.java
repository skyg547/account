package com.ho.account.shared.infrastructure.security.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 감사 로그(Audit Log) 엔티티 — 시스템 내 모든 중요한 데이터 변경 이력을 기록합니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 감사 로그는 비행기의 '블랙박스'나 CCTV와 같습니다. 
 * 누군가 시스템에 들어와서 중요한 장부를 수정하거나 지웠을 때, 
 * "누가(auditUser)", "언제(eventDateTime)", "어디서(ipAddress)", "어떤 데이터(targetEntity)를", 
 * "수정 전(beforeData)은 어땠는데, 수정 후(afterData)는 어떻게 바꾸었는지" 
 * 꼼꼼하게 빠짐없이 기록해두는 보안 장치입니다. 문제가 생기면 제일 먼저 열어보는 데이터입니다.
 */
@Entity
@Table(name = "AUDIT_LOG")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "EVENT_TYPE", nullable = false, length = 50)
    private String eventType;

    @Column(name = "EVENT_DATE_TIME", nullable = false)
    private LocalDateTime eventDateTime;

    @Column(name = "USER_ID", nullable = false, length = 50)
    private String userId;

    @Column(name = "TARGET_ENTITY", nullable = false, length = 100)
    private String targetEntity;

    @Column(name = "TARGET_ID", nullable = false, length = 100)
    private String targetId;

    @Column(name = "BEFORE_DATA", columnDefinition = "TEXT")
    private String beforeData;

    @Column(name = "AFTER_DATA", columnDefinition = "TEXT")

    private String afterData;

    @Column(name = "STATUS", length = 20)
    private String status;

    @Column(name = "REMARKS", length = 1000)
    private String remarks;

    @Column(name = "IP_ADDRESS", length = 50)
    private String ipAddress;

    @Column(name = "CREATE_DATE", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Column(name = "UPDATE_DATE", nullable = false)
    private LocalDateTime updateDate;

    @Column(name = "AUDIT_USER", nullable = false, length = 50)
    private String auditUser;

    @PrePersist
    protected void onCreate() {
        this.createDate = LocalDateTime.now();
        this.updateDate = LocalDateTime.now();
        if (this.eventDateTime == null)
            this.eventDateTime = LocalDateTime.now();
        if (this.auditUser == null)
            this.auditUser = "SYSTEM";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateDate = LocalDateTime.now();
    }

    // Getter 및 Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getEventDateTime() {
        return eventDateTime;
    }

    public void setEventDateTime(LocalDateTime eventDateTime) {
        this.eventDateTime = eventDateTime;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTargetEntity() {
        return targetEntity;
    }

    public void setTargetEntity(String targetEntity) {
        this.targetEntity = targetEntity;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getBeforeData() {
        return beforeData;
    }

    public void setBeforeData(String beforeData) {
        this.beforeData = beforeData;
    }

    public String getAfterData() {
        return afterData;
    }

    public void setAfterData(String afterData) {
        this.afterData = afterData;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(LocalDateTime createDate) {
        this.createDate = createDate;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }

    public String getAuditUser() {
        return auditUser;
    }

    public void setAuditUser(String auditUser) {
        this.auditUser = auditUser;
    }
}
