package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.command.MasterDataChangeRequestCommand;
import com.ho.account.masterdata.core.application.exception.UnsupportedMasterDataTypeException;
import com.ho.account.masterdata.core.application.port.in.MasterDataChangeRequestUseCase;
import com.ho.account.masterdata.core.application.port.out.MasterDataBusinessKeyLockPort;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangeRequestPersistencePort;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangeRequestPersistencePort.CreateOrLoadResult;
import com.ho.account.masterdata.core.application.port.out.MasterDataVersionQueryPort;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.application.query.PageSlice;
import com.ho.account.masterdata.core.application.query.PageWindow;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.domain.exception.MasterDataVersionConflictException;
import com.ho.account.masterdata.core.domain.policy.MasterDataChangeVersionPolicy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
    private final MasterDataDueChangeExecutor dueChangeExecutor;

    @Autowired
    public MasterDataChangeRequestService(
            MasterDataChangeRequestPersistencePort persistencePort,
            MasterDataVersionQueryPort versionQueryPort,
            List<MasterDataChangeApplier> appliers,
            MasterDataBusinessKeyLockPort businessKeyLockPort,
            MasterDataDueChangeExecutor dueChangeExecutor) {
        this.persistencePort = persistencePort;
        this.versionQueryPort = versionQueryPort;
        this.applierRegistry = new MasterDataChangeApplierRegistry(appliers);
        this.businessKeyLockPort = businessKeyLockPort;
        this.dueChangeExecutor = dueChangeExecutor;
    }

    /** Direct construction is retained for core unit tests; Spring injects the proxied executor above. */
    public MasterDataChangeRequestService(
            MasterDataChangeRequestPersistencePort persistencePort,
            MasterDataVersionQueryPort versionQueryPort,
            List<MasterDataChangeApplier> appliers,
            MasterDataBusinessKeyLockPort businessKeyLockPort) {
        this(persistencePort, versionQueryPort, appliers, businessKeyLockPort,
                new MasterDataDueChangeExecutor(persistencePort, versionQueryPort, appliers, businessKeyLockPort));
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

        MasterDataChangeRequest created = null;
        if (request.getSourceReference() != null) {
            // 먼저 식별자를 예약해 동시 재사용 충돌을 업무 버전 검증보다 앞서 판정합니다.
            CreateOrLoadResult result = persistencePort.createOrLoad(request);
            result.request().verifySameChange(request);
            if (!result.created()) {
                return result.request();
            }
            created = result.request();
        }

        // 구현되지 않은 전략은 접수 후 장기간 방치하지 않고 입구에서 바로 차단합니다.
        MasterDataChangeApplier applier = applierRegistry.require(request.getTargetType());
        // UPDATE 검증은 현재 버전을 읽으므로 업무 키 잠금 후에 실행해 종료와 경합해도 같은 상태를 봅니다.
        lockBusinessKey(request);
        applier.validate(request);
        verifyCurrentVersion(request);

        // 생성 예약은 같은 트랜잭션이므로 검증 실패 시 행도 함께 롤백됩니다.
        return created != null ? created : persistencePort.save(request);
    }

    @Override
    @Transactional(readOnly = true)
    public PageSlice<MasterDataChangeRequest> findPendingRequests(PageWindow page) {
        return persistencePort.findByStatus(ChangeStatus.REQUESTED, page);
    }

    @Override
    @Transactional
    public MasterDataChangeRequest approve(Long requestId, String approver) {
        MasterDataChangeRequest request = findAfterBusinessKeyLock(requestId);
        MasterDataChangeApplier applier = applierRegistry.require(request.getTargetType());
        // The persisted payload may predate current rules; approval must validate it before changing audit state.
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
        // 잠금 후 최신 상태가 이미 반영됐으면 원래 결과를 반환하고 이력과 appliedAt을 보존합니다.
        if (request.getStatus() == ChangeStatus.APPLIED) {
            return request;
        }
        return applyAndSave(request, LocalDate.now());
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<MasterDataChangeRequest> applyDueApprovedChanges() {
        LocalDate today = LocalDate.now();
        List<MasterDataChangeRequest> dueChanges = persistencePort.findReadyToApply(today, APPLY_CHUNK_SIZE);
        List<MasterDataChangeRequest> results = new ArrayList<>(dueChanges.size());

        // Selection is only a candidate snapshot. Each request rechecks status under its own
        // business-key then row lock, so concurrent workers never replay a completed request.
        for (MasterDataChangeRequest candidate : dueChanges) {
            try {
                dueChangeExecutor.applyDue(candidate.getId(), today).ifPresent(results::add);
            } catch (MasterDataVersionConflictException conflict) {
                // The failed apply transaction has rolled back. Only a deterministic version
                // conflict is quarantined; SQL/lock failures must remain eligible for retry.
                dueChangeExecutor.recordFailure(candidate.getId(), today,
                        ChangeStatus.FAILED_CONFLICT, "VERSION_CONFLICT").ifPresent(results::add);
            } catch (IllegalArgumentException invalid) {
                if (invalid instanceof UnsupportedMasterDataTypeException) {
                    // Missing strategy wiring is a deployment defect, not invalid request data.
                    throw invalid;
                }
                // Store a fixed label rather than exception text that may contain payload data.
                dueChangeExecutor.recordFailure(candidate.getId(), today,
                        ChangeStatus.FAILED_INVALID, "INVALID_REQUEST").ifPresent(results::add);
            }
        }
        return List.copyOf(results);
    }

    private MasterDataChangeRequest applyAndSave(MasterDataChangeRequest request, LocalDate today) {
        if (request.getStatus() == ChangeStatus.APPLIED) {
            throw new MasterDataVersionConflictException("Change request already applied. ID: " + request.getId());
        }
        if (!request.isReadyToApply(today)) {
            throw new IllegalStateException("Change request is not ready to apply. ID: " + request.getId());
        }

        MasterDataChangeApplier applier = applierRegistry.require(request.getTargetType());
        // 단건 반영 호출자가 잡은 업무 키 잠금은 applier와 APPLIED 저장까지 유지됩니다.
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
