package com.ho.account.masterdata.core.domain.changerequest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Controlled master-data change request.
 *
 * <p>The aggregate keeps approval workflow and audit state for sensitive master-data changes.
 * It enforces the REQUESTED -> APPROVED/REJECTED -> APPLIED lifecycle and separation of
 * duties between requester and approver.</p>
 */
@Entity
@Table(name = "master_data_change_requests")
public class MasterDataChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MasterDataType targetType;

    @Column(nullable = false, length = 100)
    private String targetKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChangeType changeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChangeStatus status;

    @Column(nullable = false)
    private LocalDate effectiveDate;

    @Column(nullable = false)
    private Integer requestedVersion;

    @Column(nullable = false, length = 80)
    private String requestedBy;

    @Column(length = 80)
    private String approvedBy;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime approvedAt;

    @Column(length = 500)
    private String reason;

    @Column(columnDefinition = "CLOB")
    private String payloadJson;

    protected MasterDataChangeRequest() {
    }

    public MasterDataChangeRequest(MasterDataType targetType, String targetKey, ChangeType changeType,
            LocalDate effectiveDate, Integer requestedVersion, String requestedBy, String reason, String payloadJson) {
        this.targetType = require(targetType, "Target type is required.");
        this.targetKey = requireText(targetKey, "Target key is required.");
        this.changeType = require(changeType, "Change type is required.");
        this.effectiveDate = require(effectiveDate, "Effective date is required.");
        this.requestedVersion = requirePositiveVersion(requestedVersion);
        this.requestedBy = requireText(requestedBy, "Requester is required.");
        this.reason = reason;
        this.payloadJson = payloadJson;
        this.status = ChangeStatus.REQUESTED;
        this.requestedAt = LocalDateTime.now();
    }

    public void approve(String approver) {
        ensureRequested("Only REQUESTED changes can be approved.");
        String normalizedApprover = requireText(approver, "Approver is required.");
        if (requestedBy.equals(normalizedApprover)) {
            throw new IllegalStateException("Requester and approver must be different users.");
        }
        this.approvedBy = normalizedApprover;
        this.approvedAt = LocalDateTime.now();
        this.status = ChangeStatus.APPROVED;
    }

    public void reject(String approver, String rejectReason) {
        ensureRequested("Only REQUESTED changes can be rejected.");
        this.approvedBy = requireText(approver, "Rejecter is required.");
        this.approvedAt = LocalDateTime.now();
        this.reason = rejectReason;
        this.status = ChangeStatus.REJECTED;
    }

    public void markApplied() {
        if (status != ChangeStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED changes can be applied.");
        }
        this.status = ChangeStatus.APPLIED;
    }

    public boolean isReadyToApply(LocalDate today) {
        return status == ChangeStatus.APPROVED && !today.isBefore(effectiveDate);
    }

    private void ensureRequested(String message) {
        if (status != ChangeStatus.REQUESTED) {
            throw new IllegalStateException(message);
        }
    }

    private static <T> T require(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static Integer requirePositiveVersion(Integer version) {
        if (version == null || version < 1) {
            throw new IllegalArgumentException("Requested version must be 1 or greater.");
        }
        return version;
    }

    public Long getId() { return id; }
    public MasterDataType getTargetType() { return targetType; }
    public String getTargetKey() { return targetKey; }
    public ChangeType getChangeType() { return changeType; }
    public ChangeStatus getStatus() { return status; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public Integer getRequestedVersion() { return requestedVersion; }
    public String getRequestedBy() { return requestedBy; }
    public String getApprovedBy() { return approvedBy; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public String getReason() { return reason; }
    public String getPayloadJson() { return payloadJson; }

    public enum MasterDataType {
        ACCOUNT_SUBJECT, BUSINESS_PARTNER, DEPARTMENT, PRODUCT, CURRENCY, EXCHANGE_RATE, FISCAL_PERIOD
    }

    public enum ChangeType {
        CREATE, UPDATE, DEACTIVATE
    }

    public enum ChangeStatus {
        REQUESTED, APPROVED, REJECTED, APPLIED
    }
}
