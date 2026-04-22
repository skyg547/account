package com.ho.account.masterdata.core.application.usecase;

import com.ho.account.masterdata.core.application.command.MasterDataChangeRequestCommand;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import java.util.List;

/**
 * 마스터 변경관리 입력 포트입니다.
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
