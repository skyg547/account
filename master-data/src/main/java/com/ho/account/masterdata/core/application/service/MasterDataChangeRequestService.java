package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.port.in.MasterDataChangeRequestUseCase;
import com.ho.account.masterdata.core.application.command.MasterDataChangeRequestCommand;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangeRequestPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 마스터 데이터 변경 요청 서비스
 */
@Service
@RequiredArgsConstructor
public class MasterDataChangeRequestService implements MasterDataChangeRequestUseCase {

    private static final int APPLY_CHUNK_SIZE = 500;

    private final MasterDataChangeRequestPersistencePort persistencePort;
    private final List<MasterDataChangeApplier> appliers;

    @Override
    @Transactional(readOnly = true)
    public List<MasterDataChangeRequest> getAllChangeRequests() {
        return persistencePort.findAll();
    }

    @Override
    @Transactional
    public MasterDataChangeRequest requestChange(MasterDataChangeRequestCommand command) {
        // @todo requestedVersion을 targetType/targetKey별 현재 버전과 비교해 오래된 변경 요청이
        // 최신 SCD2 버전을 덮어쓰지 못하도록 낙관적 충돌 검사를 추가해야 한다.
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
        return applyAndSave(request);
    }

    @Override
    @Transactional
    public List<MasterDataChangeRequest> applyDueApprovedChanges() {
        LocalDate today = LocalDate.now();
        List<MasterDataChangeRequest> dueChanges = persistencePort.findReadyToApply(today, APPLY_CHUNK_SIZE);
        List<MasterDataChangeRequest> applied = new ArrayList<>();
        for (MasterDataChangeRequest request : dueChanges) {
            applied.add(applyAndSave(request));
        }
        return applied;
    }

    private MasterDataChangeRequest applyAndSave(MasterDataChangeRequest request) {
        if (!request.isReadyToApply(LocalDate.now())) {
            throw new IllegalStateException("Change request is not ready to apply. ID: " + request.getId());
        }
        MasterDataChangeApplier applier = appliers.stream()
                .filter(candidate -> candidate.supports(request.getTargetType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No MasterDataChangeApplier supports targetType: " + request.getTargetType()));
        applier.apply(request);
        request.markApplied();
        return persistencePort.save(request);
    }
}
