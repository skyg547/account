package com.ho.account.masterdata.core.application.command;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import java.time.LocalDate;

/**
 * Command for requesting controlled master-data changes.
 */
public record MasterDataChangeRequestCommand(
        MasterDataType targetType,
        String targetKey,
        ChangeType changeType,
        LocalDate effectiveDate,
        Integer requestedVersion,
        String requestedBy,
        String reason,
        String payloadJson,
        String sourceReference) {

    public MasterDataChangeRequestCommand(
            MasterDataType targetType,
            String targetKey,
            ChangeType changeType,
            LocalDate effectiveDate,
            Integer requestedVersion,
            String requestedBy,
            String reason,
            String payloadJson) {
        this(targetType, targetKey, changeType, effectiveDate, requestedVersion,
                requestedBy, reason, payloadJson, null);
    }
}
