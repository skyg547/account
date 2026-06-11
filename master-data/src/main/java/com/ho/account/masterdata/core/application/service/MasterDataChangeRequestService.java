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
        // @todo 승인 요청을 APPLIED로만 바꾸지 말고 targetType별 MasterDataChangeApplier를 호출해 실제 SCD2 도메인 반영까지 한 트랜잭션으로 묶는다.
        request.markApplied();
        return persistencePort.save(request);
    }

    @Override
    @Transactional
    public List<MasterDataChangeRequest> applyDueApprovedChanges() {
        LocalDate today = LocalDate.now();
        // @todo 대량 기준정보 변경 예약 반영은 findAll() 메모리 필터 대신 status/effectiveDate 조건 조회와 chunk 처리로 전환한다.
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
