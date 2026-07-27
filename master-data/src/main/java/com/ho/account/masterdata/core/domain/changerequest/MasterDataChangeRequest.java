package com.ho.account.masterdata.core.domain.changerequest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import com.ho.account.masterdata.core.domain.exception.MasterDataIdempotencyConflictException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

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

    /**
     * 같은 변경 요청을 두 노드가 동시에 승인하거나 반영하는 기술적 충돌을 감지합니다.
     * 업무상의 SCD2 순번은 requestedVersion과 별도로 검증합니다.
     */
    @Version
    @Column(nullable = false)
    private long lockVersion;

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

    @Column(columnDefinition = "TEXT")
    private String payloadJson;

    /**
     * 외부 승인/이벤트의 안정적인 식별자입니다. 같은 승인 재시도가 새 요청을 만들지 않게 합니다.
     */
    @Column(length = 120, unique = true)
    private String sourceReference;

    private LocalDateTime appliedAt;

    protected MasterDataChangeRequest() {
    }

    public MasterDataChangeRequest(MasterDataType targetType, String targetKey, ChangeType changeType,
            LocalDate effectiveDate, Integer requestedVersion, String requestedBy, String reason, String payloadJson) {
        this(targetType, targetKey, changeType, effectiveDate, requestedVersion,
                requestedBy, reason, payloadJson, null);
    }

    public MasterDataChangeRequest(MasterDataType targetType, String targetKey, ChangeType changeType,
            LocalDate effectiveDate, Integer requestedVersion, String requestedBy, String reason,
            String payloadJson, String sourceReference) {
        this.targetType = require(targetType, "Target type is required.");
        this.targetKey = requireText(targetKey, 100, "Target key is required.");
        this.changeType = require(changeType, "Change type is required.");
        this.effectiveDate = require(effectiveDate, "Effective date is required.");
        this.requestedVersion = requirePositiveVersion(requestedVersion);
        this.requestedBy = requireText(requestedBy, 80, "Requester is required.");
        this.reason = normalizeNullableText(reason, 500, "Reason must be 500 characters or fewer.");
        this.payloadJson = requirePayload(changeType, payloadJson);
        this.sourceReference = normalizeNullableText(
                sourceReference, 120, "Source reference must be 120 characters or fewer.");
        this.status = ChangeStatus.REQUESTED;
        this.requestedAt = LocalDateTime.now();
    }

    public void approve(String approver) {
        ensureRequested("Only REQUESTED changes can be approved.");
        String normalizedApprover = requireText(approver, 80, "Approver is required.");
        if (requestedBy.equals(normalizedApprover)) {
            throw new IllegalStateException("Requester and approver must be different users.");
        }
        this.approvedBy = normalizedApprover;
        this.approvedAt = LocalDateTime.now();
        this.status = ChangeStatus.APPROVED;
    }

    public void reject(String approver, String rejectReason) {
        ensureRequested("Only REQUESTED changes can be rejected.");
        String normalizedApprover = requireText(approver, 80, "Rejecter is required.");
        if (requestedBy.equals(normalizedApprover)) {
            throw new IllegalStateException("Requester and rejecter must be different users.");
        }
        this.approvedBy = normalizedApprover;
        this.approvedAt = LocalDateTime.now();
        this.reason = requireText(rejectReason, 500, "Reject reason is required.");
        this.status = ChangeStatus.REJECTED;
    }

    public void markApplied() {
        if (status != ChangeStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED changes can be applied.");
        }
        this.status = ChangeStatus.APPLIED;
        this.appliedAt = LocalDateTime.now();
    }

    /**
     * 같은 sourceReference가 다른 업무 명령에 재사용되는 사고를 차단합니다.
     */
    public void verifySameChange(MasterDataChangeRequest candidate) {
        if (sourceReference == null || candidate == null
                || !Objects.equals(sourceReference, candidate.sourceReference)
                || targetType != candidate.targetType
                || !Objects.equals(targetKey, candidate.targetKey)
                || changeType != candidate.changeType
                || !Objects.equals(effectiveDate, candidate.effectiveDate)
                || !Objects.equals(requestedVersion, candidate.requestedVersion)
                || !Objects.equals(requestedBy, candidate.requestedBy)
                || !Objects.equals(reason, candidate.reason)
                || !Objects.equals(payloadJson, candidate.payloadJson)) {
            throw new MasterDataIdempotencyConflictException(sourceReference);
        }
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

    private static String requireText(String value, int maxLength, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private static String requirePayload(ChangeType changeType, String payloadJson) {
        if (changeType == ChangeType.DEACTIVATE) {
            // 종료는 승인된 effectiveDate만 사용하므로 불필요한 원문을 저장하지 않습니다.
            return null;
        }
        return requireText(payloadJson, Integer.MAX_VALUE,
                "Payload is required for CREATE and UPDATE changes.");
    }

    private static String normalizeNullableText(String value, int maxLength, String message) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
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
    public String getSourceReference() { return sourceReference; }
    public LocalDateTime getAppliedAt() { return appliedAt; }

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
