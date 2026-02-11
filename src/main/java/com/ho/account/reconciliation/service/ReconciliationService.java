package com.ho.account.reconciliation.service;

import com.ho.account.reconciliation.domain.*;
import com.ho.account.reconciliation.repository.ReconciliationResultRepository;
import com.ho.account.reconciliation.repository.ReconciliationVarianceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import com.ho.account.journal.domain.JournalEntry;

@Service
@Transactional
public class ReconciliationService {

    private final ReconciliationResultRepository reconciliationResultRepository;
    private final ReconciliationVarianceRepository reconciliationVarianceRepository;

    @Autowired
    public ReconciliationService(ReconciliationResultRepository reconciliationResultRepository,
                                 ReconciliationVarianceRepository reconciliationVarianceRepository) {
        this.reconciliationResultRepository = reconciliationResultRepository;
        this.reconciliationVarianceRepository = reconciliationVarianceRepository;
    }

    /**
     * Executes a Source-Standard-Journal-Ledger reconciliation.
     * @param reconciliationDate The date for which reconciliation is performed.
     * @param runBy The user who initiated the reconciliation.
     * @return The ReconciliationResult.
     */
    public ReconciliationResult performSourceStandardReconciliation(LocalDate reconciliationDate, String runBy) {
        ReconciliationResult result = new ReconciliationResult();
        result.setReconciliationDate(reconciliationDate);
        result.setReconciliationType(ReconciliationType.SOURCE_STANDARD);
        result.setRunBy(runBy);
        result.setRunAt(LocalDateTime.now());

        // TODO: Implement actual reconciliation logic for Source-Standard-Journal-Ledger
        // This will involve querying various data sources (source systems, standard data, journal entries, ledger data)
        // and comparing counts and amounts.

        // Placeholder values for now
        result.setTotalCountSource(100L);
        result.setTotalAmountSource(new BigDecimal("10000.00"));
        result.setTotalCountTarget(98L);
        result.setTotalAmountTarget(new BigDecimal("9800.00"));
        result.setVarianceCount(2L);
        result.setVarianceAmount(new BigDecimal("200.00"));

        if (result.getVarianceCount() > 0 || result.getVarianceAmount().compareTo(BigDecimal.ZERO) != 0) {
            result.setStatus(ReconciliationStatus.VARIANCE_FOUND);
            // Example: create a variance
            ReconciliationVariance variance = new ReconciliationVariance();
            variance.setReconciliationResult(result);
            variance.setVarianceCode("SOURCE_DIFF");
            variance.setDescription("Difference found between source and target data.");
            variance.setAmount(result.getVarianceAmount());
            variance.setDrCrType("DEBIT"); // Placeholder
            variance.setStatus(VarianceStatus.OPEN);
            reconciliationVarianceRepository.save(variance);
        } else {
            result.setStatus(ReconciliationStatus.SUCCESS);
        }

        return reconciliationResultRepository.save(result);
    }

    /**
     * Executes an Account Totals reconciliation.
     * @param reconciliationDate The date for which reconciliation is performed.
     * @param runBy The user who initiated the reconciliation.
     * @return The ReconciliationResult.
     */
    public ReconciliationResult performAccountTotalsReconciliation(LocalDate reconciliationDate, String runBy) {
        ReconciliationResult result = new ReconciliationResult();
        result.setReconciliationDate(reconciliationDate);
        result.setReconciliationType(ReconciliationType.ACCOUNT_TOTALS);
        result.setRunBy(runBy);
        result.setRunAt(LocalDateTime.now());

        // TODO: Implement actual reconciliation logic for Account Totals
        // This will involve comparing aggregated balances for accounts.

        // Placeholder values
        result.setTotalCountSource(50L);
        result.setTotalAmountSource(new BigDecimal("50000.00"));
        result.setTotalCountTarget(50L);
        result.setTotalAmountTarget(new BigDecimal("50000.00"));
        result.setVarianceCount(0L);
        result.setVarianceAmount(BigDecimal.ZERO);
        result.setStatus(ReconciliationStatus.SUCCESS);

        return reconciliationResultRepository.save(result);
    }

    /**
     * Executes a Bank Account reconciliation.
     * @param reconciliationDate The date for which reconciliation is performed.
     * @param runBy The user who initiated the reconciliation.
     * @return The ReconciliationResult.
     */
    public ReconciliationResult performBankAccountReconciliation(LocalDate reconciliationDate, String runBy) {
        ReconciliationResult result = new ReconciliationResult();
        result.setReconciliationDate(reconciliationDate);
        result.setReconciliationType(ReconciliationType.BANK_ACCOUNT);
        result.setRunBy(runBy);
        result.setRunAt(LocalDateTime.now());

        // TODO: Implement actual reconciliation logic for Bank Account
        // This will involve comparing bank statements with ledger balances.

        // Placeholder values
        result.setTotalCountSource(200L);
        result.setTotalAmountSource(new BigDecimal("200000.00"));
        result.setTotalCountTarget(199L);
        result.setTotalAmountTarget(new BigDecimal("199900.00"));
        result.setVarianceCount(1L);
        result.setVarianceAmount(new BigDecimal("100.00"));

        if (result.getVarianceCount() > 0 || result.getVarianceAmount().compareTo(BigDecimal.ZERO) != 0) {
            result.setStatus(ReconciliationStatus.VARIANCE_FOUND);
        } else {
            result.setStatus(ReconciliationStatus.SUCCESS);
        }

        return reconciliationResultRepository.save(result);
    }

    /**
     * Retrieves all reconciliation results.
     * @return A list of ReconciliationResult.
     */
    @Transactional(readOnly = true)
    public List<ReconciliationResult> getAllReconciliationResults() {
        return reconciliationResultRepository.findAll();
    }

    /**
     * Retrieves reconciliation results by type.
     * @param type The type of reconciliation.
     * @return A list of ReconciliationResult matching the type.
     */
    @Transactional(readOnly = true)
    public List<ReconciliationResult> getReconciliationResultsByType(ReconciliationType type) {
        // Assuming findByType method is needed in repository
        // return reconciliationResultRepository.findByReconciliationType(type);
        // For now, returning all results and filtering
        return reconciliationResultRepository.findAll().stream()
                .filter(r -> r.getReconciliationType() == type)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all variances for a given reconciliation result.
     * @param reconciliationResultId The ID of the reconciliation result.
     * @return A list of ReconciliationVariance.
     */
    @Transactional(readOnly = true)
    public List<ReconciliationVariance> getVariancesByReconciliationResult(Long reconciliationResultId) {
        // Assuming findByReconciliationResultId method is needed in repository
        // return reconciliationVarianceRepository.findByReconciliationResultId(reconciliationResultId);
        // For now, returning all and filtering
        return reconciliationVarianceRepository.findAll().stream()
                .filter(v -> v.getReconciliationResult() != null && v.getReconciliationResult().getId().equals(reconciliationResultId))
                .collect(Collectors.toList());
    }

    /**
     * Resolves a variance by linking it to an adjustment journal entry.
     * @param varianceId The ID of the variance to resolve.
     * @param journalEntry The adjustment journal entry.
     * @param resolvedBy The user who resolved the variance.
     * @return The updated ReconciliationVariance.
     */
    public ReconciliationVariance resolveVarianceWithAdjustment(Long varianceId, JournalEntry journalEntry, String resolvedBy) {
        ReconciliationVariance variance = reconciliationVarianceRepository.findById(varianceId)
                .orElseThrow(() -> new IllegalArgumentException("Variance not found with ID: " + varianceId));

        variance.setAdjustmentJournalEntry(journalEntry);
        variance.setStatus(VarianceStatus.ADJUSTED);
        variance.setResolvedBy(resolvedBy);
        variance.setResolvedAt(LocalDateTime.now());
        return reconciliationVarianceRepository.save(variance);
    }
}
