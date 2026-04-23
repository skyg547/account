package com.ho.account.masterdata.core.application.command;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import java.time.LocalDate;

/**
 * ë‰???‚ÂƒìŒ?‚ï???¹ê½¦ command??…ë•²??
 */
public record MasterDataChangeRequestCommand(
        MasterDataType targetType,
        String targetKey,
        ChangeType changeType,
        LocalDate effectiveDate,
        Integer requestedVersion,
        String requestedBy,
        String reason,
        String payloadJson) {
}
