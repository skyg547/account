package com.ho.account.masterdata.api.dto;

import com.ho.account.masterdata.core.application.command.MasterDataChangeRequestCommand;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import java.time.LocalDate;

public class MasterDataChangeRequestCreateDto {

    private MasterDataType targetType;
    private String targetKey;
    private ChangeType changeType;
    private LocalDate effectiveDate;
    private Integer requestedVersion;
    private String requestedBy;
    private String reason;
    private String payloadJson;

    public MasterDataChangeRequestCommand toCommand() {
        return new MasterDataChangeRequestCommand(
                targetType, targetKey, changeType, effectiveDate, requestedVersion, requestedBy, reason, payloadJson);
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
    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
}
