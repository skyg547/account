package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.port.in.MasterDataChangeRequestUseCase;
import com.ho.account.masterdata.core.application.command.MasterDataChangeRequestCommand;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangeRequestPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 마스터 데이터 변경 요청 서비스
 */
@Service
@RequiredArgsConstructor
public class MasterDataChangeRequestService implements MasterDataChangeRequestUseCase {

    private final MasterDataChangeRequestPersistencePort persistencePort;

    @Override
    @Transactional(readOnly = true)
    public List<MasterDataChangeRequest> getAllChangeRequests() {
        return persistencePort.findAll();
    }

    @Override
    @Transactional
    public MasterDataChangeRequest requestChange(MasterDataChangeRequestCommand command) {
        MasterDataChangeRequest request = new MasterDataChangeRequest(
                command.targetType(),
                command.targetKey(),
                command.changeType(),
                command.effectiveDate(),
                command.requestedVersion(),
                command.requestedBy(),
                command.reason(),
                command.payloadJson()
        );
        return persistencePort.save(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MasterDataChangeRequest> findPendingRequests() {
        return persistencePort.findByStatus(MasterDataChangeRequest.ChangeStatus.REQUESTED);
    }

    @Override
    @Transactional
    public MasterDataChangeRequest approve(Long requestId, String approver) {
        MasterDataChangeRequest request = persistencePort.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Change request not found. ID: " + requestId));
        request.approve(approver);
        return persistencePort.save(request);
    }

    @Override
    @Transactional
    public MasterDataChangeRequest reject(Long requestId, String approver, String reason) {
        MasterDataChangeRequest request = persistencePort.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Change request not found. ID: " + requestId));
        request.reject(approver, reason);
        return persistencePort.save(request);
    }

    @Override
    @Transactional
    public MasterDataChangeRequest applyApprovedChange(Long requestId) {
        MasterDataChangeRequest request = persistencePort.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Change request not found. ID: " + requestId));
        request.markApplied();
        return persistencePort.save(request);
    }

    @Override
    @Transactional
    public List<MasterDataChangeRequest> applyDueApprovedChanges() {
        LocalDate today = LocalDate.now();
        List<MasterDataChangeRequest> dueChanges = persistencePort.findAll().stream()
                .filter(r -> r.isReadyToApply(today))
                .collect(Collectors.toList());
        
        dueChanges.forEach(r -> {
            r.markApplied();
            persistencePort.save(r);
        });
        return dueChanges;
    }
}
