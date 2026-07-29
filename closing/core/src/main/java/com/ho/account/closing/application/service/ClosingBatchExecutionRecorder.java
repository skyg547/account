package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.out.ProvisionBatchPersistencePort;
import com.ho.account.closing.application.port.out.ValuationBatchPersistencePort;
import com.ho.account.closing.domain.ProvisionBatch;
import com.ho.account.closing.domain.ValuationBatch;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Persists API-triggered execution history independently from journal transactions.
 *
 * <p>A failed journal call must not erase the RUNNING/FAILED audit record. Every method therefore
 * owns a REQUIRES_NEW transaction.</p>
 */
@Service
@RequiredArgsConstructor
public class ClosingBatchExecutionRecorder {

    private final ValuationBatchPersistencePort valuationBatchPersistencePort;
    private final ProvisionBatchPersistencePort provisionBatchPersistencePort;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ValuationBatch startValuation(
            FiscalPeriodRef period,
            ValuationBatch.ValuationType type,
            String actor) {
        ValuationBatch batch = new ValuationBatch();
        batch.assignFiscalPeriod(period.id(), period.fiscalYear(), period.fiscalPeriod());
        batch.setValuationType(type);
        batch.setRunDateTime(LocalDateTime.now());
        batch.setStatus(ValuationBatch.ValuationBatchStatus.RUNNING);
        batch.setRunBy(requireActor(actor));
        batch.setAuditUser(actor.trim());
        return valuationBatchPersistencePort.save(batch);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ValuationBatch markValuationPendingApproval(
            Long batchId,
            Long journalEntryId,
            String reportLink,
            String actor) {
        ValuationBatch batch = valuationBatchPersistencePort.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("ValuationBatch not found: " + batchId));
        batch.setGeneratedJournalEntryId(journalEntryId);
        batch.setStatus(ValuationBatch.ValuationBatchStatus.PENDING_APPROVAL);
        batch.setReportLink(reportLink);
        batch.setAuditUser(requireActor(actor));
        return valuationBatchPersistencePort.save(batch);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markValuationFailed(Long batchId, String actor) {
        ValuationBatch batch = valuationBatchPersistencePort.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("ValuationBatch not found: " + batchId));
        batch.setStatus(ValuationBatch.ValuationBatchStatus.FAILED);
        batch.setAuditUser(requireActor(actor));
        valuationBatchPersistencePort.save(batch);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProvisionBatch startProvision(
            FiscalPeriodRef period,
            ProvisionBatch.ProvisionType type,
            String actor) {
        ProvisionBatch batch = new ProvisionBatch();
        batch.assignFiscalPeriod(period.id(), period.fiscalYear(), period.fiscalPeriod());
        batch.setProvisionType(type);
        batch.setRunDateTime(LocalDateTime.now());
        batch.setStatus(ProvisionBatch.ProvisionBatchStatus.RUNNING);
        batch.setRunBy(requireActor(actor));
        batch.setAuditUser(actor.trim());
        return provisionBatchPersistencePort.save(batch);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProvisionBatch markProvisionPendingApproval(
            Long batchId,
            Long journalEntryId,
            String actor) {
        ProvisionBatch batch = provisionBatchPersistencePort.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("ProvisionBatch not found: " + batchId));
        batch.setGeneratedJournalEntryId(journalEntryId);
        batch.setStatus(ProvisionBatch.ProvisionBatchStatus.PENDING_APPROVAL);
        batch.setAuditUser(requireActor(actor));
        return provisionBatchPersistencePort.save(batch);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProvisionFailed(Long batchId, String actor) {
        ProvisionBatch batch = provisionBatchPersistencePort.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("ProvisionBatch not found: " + batchId));
        batch.setStatus(ProvisionBatch.ProvisionBatchStatus.FAILED);
        batch.setAuditUser(requireActor(actor));
        provisionBatchPersistencePort.save(batch);
    }

    private String requireActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("actor must not be blank");
        }
        return actor.trim();
    }
}
