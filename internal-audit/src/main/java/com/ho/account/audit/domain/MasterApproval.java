package com.ho.account.audit.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 마스터 승인 이력 엔티티 (SOD 적용용)
 */
@Entity
@Table(name = "MASTER_APPROVAL")
public class MasterApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String masterType; // 예: ACCOUNT_SUBJECT, DEPARTMENT, BUSINESS_PARTNER

    @Column(nullable = false, length = 100)
    private String masterKey; // 마스터 코드 또는 ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChangeRequestType requestType;

    @Column(columnDefinition = "CLOB")
    private String payload; // 변경될 데이터 (JSON 형태 저장 권장)

    @Column(nullable = false, length = 50)
    private String requestUser;

    @Column(nullable = false)
    private LocalDateTime requestDate;

    @Column(length = 50)
    private String approverUser;

    private LocalDateTime approvalDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus status;

    @Column(name = "EFFECTIVE_DATE")
    private LocalDate effectiveDate;

    @Column(name = "REQUESTED_VERSION")
    private Integer requestedVersion;

    @Column(length = 500)
    private String remarks;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(nullable = false, length = 50)
    private String auditUser;

    public enum ChangeRequestType {
        CREATE, UPDATE, DELETE
    }

    public enum ApprovalStatus {
        PENDING, APPROVED, REJECTED
    }

    public void approve(String approverUser, String remarks, LocalDateTime approvedAt) {
        if (this.status != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Only PENDING approval can be approved.");
        }
        String normalizedApprover = requireText(approverUser, "Approver user is required.");
        if (normalizedApprover.equals(this.requestUser)) {
            throw new IllegalStateException("Self-approval is not allowed (SOD Violation)");
        }

        this.status = ApprovalStatus.APPROVED;
        this.approverUser = normalizedApprover;
        this.approvalDate = approvedAt != null ? approvedAt : LocalDateTime.now();
        this.remarks = remarks;
        this.auditUser = normalizedApprover;
    }

    public void reject(String approverUser, String remarks, LocalDateTime rejectedAt) {
        if (this.status != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Only PENDING approval can be rejected.");
        }
        String normalizedApprover = requireText(approverUser, "Approver user is required.");

        this.status = ApprovalStatus.REJECTED;
        this.approverUser = normalizedApprover;
        this.approvalDate = rejectedAt != null ? rejectedAt : LocalDateTime.now();
        this.remarks = remarks;
        this.auditUser = normalizedApprover;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.requestDate = LocalDateTime.now();
        this.status = ApprovalStatus.PENDING;
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

    public String getMasterType() {
        return masterType;
    }

    public void setMasterType(String masterType) {
        this.masterType = masterType;
    }

    public String getMasterKey() {
        return masterKey;
    }

    public void setMasterKey(String masterKey) {
        this.masterKey = masterKey;
    }

    public ChangeRequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(ChangeRequestType requestType) {
        this.requestType = requestType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getRequestUser() {
        return requestUser;
    }

    public void setRequestUser(String requestUser) {
        this.requestUser = requestUser;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public String getApproverUser() {
        return approverUser;
    }

    public void setApproverUser(String approverUser) {
        this.approverUser = approverUser;
    }

    public LocalDateTime getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(LocalDateTime approvalDate) {
        this.approvalDate = approvalDate;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public void setStatus(ApprovalStatus status) {
        this.status = status;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public Integer getRequestedVersion() {
        return requestedVersion;
    }

    public void setRequestedVersion(Integer requestedVersion) {
        this.requestedVersion = requestedVersion;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getAuditUser() {
        return auditUser;
    }

    public void setAuditUser(String auditUser) {
        this.auditUser = auditUser;
    }
}
