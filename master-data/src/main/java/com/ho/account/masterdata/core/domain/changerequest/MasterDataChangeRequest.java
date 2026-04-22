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
 * 마스터 변경요청 도메인입니다.
 *
 * <p>마스터 데이터는 전표, 원장, 보고가 공통으로 믿고 쓰는 기준입니다. 그래서 운영자가 바로
 * 값을 바꾸지 않고 "요청 -> 승인/반려 -> 적용" 상태를 남겨야 감사 추적이 가능합니다.</p>
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
        this.targetType = require(targetType, "대상 마스터 유형은 필수입니다.");
        this.targetKey = requireText(targetKey, "대상 키는 필수입니다.");
        this.changeType = require(changeType, "변경 유형은 필수입니다.");
        this.effectiveDate = require(effectiveDate, "적용일은 필수입니다.");
        this.requestedVersion = requirePositiveVersion(requestedVersion);
        this.requestedBy = requireText(requestedBy, "요청자는 필수입니다.");
        this.reason = reason;
        this.payloadJson = payloadJson;
        this.status = ChangeStatus.REQUESTED;
        this.requestedAt = LocalDateTime.now();
    }

    public void approve(String approver) {
        ensureRequested("승인은 REQUESTED 상태에서만 가능합니다.");
        String normalizedApprover = requireText(approver, "승인자는 필수입니다.");
        if (requestedBy.equals(normalizedApprover)) {
            throw new IllegalStateException("요청자와 승인자는 같을 수 없습니다.");
        }
        this.approvedBy = normalizedApprover;
        this.approvedAt = LocalDateTime.now();
        this.status = ChangeStatus.APPROVED;
    }

    public void reject(String approver, String rejectReason) {
        ensureRequested("반려는 REQUESTED 상태에서만 가능합니다.");
        this.approvedBy = requireText(approver, "반려자는 필수입니다.");
        this.approvedAt = LocalDateTime.now();
        this.reason = rejectReason;
        this.status = ChangeStatus.REJECTED;
    }

    public void markApplied() {
        if (status != ChangeStatus.APPROVED) {
            throw new IllegalStateException("적용 완료 처리는 APPROVED 상태에서만 가능합니다.");
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
            throw new IllegalArgumentException("요청 버전은 1 이상이어야 합니다.");
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
