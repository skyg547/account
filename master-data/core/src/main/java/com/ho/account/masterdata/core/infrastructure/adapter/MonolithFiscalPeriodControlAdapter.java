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
import com.ho.account.masterdata.core.application.service.FiscalPeriodTransitionService;
import com.ho.account.masterdata.core.domain.model.FiscalPeriodTransition;
import com.ho.account.masterdata.core.application.port.out.FiscalPeriodTransitionPersistencePort;
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
    private final FiscalPeriodTransitionPersistencePort transitions;
    private final FiscalPeriodTransitionService transitionService;

    @Autowired
    public MonolithFiscalPeriodControlAdapter(FiscalPeriodPersistencePort fiscalPeriodPersistencePort,
            MasterDataAuditPort auditPort, FiscalPeriodTransitionPersistencePort transitions,
            FiscalPeriodTransitionService transitionService) {
        this.fiscalPeriodPersistencePort = fiscalPeriodPersistencePort;
        this.auditPort = auditPort;
        this.transitions = transitions;
        this.transitionService = transitionService;
    }

    /** Retained for isolated legacy adapter tests; production wiring uses the durable receipt port. */
    public MonolithFiscalPeriodControlAdapter(FiscalPeriodPersistencePort fiscalPeriodPersistencePort,
            MasterDataAuditPort auditPort) {
        this(fiscalPeriodPersistencePort, auditPort, null, null);
    }

    @Override
    public Optional<FiscalPeriodRef> findFiscalPeriodById(Long id) {
        return transitions == null
                ? fiscalPeriodPersistencePort.findById(id).map(this::toRef)
                : transitions.findAdmissionById(id).map(this::toAdmissionRef);
    }

    @Override
    public Optional<FiscalPeriodRef> findFiscalPeriod(String fiscalYear, String fiscalPeriod) {
        return transitions == null
                ? fiscalPeriodPersistencePort.findByFiscalYearAndFiscalPeriod(fiscalYear, fiscalPeriod)
                        .map(this::toRef)
                : transitions.findAdmissionByPeriodKey(fiscalYear, fiscalPeriod)
                        .map(this::toAdmissionRef);
    }

    @Override
    @Transactional
    public FiscalPeriodRef updateClosingStatus(Long fiscalPeriodId, String closingStatus, String auditUser) {
        FiscalPeriod fiscalPeriod = fiscalPeriodPersistencePort.findByIdForUpdate(fiscalPeriodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));
        if (transitions != null && transitions.existsByFiscalPeriodId(fiscalPeriodId)) {
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
    public FiscalPeriodTransitionReceipt prepareClosingTransition(FiscalPeriodTransitionCommand command) {
        return toReceipt(transitionService.prepare(command));
    }

    @Override
    public FiscalPeriodTransitionReceipt applyClosingTransition(String operationId) {
        return toReceipt(transitionService.apply(operationId));
    }

    @Override
    public FiscalPeriodTransitionReceipt cancelClosingTransition(String operationId, String actor) {
        return toReceipt(transitionService.cancel(operationId, actor));
    }

    @Override
    public FiscalPeriodTransitionReceipt confirmClosingTransition(String operationId) {
        return toReceipt(transitionService.confirm(operationId));
    }

    @Override
    public Optional<FiscalPeriodTransitionReceipt> findTransitionReceipt(String operationId) {
        return transitionService.find(operationId).map(this::toReceipt);
    }

    private FiscalPeriodTransitionReceipt toReceipt(FiscalPeriodTransition receipt) {
        return new FiscalPeriodTransitionReceipt(receipt.operationId(), receipt.fiscalPeriodId(),
                FiscalPeriodClosingStatus.valueOf(receipt.expectedStatus().name()),
                FiscalPeriodClosingStatus.valueOf(receipt.targetStatus().name()), receipt.auditUser(),
                FiscalPeriodTransitionReceipt.Phase.valueOf(receipt.phase().name()));
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

    private FiscalPeriodRef toAdmissionRef(FiscalPeriodTransitionPersistencePort.Admission view) {
        return new FiscalPeriodRef(view.id(), view.fiscalYear(), view.fiscalPeriod(),
                view.startDate(), view.endDate(), view.closingStatus());
    }
}
