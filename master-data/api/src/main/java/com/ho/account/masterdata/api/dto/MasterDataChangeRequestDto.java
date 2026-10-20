package com.ho.account.masterdata.api.dto;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 변경 요청 목록과 mutation 응답의 메타데이터 전용 HTTP 요약입니다.
 * 자유 입력 문자열은 민감 값이 복사될 수 있으므로 이 타입에 포함하지 않습니다.
 */
public class MasterDataChangeRequestDto {

    private final Long id;
    private final MasterDataType targetType;
    private final ChangeType changeType;
    private final ChangeStatus status;
    private final LocalDate effectiveDate;
    private final Integer requestedVersion;
    private final LocalDateTime requestedAt;
    private final LocalDateTime approvedAt;
    private final LocalDateTime appliedAt;

    public MasterDataChangeRequestDto(Long id, MasterDataType targetType, ChangeType changeType,
            ChangeStatus status, LocalDate effectiveDate, Integer requestedVersion,
            LocalDateTime requestedAt, LocalDateTime approvedAt, LocalDateTime appliedAt) {
        this.id = id;
        this.targetType = targetType;
        this.changeType = changeType;
        this.status = status;
        this.effectiveDate = effectiveDate;
        this.requestedVersion = requestedVersion;
        this.requestedAt = requestedAt;
        this.approvedAt = approvedAt;
        this.appliedAt = appliedAt;
    }

    public static MasterDataChangeRequestDto fromEntity(MasterDataChangeRequest entity) {
        // Project only fixed metadata; the stored command and its raw payload remain untouched.
        return new MasterDataChangeRequestDto(entity.getId(), entity.getTargetType(),
                entity.getChangeType(), entity.getStatus(), entity.getEffectiveDate(), entity.getRequestedVersion(),
                entity.getRequestedAt(), entity.getApprovedAt(), entity.getAppliedAt());
    }

    public Long getId() { return id; }
    public MasterDataType getTargetType() { return targetType; }
    public ChangeType getChangeType() { return changeType; }
    public ChangeStatus getStatus() { return status; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public Integer getRequestedVersion() { return requestedVersion; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
}
