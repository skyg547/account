package com.ho.account.masterdata.core.application.port.in;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.application.command.MasterDataChangeRequestCommand;
import java.util.List;

/**
 * 마스터 데이터 변경 요청 유즈케이스
 */
public interface MasterDataChangeRequestUseCase {
    MasterDataChangeRequest requestChange(MasterDataChangeRequestCommand command);
    
    List<MasterDataChangeRequest> findPendingRequests();
    
    MasterDataChangeRequest approve(Long requestId, String approver);
    
    MasterDataChangeRequest reject(Long requestId, String approver, String reason);
    
    MasterDataChangeRequest applyApprovedChange(Long requestId);
    
    List<MasterDataChangeRequest> applyDueApprovedChanges();
}
