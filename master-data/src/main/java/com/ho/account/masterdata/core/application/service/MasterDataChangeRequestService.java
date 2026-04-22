package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.command.MasterDataChangeRequestCommand;
import com.ho.account.masterdata.core.application.usecase.MasterDataChangeRequestUseCase;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.port.out.MasterDataChangeRequestPersistencePort;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 마스터 변경관리 유스케이스 서비스입니다.
 *
 * <p>이 서비스는 변경요청의 상태 흐름만 관리합니다. 실제 계정과목/거래처 row를 바꾸는 작업은
 * 승인된 요청을 적용하는 별도 application service 또는 batch가 담당하도록 분리합니다.</p>
 */
@Service
@Transactional
public class MasterDataChangeRequestService implements MasterDataChangeRequestUseCase {

    private final MasterDataChangeRequestPersistencePort persistencePort;

    public MasterDataChangeRequestService(MasterDataChangeRequestPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    @Override
    public MasterDataChangeRequest requestChange(MasterDataChangeRequestCommand command) {
        MasterDataChangeRequest request = new MasterDataChangeRequest(
                command.targetType(),
                command.targetKey(),
                command.changeType(),
                command.effectiveDate(),
                command.requestedVersion(),
                command.requestedBy(),
                command.reason(),
                command.payloadJson());
        return persistencePort.save(request);
    }

    @Override
    public MasterDataChangeRequest approve(Long requestId, String approver) {
        MasterDataChangeRequest request = findRequired(requestId);
        request.approve(approver);
        return persistencePort.save(request);
    }

    @Override
    public MasterDataChangeRequest reject(Long requestId, String approver, String reason) {
        MasterDataChangeRequest request = findRequired(requestId);
        request.reject(approver, reason);
        return persistencePort.save(request);
    }

    @Override
    public MasterDataChangeRequest markApplied(Long requestId) {
        MasterDataChangeRequest request = findRequired(requestId);
        request.markApplied();
        return persistencePort.save(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MasterDataChangeRequest> findPendingRequests() {
        return persistencePort.findByStatus(ChangeStatus.REQUESTED);
    }

    private MasterDataChangeRequest findRequired(Long requestId) {
        return persistencePort.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("마스터 변경요청을 찾을 수 없습니다. ID: " + requestId));
    }
}
