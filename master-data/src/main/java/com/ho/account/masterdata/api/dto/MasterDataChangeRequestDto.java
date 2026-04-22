package com.ho.account.masterdata.api.dto;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class MasterDataChangeRequestDto {

    private final Long id;
    private final MasterDataType targetType;
    private final String targetKey;
    private final ChangeType changeType;
    private final ChangeStatus status;
    private final LocalDate effectiveDate;
    private final Integer requestedVersion;
    private final String requestedBy;
    private final String approvedBy;
    private final LocalDateTime requestedAt;
    private final LocalDateTime approvedAt;
    private final String reason;
    private final String payloadJson;

    public MasterDataChangeRequestDto(Long id, MasterDataType targetType, String targetKey, ChangeType changeType,
            ChangeStatus status, LocalDate effectiveDate, Integer requestedVersion, String requestedBy,
            String approvedBy, LocalDateTime requestedAt, LocalDateTime approvedAt, String reason, String payloadJson) {
        this.id = id;
        this.targetType = targetType;
        this.targetKey = targetKey;
        this.changeType = changeType;
        this.status = status;
        this.effectiveDate = effectiveDate;
        this.requestedVersion = requestedVersion;
        this.requestedBy = requestedBy;
        this.approvedBy = approvedBy;
        this.requestedAt = requestedAt;
        this.approvedAt = approvedAt;
        this.reason = reason;
        this.payloadJson = payloadJson;
    }

    public static MasterDataChangeRequestDto fromEntity(MasterDataChangeRequest entity) {
        return new MasterDataChangeRequestDto(entity.getId(), entity.getTargetType(), entity.getTargetKey(),
                entity.getChangeType(), entity.getStatus(), entity.getEffectiveDate(), entity.getRequestedVersion(),
                entity.getRequestedBy(), entity.getApprovedBy(), entity.getRequestedAt(), entity.getApprovedAt(),
                entity.getReason(), entity.getPayloadJson());
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
}
