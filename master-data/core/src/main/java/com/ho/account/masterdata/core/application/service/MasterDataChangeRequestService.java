package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.command.MasterDataChangeRequestCommand;
import com.ho.account.masterdata.core.application.port.in.MasterDataChangeRequestUseCase;
import com.ho.account.masterdata.core.application.port.out.MasterDataBusinessKeyLockPort;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangeRequestPersistencePort;
import com.ho.account.masterdata.core.application.port.out.MasterDataVersionQueryPort;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.domain.exception.MasterDataVersionConflictException;
import com.ho.account.masterdata.core.domain.policy.MasterDataChangeVersionPolicy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 마스터 데이터 변경 요청 서비스
 *
 * <p>작성자가 요청한 변경을 승인자와 실제 SCD2 반영 전략 사이에서 조정하는 애플리케이션
 * 서비스입니다. 요청 상태만 바꾸지 않고, 지원 전략과 업무 버전을 확인한 뒤 실제 반영이
 * 성공한 경우에만 {@code APPLIED}로 전환합니다.</p>
 *
 * <p>🐣 초보자 설명: 결재 문서에 적힌 대상, 순번, 시행일을 확인하고 실제 기준정보 담당자에게
 * 전달하는 결재 담당자입니다. 실제 데이터 수정 공식은 각 도메인 서비스가 담당합니다.</p>
 */
@Service
public class MasterDataChangeRequestService implements MasterDataChangeRequestUseCase {

    private static final int APPLY_CHUNK_SIZE = 500;

    private final MasterDataChangeRequestPersistencePort persistencePort;
    private final MasterDataVersionQueryPort versionQueryPort;
    private final MasterDataChangeApplierRegistry applierRegistry;
    private final MasterDataBusinessKeyLockPort businessKeyLockPort;

    public MasterDataChangeRequestService(
            MasterDataChangeRequestPersistencePort persistencePort,
            MasterDataVersionQueryPort versionQueryPort,
            List<MasterDataChangeApplier> appliers,
            MasterDataBusinessKeyLockPort businessKeyLockPort) {
        this.persistencePort = persistencePort;
        this.versionQueryPort = versionQueryPort;
        this.applierRegistry = new MasterDataChangeApplierRegistry(appliers);
        this.businessKeyLockPort = businessKeyLockPort;
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
                command.payloadJson(),
                command.sourceReference()
        );

        if (request.getSourceReference() != null) {
            MasterDataChangeRequest existing = persistencePort
                    .findBySourceReference(request.getSourceReference())
                    .orElse(null);
            if (existing != null) {
                existing.verifySameChange(request);
                return existing;
            }
        }

        // 구현되지 않은 전략은 접수 후 장기간 방치하지 않고 입구에서 바로 차단합니다.
        MasterDataChangeApplier applier = applierRegistry.require(request.getTargetType());
        applier.validate(request);
        lockBusinessKey(request);
        verifyCurrentVersion(request);

        // @todo 두 노드가 같은 sourceReference를 동시에 최초 저장하면 unique key 충돌이 날 수 있습니다.
        // 저장 포트가 충돌 후 기존 요청을 다시 읽어 동일 명령인지 검증하는 원자적 멱등 연산을 제공해야 합니다.
        return persistencePort.save(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MasterDataChangeRequest> findPendingRequests() {
        return persistencePort.findByStatus(ChangeStatus.REQUESTED);
    }

    @Override
    @Transactional
    public MasterDataChangeRequest approve(Long requestId, String approver) {
        MasterDataChangeRequest request = findAfterBusinessKeyLock(requestId);
        MasterDataChangeApplier applier = applierRegistry.require(request.getTargetType());
        applier.validate(request);
        verifyCurrentVersion(request);
        request.approve(approver);
        return persistencePort.save(request);
    }

    @Override
    @Transactional
    public MasterDataChangeRequest reject(Long requestId, String approver, String reason) {
        MasterDataChangeRequest request = findForDecision(requestId);
        request.reject(approver, reason);
        return persistencePort.save(request);
    }

    @Override
    @Transactional
    public MasterDataChangeRequest applyApprovedChange(Long requestId) {
        MasterDataChangeRequest request = findAfterBusinessKeyLock(requestId);
        return applyAndSave(request, LocalDate.now());
    }

    @Override
    @Transactional
    public List<MasterDataChangeRequest> applyDueApprovedChanges() {
        LocalDate today = LocalDate.now();
        List<MasterDataChangeRequest> dueChanges = persistencePort.findReadyToApply(today, APPLY_CHUNK_SIZE);
        List<MasterDataChangeRequest> applied = new ArrayList<>(dueChanges.size());

        // 전체 chunk가 한 트랜잭션이므로 모든 업무 키를 먼저 같은 순서로 잠급니다.
        // 요청 행을 먼저 잡으면 단건 반영이나 다른 chunk와 서로의 키/요청 행을 기다릴 수 있습니다.
        dueChanges.stream()
                .map(request -> new BusinessKey(request.getTargetType(), request.getTargetKey()))
                .distinct()
                .sorted(Comparator.comparing((BusinessKey key) -> key.type().name())
                        .thenComparing(BusinessKey::key))
                .forEach(key -> businessKeyLockPort.lock(key.type(), key.key()));

        // 부수효과와 실패 순서가 중요한 승인 반영이므로 stream보다 순서가 드러나는 반복을 사용합니다.
        for (MasterDataChangeRequest candidate : dueChanges) {
            MasterDataChangeRequest locked = findForDecision(candidate.getId());
            if (locked.isReadyToApply(today)) {
                applied.add(applyAndSave(locked, today));
            }
        }

        // @todo 운영 대량 반영은 요청별 REQUIRES_NEW 트랜잭션과 DB SKIP LOCKED 파티셔닝으로
        // 분리해 한 건의 실패가 같은 chunk 전체를 롤백하지 않도록 고도화해야 한다.
        return List.copyOf(applied);
    }

    private MasterDataChangeRequest applyAndSave(MasterDataChangeRequest request, LocalDate today) {
        if (request.getStatus() == ChangeStatus.APPLIED) {
            throw new MasterDataVersionConflictException("Change request already applied. ID: " + request.getId());
        }
        if (!request.isReadyToApply(today)) {
            throw new IllegalStateException("Change request is not ready to apply. ID: " + request.getId());
        }

        MasterDataChangeApplier applier = applierRegistry.require(request.getTargetType());
        // 단건/일괄 호출자가 잡은 업무 키 잠금은 applier와 APPLIED 저장까지 유지됩니다.
        verifyCurrentVersion(request);
        applier.apply(request);
        request.markApplied();
        return persistencePort.save(request);
    }

    private MasterDataChangeRequest findAfterBusinessKeyLock(Long requestId) {
        if (requestId == null) {
            throw new IllegalArgumentException("Change request ID is required.");
        }
        MasterDataChangeRequest identity = persistencePort.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Change request not found. ID: " + requestId));
        lockBusinessKey(identity);
        // 처음 읽은 상태를 사용하지 않습니다. 잠금 대기 중 승인/반영된 최신 요청을 다시 읽습니다.
        return findForDecision(requestId);
    }

    private void lockBusinessKey(MasterDataChangeRequest request) {
        businessKeyLockPort.lock(request.getTargetType(), request.getTargetKey());
    }

    private record BusinessKey(MasterDataChangeRequest.MasterDataType type, String key) { }

    private MasterDataChangeRequest findForDecision(Long requestId) {
        if (requestId == null) {
            throw new IllegalArgumentException("Change request ID is required.");
        }
        return persistencePort.findByIdForUpdate(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Change request not found. ID: " + requestId));
    }

    private void verifyCurrentVersion(MasterDataChangeRequest request) {
        long persistedVersionCount = versionQueryPort.countPersistedVersions(
                request.getTargetType(),
                request.getTargetKey());
        MasterDataChangeVersionPolicy.verify(
                request.getChangeType(),
                request.getRequestedVersion(),
                persistedVersionCount);
    }
}
