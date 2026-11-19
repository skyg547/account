package com.ho.account.masterdata.core.application.service;

import com.ho.account.contracts.masterdata.FiscalPeriodTransitionCommand;
import com.ho.account.masterdata.core.application.port.out.FiscalPeriodPersistencePort;
import com.ho.account.masterdata.core.application.port.out.FiscalPeriodTransitionPersistencePort;
import com.ho.account.masterdata.core.application.port.out.MasterDataAuditPort;
import com.ho.account.masterdata.core.domain.audit.MasterDataAuditEvent;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.exception.FiscalPeriodTransitionConflictException;
import com.ho.account.masterdata.core.domain.exception.FiscalPeriodTransitionNotFoundException;
import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import com.ho.account.masterdata.core.domain.model.FiscalPeriodTransition;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Locks the period and atomically coordinates domain state, audit and durable receipt writes. */
@Service
@Transactional
public class FiscalPeriodTransitionService {
    private final FiscalPeriodPersistencePort periods;
    private final FiscalPeriodTransitionPersistencePort receipts;
    private final MasterDataAuditPort audits;

    public FiscalPeriodTransitionService(FiscalPeriodPersistencePort periods,
            FiscalPeriodTransitionPersistencePort receipts, MasterDataAuditPort audits) {
        this.periods = periods;
        this.receipts = receipts;
        this.audits = audits;
    }

    public FiscalPeriodTransition prepare(FiscalPeriodTransitionCommand command) {
        if (command == null) throw new IllegalArgumentException("Fiscal transition command is required");
        FiscalPeriod period = lock(command.fiscalPeriodId());
        var expected = FiscalPeriod.ClosingStatus.valueOf(command.expectedStatus().name());
        var target = FiscalPeriod.ClosingStatus.valueOf(command.targetStatus().name());
        Optional<FiscalPeriodTransition> existing = receipts.findByOperationId(command.operationId());
        if (existing.isPresent()) {
            existing.get().requireCommand(command.operationId(), command.fiscalPeriodId(), expected,
                    target, command.auditUser());
            return existing.get();
        }
        // Holding the period lock prevents same-period ABA and makes identical retries converge.
        if (receipts.hasPrepared(period.getId())) {
            throw new FiscalPeriodTransitionConflictException("Fiscal period already has a prepared transition");
        }
        if (receipts.hasUnconfirmedReopen(period.getId())) {
            throw new FiscalPeriodTransitionConflictException("Unconfirmed reopen blocks another fiscal transition");
        }
        return receipts.insert(FiscalPeriodTransition.prepare(command.operationId(), period, expected,
                target, command.auditUser(), Instant.now()));
    }

    public FiscalPeriodTransition apply(String operationId) {
        FiscalPeriod period = lockOperation(operationId);
        FiscalPeriodTransition before = requireReceipt(operationId);
        String priorStatus = period.getClosingStatus().name();
        FiscalPeriodTransition after = before.apply(period, Instant.now());
        if (after == before) return before;
        periods.save(period);
        audit("FISCAL_PERIOD_STATUS", after, after.auditUser(),
                priorStatus + " -> " + period.getClosingStatus() + " [" + operationId + "]");
        receipts.saveLifecycle(after);
        return after;
    }

    public FiscalPeriodTransition cancel(String operationId, String actor) {
        lockOperation(operationId);
        FiscalPeriodTransition before = requireReceipt(operationId);
        FiscalPeriodTransition after = before.cancel(actor, Instant.now());
        if (after == before) return before;
        audit("FISCAL_PERIOD_TRANSITION_CANCELLED", after, actor,
                "Cancelled prepared fiscal transition [" + operationId + "]");
        receipts.saveLifecycle(after);
        return after;
    }

    public FiscalPeriodTransition confirm(String operationId) {
        FiscalPeriod period = lockOperation(operationId);
        FiscalPeriodTransition before = requireReceipt(operationId);
        FiscalPeriodTransition after = before.confirm(period, Instant.now());
        if (after == before) return before;
        audit("FISCAL_PERIOD_TRANSITION_CONFIRMED", after, after.auditUser(),
                "Confirmed fiscal transition [" + operationId + "]");
        receipts.saveLifecycle(after);
        return after;
    }

    @Transactional(readOnly = true)
    public Optional<FiscalPeriodTransition> find(String operationId) {
        return receipts.findByOperationId(operationId);
    }

    private FiscalPeriod lockOperation(String operationId) {
        return lock(receipts.findPeriodId(operationId).orElseThrow(FiscalPeriodTransitionNotFoundException::new));
    }

    private FiscalPeriod lock(Long periodId) {
        return periods.findByIdForUpdate(periodId).orElseThrow(FiscalPeriodTransitionNotFoundException::new);
    }

    private FiscalPeriodTransition requireReceipt(String operationId) {
        return receipts.findByOperationId(operationId).orElseThrow(FiscalPeriodTransitionNotFoundException::new);
    }

    private void audit(String event, FiscalPeriodTransition receipt, String actor, String detail) {
        var context = MasterDataActorContext.current();
        audits.append(new MasterDataAuditEvent(event, Instant.now(), MasterDataType.FISCAL_PERIOD,
                receipt.fiscalPeriodId().toString(), null, null, context == null ? "closing" : context.serviceActor(),
                actor, detail, receipt.fiscalPeriodId(), receipt.fiscalPeriodId()));
    }
}
