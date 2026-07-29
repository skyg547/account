package com.ho.account.masterdata.api.dto;

import com.ho.account.masterdata.core.application.command.MasterDataChangeRequestCommand;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 변경 요청 HTTP 입력 DTO입니다.
 *
 * <p>형식과 길이는 API 경계에서 먼저 검사하고, 변경 유형별 payload와 SCD2 버전 규칙은
 * 도메인/애플리케이션 계층에서 다시 검사합니다.</p>
 */
public class MasterDataChangeRequestCreateDto {

    @NotNull
    private MasterDataType targetType;

    @NotBlank
    @Size(max = 100)
    private String targetKey;

    @NotNull
    private ChangeType changeType;

    @NotNull
    private LocalDate effectiveDate;

    @NotNull
    @Positive
    private Integer requestedVersion;

    @Size(max = 500)
    private String reason;

    private String payloadJson;

    public MasterDataChangeRequestCommand toCommand(String authenticatedRequester) {
        return new MasterDataChangeRequestCommand(
                targetType, targetKey, changeType, effectiveDate, requestedVersion,
                authenticatedRequester, reason, payloadJson);
    }

    public MasterDataType getTargetType() { return targetType; }
    public void setTargetType(MasterDataType targetType) { this.targetType = targetType; }
    public String getTargetKey() { return targetKey; }
    public void setTargetKey(String targetKey) { this.targetKey = targetKey; }
    public ChangeType getChangeType() { return changeType; }
    public void setChangeType(ChangeType changeType) { this.changeType = changeType; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public Integer getRequestedVersion() { return requestedVersion; }
    public void setRequestedVersion(Integer requestedVersion) { this.requestedVersion = requestedVersion; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
}
