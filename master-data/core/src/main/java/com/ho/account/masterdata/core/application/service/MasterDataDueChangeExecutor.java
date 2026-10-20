package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.port.out.MasterDataAuditPort;
import com.ho.account.masterdata.core.application.port.out.MasterDataBusinessKeyLockPort;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangeRequestPersistencePort;
import com.ho.account.masterdata.core.application.port.out.MasterDataVersionQueryPort;
import com.ho.account.masterdata.core.domain.audit.MasterDataAuditEvent;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.domain.policy.MasterDataChangeVersionPolicy;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies one due request per transaction. The coordinator calls this separate Spring bean so a
 * failed applier rolls back its business writes before the failure record is committed.
 */
@Service
public class MasterDataDueChangeExecutor {

    private final MasterDataChangeRequestPersistencePort persistencePort;
    private final MasterDataVersionQueryPort versionQueryPort;
    private final MasterDataChangeApplierRegistry applierRegistry;
    private final MasterDataBusinessKeyLockPort businessKeyLockPort;
    private final MasterDataAuditPort auditPort;

    @Autowired
    public MasterDataDueChangeExecutor(
            MasterDataChangeRequestPersistencePort persistencePort,
            MasterDataVersionQueryPort versionQueryPort,
            List<MasterDataChangeApplier> appliers,
            MasterDataBusinessKeyLockPort businessKeyLockPort,
            ObjectProvider<MasterDataAuditPort> auditPortProvider) {
        this(persistencePort, versionQueryPort, appliers, businessKeyLockPort,
                auditPortProvider.getIfAvailable(() -> event -> { }));
    }

    public MasterDataDueChangeExecutor(
            MasterDataChangeRequestPersistencePort persistencePort,
            MasterDataVersionQueryPort versionQueryPort,
            List<MasterDataChangeApplier> appliers,
            MasterDataBusinessKeyLockPort businessKeyLockPort) {
        this(persistencePort, versionQueryPort, appliers, businessKeyLockPort, event -> { });
    }

    public MasterDataDueChangeExecutor(
            MasterDataChangeRequestPersistencePort persistencePort,
            MasterDataVersionQueryPort versionQueryPort,
            List<MasterDataChangeApplier> appliers,
            MasterDataBusinessKeyLockPort businessKeyLockPort,
            MasterDataAuditPort auditPort) {
        this.persistencePort = persistencePort;
        this.versionQueryPort = versionQueryPort;
        this.applierRegistry = new MasterDataChangeApplierRegistry(appliers);
        this.businessKeyLockPort = businessKeyLockPort;
        this.auditPort = auditPort != null ? auditPort : event -> { };
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<MasterDataChangeRequest> applyDue(Long requestId, LocalDate today) {
        Optional<MasterDataChangeRequest> locked = findAfterBusinessKeyLock(requestId);
        if (locked.isEmpty() || !locked.get().isReadyToApply(today)) {
            return Optional.empty();
        }

        MasterDataChangeRequest request = locked.get();
        MasterDataChangeApplier applier = applierRegistry.require(request.getTargetType());
        // A persisted approval can become invalid before its effective date. Validate it again
        // while the business key and request row are both locked.
        applier.validate(request);
        MasterDataChangeVersionPolicy.verify(request.getChangeType(), request.getRequestedVersion(),
                versionQueryPort.countPersistedVersions(request.getTargetType(), request.getTargetKey()));
        MasterDataActorContext.Actor ingress = MasterDataActorContext.current();
        String applyingService = ingress == null ? "master-data-scheduler" : ingress.serviceActor();
        String applyingActor = ingress == null ? request.getApprovedBy() : ingress.businessActor();
        MasterDataChangeApplier.AppliedVersion version;
        try (MasterDataActorContext.Scope ignored = MasterDataActorContext.enterApprovedChange(
                null, applyingActor)) {
            version = applier.apply(request);
        }
        request.markApplied();
        MasterDataChangeRequest saved = persistencePort.save(request);
        if (version != null && version.afterId() != null) {
            auditPort.append(new MasterDataAuditEvent("APPLIED", Instant.now(), saved.getTargetType(),
                    saved.getTargetKey(), saved.getId(), saved.getSourceReference(),
                    applyingService, applyingActor,
                    MasterDataAuditRecorder.reasonReference("APPLIED", saved.getReason()),
                    version.beforeId(), version.afterId()));
        }
        return Optional.of(saved);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<MasterDataChangeRequest> recordFailure(
            Long requestId, LocalDate today, ChangeStatus failureStatus, String safeReason) {
        Optional<MasterDataChangeRequest> locked = findAfterBusinessKeyLock(requestId);
        if (locked.isEmpty() || !locked.get().isReadyToApply(today)) {
            return Optional.empty();
        }

        MasterDataChangeRequest request = locked.get();
        request.markFailed(failureStatus, safeReason);
        return Optional.of(persistencePort.save(request));
    }

    private Optional<MasterDataChangeRequest> findAfterBusinessKeyLock(Long requestId) {
        if (requestId == null) {
            throw new IllegalArgumentException("Change request ID is required.");
        }
        Optional<MasterDataChangeRequest> identity = persistencePort.findById(requestId);
        if (identity.isEmpty()) {
            return Optional.empty();
        }
        businessKeyLockPort.lock(identity.get().getTargetType(), identity.get().getTargetKey());
        // A competing worker may have applied or quarantined this row while the key lock waited.
        return persistencePort.findByIdForUpdate(requestId);
    }
}
