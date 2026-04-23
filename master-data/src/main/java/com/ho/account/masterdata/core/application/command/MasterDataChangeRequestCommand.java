package com.ho.account.masterdata.core.application.command;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import java.time.LocalDate;

/**
 * 留덉뒪??蹂寃쎌슂泥??앹꽦 command?낅땲??
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
