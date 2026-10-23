package com.ho.account.masterdata.core.infrastructure.adapter;

import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import com.ho.account.contracts.masterdata.FiscalPeriodClosingStatus;
import com.ho.account.contracts.masterdata.FiscalPeriodTransitionCommand;
import com.ho.account.contracts.masterdata.FiscalPeriodTransitionReceipt;
import com.ho.account.masterdata.core.application.port.out.FiscalPeriodPersistencePort;
import com.ho.account.masterdata.core.application.port.out.MasterDataAuditPort;
import com.ho.account.masterdata.core.application.service.MasterDataActorContext;
import com.ho.account.masterdata.core.domain.audit.MasterDataAuditEvent;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import com.ho.account.masterdata.core.domain.exception.FiscalPeriodTransitionConflictException;
import com.ho.account.masterdata.core.domain.exception.FiscalPeriodTransitionNotFoundException;
import com.ho.account.masterdata.core.infrastructure.persistence.entity.FiscalPeriodTransitionEntity;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.FiscalPeriodTransitionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Locale;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MonolithFiscalPeriodControlAdapter implements FiscalPeriodControlPort {

    private final FiscalPeriodPersistencePort fiscalPeriodPersistencePort;
    private final MasterDataAuditPort auditPort;
    private final FiscalPeriodTransitionRepository transitionRepository;

    @Autowired
    public MonolithFiscalPeriodControlAdapter(FiscalPeriodPersistencePort fiscalPeriodPersistencePort,
            MasterDataAuditPort auditPort, FiscalPeriodTransitionRepository transitionRepository) {
        this.fiscalPeriodPersistencePort = fiscalPeriodPersistencePort;
        this.auditPort = auditPort;
        this.transitionRepository = transitionRepository;
    }

    /** Retained for isolated legacy adapter tests; production wiring uses the durable repository. */
    public MonolithFiscalPeriodControlAdapter(FiscalPeriodPersistencePort fiscalPeriodPersistencePort,
            MasterDataAuditPort auditPort) {
        this(fiscalPeriodPersistencePort, auditPort, null);
    }

    @Override
    public Optional<FiscalPeriodRef> findFiscalPeriodById(Long id) {
        return transitionRepository == null
                ? fiscalPeriodPersistencePort.findById(id).map(this::toRef)
                : transitionRepository.findAdmissionById(id).map(this::toAdmissionRef);
    }

    @Override
    public Optional<FiscalPeriodRef> findFiscalPeriod(String fiscalYear, String fiscalPeriod) {
        return transitionRepository == null
                ? fiscalPeriodPersistencePort.findByFiscalYearAndFiscalPeriod(fiscalYear, fiscalPeriod)
                        .map(this::toRef)
                : transitionRepository.findAdmissionByPeriodKey(fiscalYear, fiscalPeriod)
                        .map(this::toAdmissionRef);
    }

    @Override
    @Transactional
    public FiscalPeriodRef updateClosingStatus(Long fiscalPeriodId, String closingStatus, String auditUser) {
        FiscalPeriod fiscalPeriod = fiscalPeriodPersistencePort.findByIdForUpdate(fiscalPeriodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));
        if (transitionRepository != null && transitionRepository.existsByFiscalPeriodId(fiscalPeriodId)) {
            throw new FiscalPeriodTransitionConflictException("Fiscal period with operation history requires transition protocol");
        }
        FiscalPeriod.ClosingStatus beforeStatus = fiscalPeriod.getClosingStatus();
        fiscalPeriod.changeClosingStatus(parseStatus(closingStatus), auditUser);
        FiscalPeriod saved = fiscalPeriodPersistencePort.save(fiscalPeriod);
        MasterDataActorContext.Actor actor = MasterDataActorContext.current();
        // A signed Closing assertion supplies the separate service/business identities at ingress.
        auditPort.append(new MasterDataAuditEvent("FISCAL_PERIOD_STATUS", Instant.now(),
                MasterDataType.FISCAL_PERIOD, Long.toString(fiscalPeriodId), null, null,
                actor == null ? null : actor.serviceActor(),
                actor == null ? auditUser : actor.businessActor(),
                beforeStatus + " -> " + saved.getClosingStatus(), fiscalPeriodId, saved.getId()));
        return toRef(saved);
    }

    @Override
    @Transactional
    public FiscalPeriodTransitionReceipt prepareClosingTransition(FiscalPeriodTransitionCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Fiscal transition command is required");
        }
        // A row lock serializes all prepared operations for the same period with status changes.
        FiscalPeriod period = fiscalPeriodPersistencePort.findByIdForUpdate(command.fiscalPeriodId())
                .orElseThrow(() -> new FiscalPeriodTransitionNotFoundException());
        Optional<FiscalPeriodTransitionEntity> existing = transitionRepository.findById(command.operationId());
        if (existing.isPresent()) {
            FiscalPeriodTransitionReceipt receipt = toReceipt(existing.get());
            if (!receipt.matches(command)) {
                throw new FiscalPeriodTransitionConflictException("Fiscal transition operation ID belongs to a different command");
            }
            return receipt;
        }
        // Without this fence an old PREPARED CLOSED→OPEN could pass an ABA status cycle.
        rejectWhilePrepared(command.fiscalPeriodId());
        if (transitionRepository.findFirstByFiscalPeriodIdAndPhaseAndTargetStatus(
                command.fiscalPeriodId(), FiscalPeriodTransitionEntity.Phase.APPLIED, "OPEN").isPresent()) {
            throw new FiscalPeriodTransitionConflictException("Unconfirmed reopen blocks another fiscal transition");
        }
        if (!period.getClosingStatus().name().equals(command.expectedStatus().name())) {
            throw new FiscalPeriodTransitionConflictException("Fiscal transition expected state does not match current state");
        }
        // Validate the domain transition before any receipt can be committed.
        FiscalPeriod candidate = FiscalPeriod.reconstitute(period.getId(), period.getFiscalYear(),
                period.getFiscalPeriod(), period.getStartDate(), period.getEndDate(),
                period.getClosingStatus(), period.getCreatedAt(), period.getUpdatedAt(), period.getAuditUser());
        try {
            candidate.changeClosingStatus(FiscalPeriod.ClosingStatus.valueOf(command.targetStatus().name()),
                    "closing:" + command.auditUser());
        } catch (IllegalStateException invalidTransition) {
            throw new FiscalPeriodTransitionConflictException("Fiscal transition violates period state rules");
        }
        FiscalPeriodTransitionEntity receipt = new FiscalPeriodTransitionEntity();
        receipt.setOperationId(command.operationId());
        receipt.setFiscalPeriodId(command.fiscalPeriodId());
        receipt.setExpectedStatus(command.expectedStatus().name());
        receipt.setTargetStatus(command.targetStatus().name());
        receipt.setAuditUser(command.auditUser());
        receipt.setPhase(FiscalPeriodTransitionEntity.Phase.PREPARED);
        receipt.setCreatedAt(Instant.now());
        return toReceipt(transitionRepository.saveAndFlush(receipt));
    }

    @Override
    @Transactional
    public FiscalPeriodTransitionReceipt applyClosingTransition(String operationId) {
        Long fiscalPeriodId = transitionRepository.findPeriodIdByOperationId(operationId)
                .orElseThrow(() -> new FiscalPeriodTransitionNotFoundException());
        FiscalPeriod period = fiscalPeriodPersistencePort.findByIdForUpdate(fiscalPeriodId)
                .orElseThrow(() -> new FiscalPeriodTransitionNotFoundException());
        FiscalPeriodTransitionEntity receipt = transitionRepository.findById(operationId).orElseThrow();
        if (receipt.getPhase() == FiscalPeriodTransitionEntity.Phase.APPLIED
                || receipt.getPhase() == FiscalPeriodTransitionEntity.Phase.CONFIRMED) {
            return toReceipt(receipt);
        }
        if (receipt.getPhase() == FiscalPeriodTransitionEntity.Phase.CANCELLED) {
            throw new FiscalPeriodTransitionConflictException("Cancelled fiscal transition cannot be applied");
        }
        if (!period.getClosingStatus().name().equals(receipt.getExpectedStatus())) {
            throw new FiscalPeriodTransitionConflictException("Fiscal transition expected state does not match current state");
        }
        FiscalPeriod.ClosingStatus before = period.getClosingStatus();
        period.changeClosingStatus(FiscalPeriod.ClosingStatus.valueOf(receipt.getTargetStatus()),
                "closing:" + receipt.getAuditUser());
        FiscalPeriod saved = fiscalPeriodPersistencePort.save(period);
        MasterDataActorContext.Actor actor = MasterDataActorContext.current();
        auditPort.append(new MasterDataAuditEvent("FISCAL_PERIOD_STATUS", Instant.now(),
                MasterDataType.FISCAL_PERIOD, Long.toString(saved.getId()), null, null,
                actor == null ? "closing" : actor.serviceActor(),
                receipt.getAuditUser(),
                before + " -> " + saved.getClosingStatus() + " [" + operationId + "]",
                saved.getId(), saved.getId()));
        receipt.setPhase(FiscalPeriodTransitionEntity.Phase.APPLIED);
        receipt.setAppliedAt(Instant.now());
        return toReceipt(transitionRepository.save(receipt));
    }

    @Override
    @Transactional
    public FiscalPeriodTransitionReceipt cancelClosingTransition(String operationId, String cancellationActor) {
        if (cancellationActor == null || cancellationActor.length() > 42
                || !cancellationActor.matches("[A-Za-z0-9._@-]+")) {
            throw new IllegalArgumentException("Verified cancellation actor is required");
        }
        Long periodId = transitionRepository.findPeriodIdByOperationId(operationId)
                .orElseThrow(() -> new FiscalPeriodTransitionNotFoundException());
        // The same period lock as apply gives cancellation a single winner in an apply/cancel race.
        fiscalPeriodPersistencePort.findByIdForUpdate(periodId)
                .orElseThrow(() -> new FiscalPeriodTransitionNotFoundException());
        FiscalPeriodTransitionEntity receipt = transitionRepository.findById(operationId).orElseThrow();
        if (receipt.getPhase() == FiscalPeriodTransitionEntity.Phase.CANCELLED) {
            return toReceipt(receipt);
        }
        if (receipt.getPhase() == FiscalPeriodTransitionEntity.Phase.APPLIED
                || receipt.getPhase() == FiscalPeriodTransitionEntity.Phase.CONFIRMED) {
            throw new FiscalPeriodTransitionConflictException("Applied fiscal transition cannot be cancelled");
        }
        MasterDataActorContext.Actor actor = MasterDataActorContext.current();
        auditPort.append(new MasterDataAuditEvent("FISCAL_PERIOD_TRANSITION_CANCELLED", Instant.now(),
                MasterDataType.FISCAL_PERIOD, Long.toString(periodId), null, null,
                actor == null ? "closing" : actor.serviceActor(),
                cancellationActor,
                "Cancelled prepared fiscal transition [" + operationId + "]", periodId, periodId));
        receipt.setPhase(FiscalPeriodTransitionEntity.Phase.CANCELLED);
        receipt.setCancelledAt(Instant.now());
        return toReceipt(transitionRepository.save(receipt));
    }

    @Override
    @Transactional
    public FiscalPeriodTransitionReceipt confirmClosingTransition(String operationId) {
        Long periodId = transitionRepository.findPeriodIdByOperationId(operationId)
                .orElseThrow(() -> new FiscalPeriodTransitionNotFoundException());
        FiscalPeriod period = fiscalPeriodPersistencePort.findByIdForUpdate(periodId)
                .orElseThrow(() -> new FiscalPeriodTransitionNotFoundException());
        FiscalPeriodTransitionEntity receipt = transitionRepository.findById(operationId).orElseThrow();
        if (receipt.getPhase() == FiscalPeriodTransitionEntity.Phase.CONFIRMED) {
            return toReceipt(receipt);
        }
        if (receipt.getPhase() != FiscalPeriodTransitionEntity.Phase.APPLIED) {
            throw new FiscalPeriodTransitionConflictException("Only an applied fiscal transition can be confirmed");
        }
        if (!"CLOSED".equals(receipt.getExpectedStatus()) || !"OPEN".equals(receipt.getTargetStatus())) {
            throw new FiscalPeriodTransitionConflictException("Only an applied reopen can be confirmed");
        }
        if (!period.getClosingStatus().name().equals(receipt.getTargetStatus())) {
            throw new FiscalPeriodTransitionConflictException("Fiscal transition target no longer matches current state");
        }
        MasterDataActorContext.Actor actor = MasterDataActorContext.current();
        auditPort.append(new MasterDataAuditEvent("FISCAL_PERIOD_TRANSITION_CONFIRMED", Instant.now(),
                MasterDataType.FISCAL_PERIOD, Long.toString(periodId), null, null,
                actor == null ? "closing" : actor.serviceActor(), receipt.getAuditUser(),
                "Confirmed fiscal transition [" + operationId + "]", periodId, periodId));
        receipt.setPhase(FiscalPeriodTransitionEntity.Phase.CONFIRMED);
        receipt.setConfirmedAt(Instant.now());
        return toReceipt(transitionRepository.save(receipt));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FiscalPeriodTransitionReceipt> findTransitionReceipt(String operationId) {
        return transitionRepository.findById(operationId).map(this::toReceipt);
    }

    private FiscalPeriodTransitionReceipt toReceipt(FiscalPeriodTransitionEntity entity) {
        return new FiscalPeriodTransitionReceipt(entity.getOperationId(), entity.getFiscalPeriodId(),
                FiscalPeriodClosingStatus.valueOf(entity.getExpectedStatus()),
                FiscalPeriodClosingStatus.valueOf(entity.getTargetStatus()), entity.getAuditUser(),
                FiscalPeriodTransitionReceipt.Phase.valueOf(entity.getPhase().name()));
    }

    private void rejectWhilePrepared(Long fiscalPeriodId) {
        if (transitionRepository != null && transitionRepository.findFirstByFiscalPeriodIdAndPhase(
                fiscalPeriodId, FiscalPeriodTransitionEntity.Phase.PREPARED).isPresent()) {
            throw new FiscalPeriodTransitionConflictException("Fiscal period already has a prepared transition");
        }
    }

    private FiscalPeriod.ClosingStatus parseStatus(String closingStatus) {
        if (closingStatus == null || closingStatus.isBlank()) {
            throw new IllegalArgumentException("Closing status is required.");
        }
        return FiscalPeriod.ClosingStatus.valueOf(closingStatus.trim().toUpperCase(Locale.ROOT));
    }

    private FiscalPeriodRef toRef(FiscalPeriod fiscalPeriod) {
        return new FiscalPeriodRef(
                fiscalPeriod.getId(),
                fiscalPeriod.getFiscalYear(),
                fiscalPeriod.getFiscalPeriod(),
                fiscalPeriod.getStartDate(),
                fiscalPeriod.getEndDate(),
                fiscalPeriod.getClosingStatus() != null ? fiscalPeriod.getClosingStatus().name() : null);
    }

    private FiscalPeriodRef toAdmissionRef(FiscalPeriodTransitionRepository.AdmissionView view) {
        return new FiscalPeriodRef(view.getId(), view.getFiscalYear(), view.getFiscalPeriod(),
                view.getStartDate(), view.getEndDate(), view.getClosingStatus());
    }
}
