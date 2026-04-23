package com.ho.account.masterdata.core.application.usecase;

import com.ho.account.masterdata.core.application.command.MasterDataChangeRequestCommand;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import java.util.List;

/**
 * 留덉뒪??蹂寃쎄?由??낅젰 ?ы듃?낅땲??
 */
public interface MasterDataChangeRequestUseCase {

    MasterDataChangeRequest requestChange(MasterDataChangeRequestCommand command);

    MasterDataChangeRequest approve(Long requestId, String approver);

    MasterDataChangeRequest reject(Long requestId, String approver, String reason);

    MasterDataChangeRequest markApplied(Long requestId);

    MasterDataChangeRequest applyApprovedChange(Long requestId);

    List<MasterDataChangeRequest> applyDueApprovedChanges();

    List<MasterDataChangeRequest> findPendingRequests();
}
