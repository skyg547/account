package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.out.ProvisionBatchPersistencePort;
import com.ho.account.closing.application.port.out.ValuationBatchPersistencePort;
import com.ho.account.closing.domain.ProvisionBatch;
import com.ho.account.closing.domain.ValuationBatch;
import com.ho.account.closing.application.port.out.ClosingFinancialRunLockPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Persists API-triggered execution history across remote Journal calls.
 *
 * <p>The initial key and RUNNING row commit independently. The final state joins the keyed
 * database gate transaction, so a crash rolls it back while leaving the original run ID intact.
 * Ambiguous outcomes are recorded in a new transaction after the gate releases its lock.</p>
 */
@Service
@RequiredArgsConstructor
public class ClosingBatchExecutionRecorder {

    private final ValuationBatchPersistencePort valuationBatchPersistencePort;
    private final ProvisionBatchPersistencePort provisionBatchPersistencePort;
    private final ClosingFinancialRunLockPort runLocks;

    /** Resolves one endpoint-scoped business key to a stable batch ID before the keyed execution gate. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ValuationBatch claimValuation(FiscalPeriodRef period, ValuationBatch.ValuationType type,
                                         String actor, String key) {
        String trustedActor = requireActor(actor);
        return valuationBatchPersistencePort.findByExecutionKey(key).map(batch -> {
            requireSameRequest(batch.getFiscalPeriodId(), batch.getValuationType(), batch.getRunBy(),
                    period.id(), type, trustedActor);
            return batch;
        }).orElseGet(() -> {
            ValuationBatch batch = startValuationEntity(period, type, trustedActor);
            batch.setExecutionKey(key);
            runLocks.ensureLockRow("VALUATION", key);
            return valuationBatchPersistencePort.save(batch);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProvisionBatch claimProvision(FiscalPeriodRef period, ProvisionBatch.ProvisionType type,
                                         String actor, String key) {
        String trustedActor = requireActor(actor);
        return provisionBatchPersistencePort.findByExecutionKey(key).map(batch -> {
            requireSameRequest(batch.getFiscalPeriodId(), batch.getProvisionType(), batch.getRunBy(),
                    period.id(), type, trustedActor);
            return batch;
        }).orElseGet(() -> {
            ProvisionBatch batch = startProvisionEntity(period, type, trustedActor);
            batch.setExecutionKey(key);
            runLocks.ensureLockRow("PROVISION", key);
            return provisionBatchPersistencePort.save(batch);
        });
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public ValuationBatch findValuationByKey(String key) {
        return valuationBatchPersistencePort.findByExecutionKey(key)
                .orElseThrow(() -> new EntityNotFoundException("Valuation execution key not found"));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public ProvisionBatch findProvisionByKey(String key) {
        return provisionBatchPersistencePort.findByExecutionKey(key)
                .orElseThrow(() -> new EntityNotFoundException("Provision execution key not found"));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void valuationOutcomeUnknown(Long batchId, Integer knownJournalCount, String actor) {
        ValuationBatch batch = valuationBatchPersistencePort.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("ValuationBatch not found: " + batchId));
        if (batch.getStatus() == ValuationBatch.ValuationBatchStatus.COMPLETED
                || batch.getStatus() == ValuationBatch.ValuationBatchStatus.PENDING_APPROVAL) return;
        batch.setStatus(ValuationBatch.ValuationBatchStatus.RECONCILIATION_REQUIRED);
        batch.setJournalCount(knownJournalCount);
        batch.setAuditUser(requireActor(actor));
        valuationBatchPersistencePort.save(batch);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void provisionOutcomeUnknown(Long batchId, Integer knownJournalCount, String actor) {
        ProvisionBatch batch = provisionBatchPersistencePort.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("ProvisionBatch not found: " + batchId));
        if (batch.getStatus() == ProvisionBatch.ProvisionBatchStatus.COMPLETED
                || batch.getStatus() == ProvisionBatch.ProvisionBatchStatus.PENDING_APPROVAL) return;
        batch.setStatus(ProvisionBatch.ProvisionBatchStatus.RECONCILIATION_REQUIRED);
        batch.setJournalCount(knownJournalCount);
        batch.setAuditUser(requireActor(actor));
        provisionBatchPersistencePort.save(batch);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void valuationNoEffectFailure(Long batchId, String actor) {
        ValuationBatch batch = valuationBatchPersistencePort.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("ValuationBatch not found: " + batchId));
        if (batch.getStatus() == ValuationBatch.ValuationBatchStatus.COMPLETED
                || batch.getStatus() == ValuationBatch.ValuationBatchStatus.PENDING_APPROVAL) return;
        batch.setStatus(ValuationBatch.ValuationBatchStatus.FAILED);
        batch.setJournalCount(0);
        batch.setAuditUser(requireActor(actor));
        valuationBatchPersistencePort.save(batch);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void provisionNoEffectFailure(Long batchId, String actor) {
        ProvisionBatch batch = provisionBatchPersistencePort.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("ProvisionBatch not found: " + batchId));
        if (batch.getStatus() == ProvisionBatch.ProvisionBatchStatus.COMPLETED
                || batch.getStatus() == ProvisionBatch.ProvisionBatchStatus.PENDING_APPROVAL) return;
        batch.setStatus(ProvisionBatch.ProvisionBatchStatus.FAILED);
        batch.setJournalCount(0);
        batch.setAuditUser(requireActor(actor));
        provisionBatchPersistencePort.save(batch);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ValuationBatch finishValuation(Long batchId, int journalCount, Long journalId,
                                           boolean autoPost, String actor) {
        ValuationBatch batch = valuationBatchPersistencePort.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("ValuationBatch not found: " + batchId));
        batch.setJournalCount(journalCount);
        batch.setGeneratedJournalEntryId(journalId);
        batch.setStatus(journalCount == 0 || autoPost
                ? ValuationBatch.ValuationBatchStatus.COMPLETED
                : ValuationBatch.ValuationBatchStatus.PENDING_APPROVAL);
        batch.setReportLink("/reports/valuation/" + batchId);
        batch.setAuditUser(requireActor(actor));
        return valuationBatchPersistencePort.save(batch);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ProvisionBatch finishProvision(Long batchId, int journalCount, Long journalId,
                                           boolean autoPost, String actor) {
        ProvisionBatch batch = provisionBatchPersistencePort.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("ProvisionBatch not found: " + batchId));
        batch.setJournalCount(journalCount);
        batch.setGeneratedJournalEntryId(journalId);
        batch.setStatus(journalCount == 0 || autoPost
                ? ProvisionBatch.ProvisionBatchStatus.COMPLETED
                : ProvisionBatch.ProvisionBatchStatus.PENDING_APPROVAL);
        batch.setAuditUser(requireActor(actor));
        return provisionBatchPersistencePort.save(batch);
    }

    private void requireSameRequest(Long savedPeriod, Object savedType, String savedActor,
                                    Long period, Object type, String actor) {
        if (!Objects.equals(savedPeriod, period) || !Objects.equals(savedType, type)
                || !Objects.equals(savedActor, actor)) {
            throw new IllegalStateException("Execution key already belongs to different business content");
        }
    }

    private ValuationBatch startValuationEntity(
            FiscalPeriodRef period, ValuationBatch.ValuationType type, String actor) {
        ValuationBatch batch = new ValuationBatch();
        batch.assignFiscalPeriod(period.id(), period.fiscalYear(), period.fiscalPeriod());
        batch.setValuationType(type);
        batch.setRunDateTime(LocalDateTime.now());
        batch.setStatus(ValuationBatch.ValuationBatchStatus.RUNNING);
        batch.setRunBy(requireActor(actor));
        batch.setAuditUser(actor.trim());
        return batch;
    }

    private ProvisionBatch startProvisionEntity(
            FiscalPeriodRef period, ProvisionBatch.ProvisionType type, String actor) {
        ProvisionBatch batch = new ProvisionBatch();
        batch.assignFiscalPeriod(period.id(), period.fiscalYear(), period.fiscalPeriod());
        batch.setProvisionType(type);
        batch.setRunDateTime(LocalDateTime.now());
        batch.setStatus(ProvisionBatch.ProvisionBatchStatus.RUNNING);
        batch.setRunBy(requireActor(actor));
        batch.setAuditUser(actor.trim());
        return batch;
    }

    private String requireActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("actor must not be blank");
        }
        return actor.trim();
    }
}
