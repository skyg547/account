package com.ho.account.reconciliation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.contracts.journal.JournalSummary;
import com.ho.account.reconciliation.domain.*;
import com.ho.account.reconciliation.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;
import com.ho.account.reconciliation.domain.ReconciliationRun.ReconciliationRunStatus;

/**
 * Application service for reconciliation units, rules, difference reasons, runs, and resolution flows.
 */
@Service
@Transactional
public class ReconciliationService {

    private final ReconciliationUnitRepository reconciliationUnitRepository;
    private final ReconciliationRuleRepository reconciliationRuleRepository;
    private final DifferenceReasonCodeRepository differenceReasonCodeRepository;
    private final ReconciliationRunRepository reconciliationRunRepository;
    private final ReconciliationDifferenceRepository reconciliationDifferenceRepository;
    private final JournalQueryPort journalQueryPort;
    private final JournalPostingPort journalPostingPort;
    private final ObjectMapper objectMapper;
    private final ReconciliationTolerancePolicy tolerancePolicy = new ReconciliationTolerancePolicy();

    @Autowired
    public ReconciliationService(ReconciliationUnitRepository reconciliationUnitRepository,
                                 ReconciliationRuleRepository reconciliationRuleRepository,
                                 DifferenceReasonCodeRepository differenceReasonCodeRepository,
                                 ReconciliationRunRepository reconciliationRunRepository,
                                 ReconciliationDifferenceRepository reconciliationDifferenceRepository,
                                 JournalQueryPort journalQueryPort,
                                 JournalPostingPort journalPostingPort,
                                 ObjectMapper objectMapper) {
        this.reconciliationUnitRepository = reconciliationUnitRepository;
        this.reconciliationRuleRepository = reconciliationRuleRepository;
        this.differenceReasonCodeRepository = differenceReasonCodeRepository;
        this.reconciliationRunRepository = reconciliationRunRepository;
        this.reconciliationDifferenceRepository = reconciliationDifferenceRepository;
        this.journalQueryPort = journalQueryPort;
        this.journalPostingPort = journalPostingPort;
        this.objectMapper = objectMapper;
    }

    // --- ReconciliationUnit methods ---

    /**
     * Creates a reconciliation unit.
     *
     * @param reconciliationUnit reconciliation unit to create
     * @return created reconciliation unit
     */
    public ReconciliationUnit createReconciliationUnit(ReconciliationUnit reconciliationUnit) {
        return reconciliationUnitRepository.save(reconciliationUnit);
    }

    /**
     * Finds all reconciliation units.
     *
     * @return all reconciliation units
     */
    @Transactional(readOnly = true)
    public List<ReconciliationUnit> findAllReconciliationUnits() {
        return reconciliationUnitRepository.findAll();
    }

    /**
     * Finds a reconciliation unit by id.
     *
     * @param id reconciliation unit id
     * @return found reconciliation unit
     * @throws EntityNotFoundException when no unit exists for the id
     */
    @Transactional(readOnly = true)
    public ReconciliationUnit findReconciliationUnitById(Long id) {
        return reconciliationUnitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationUnit not found with id: " + id));
    }

    /**
     * Updates a reconciliation unit.
     *
     * @param id reconciliation unit id
     * @param updatedUnit new unit values
     * @return updated reconciliation unit
     * @throws EntityNotFoundException when no unit exists for the id
     */
    public ReconciliationUnit updateReconciliationUnit(Long id, ReconciliationUnit updatedUnit) {
        ReconciliationUnit existingUnit = reconciliationUnitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationUnit not found with id: " + id));

        existingUnit.setName(updatedUnit.getName());
        existingUnit.setDescription(updatedUnit.getDescription());
        existingUnit.setFrequency(updatedUnit.getFrequency());
        existingUnit.setReconciliationType(updatedUnit.getReconciliationType());
        existingUnit.setCriteriaJson(updatedUnit.getCriteriaJson());
        existingUnit.setActive(updatedUnit.isActive());
        return reconciliationUnitRepository.save(existingUnit);
    }

    /**
     * Deletes a reconciliation unit.
     *
     * @param id reconciliation unit id
     */
    public void deleteReconciliationUnit(Long id) {
        // TODO: define cleanup policy for related rules, runs, and differences.
        reconciliationUnitRepository.deleteById(id);
    }

    // --- ReconciliationRule methods ---

    /**
     * Creates a reconciliation rule.
     *
     * @param reconciliationRule reconciliation rule to create
     * @return created reconciliation rule
     */
    public ReconciliationRule createReconciliationRule(ReconciliationRule reconciliationRule) {
        return reconciliationRuleRepository.save(reconciliationRule);
    }

    /**
     * Finds rules for a reconciliation unit ordered by priority.
     *
     * @param unitId reconciliation unit id
     * @return reconciliation rules
     */
    @Transactional(readOnly = true)
    public List<ReconciliationRule> findRulesByReconciliationUnit(Long unitId) {
        ReconciliationUnit unit = reconciliationUnitRepository.findById(unitId)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationUnit not found with id: " + unitId));
        return reconciliationRuleRepository.findByReconciliationUnitOrderByPriorityAsc(unit);
    }

    /**
     * Updates a reconciliation rule.
     *
     * @param id reconciliation rule id
     * @param updatedRule new rule values
     * @return updated reconciliation rule
     * @throws EntityNotFoundException when no rule exists for the id
     */
    public ReconciliationRule updateReconciliationRule(Long id, ReconciliationRule updatedRule) {
        ReconciliationRule existingRule = reconciliationRuleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationRule not found with id: " + id));

        existingRule.setName(updatedRule.getName());
        existingRule.setRuleDefinitionJson(updatedRule.getRuleDefinitionJson());
        existingRule.setToleranceType(updatedRule.getToleranceType());
        existingRule.setToleranceValue(updatedRule.getToleranceValue());
        existingRule.setPriority(updatedRule.getPriority());
        existingRule.setActive(updatedRule.isActive());
        existingRule.setReconciliationUnit(updatedRule.getReconciliationUnit());
        return reconciliationRuleRepository.save(existingRule);
    }

    /**
     * Deletes a reconciliation rule.
     *
     * @param id reconciliation rule id
     */
    public void deleteReconciliationRule(Long id) {
        reconciliationRuleRepository.deleteById(id);
    }

    // --- DifferenceReasonCode methods ---

    /**
     * Creates a difference reason code.
     *
     * @param reasonCode reason code to create
     * @return created reason code
     */
    public DifferenceReasonCode createDifferenceReasonCode(DifferenceReasonCode reasonCode) {
        return differenceReasonCodeRepository.save(reasonCode);
    }

    /**
     * Finds all difference reason codes.
     *
     * @return all reason codes
     */
    @Transactional(readOnly = true)
    public List<DifferenceReasonCode> findAllDifferenceReasonCodes() {
        return differenceReasonCodeRepository.findAll();
    }

    /**
     * Finds a difference reason code by id.
     *
     * @param id reason code id
     * @return found reason code
     * @throws EntityNotFoundException when no reason code exists for the id
     */
    @Transactional(readOnly = true)
    public DifferenceReasonCode findDifferenceReasonCodeById(Long id) {
        return differenceReasonCodeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DifferenceReasonCode not found with id: " + id));
    }

    /**
     * Updates a difference reason code.
     *
     * @param id reason code id
     * @param updatedReasonCode new reason code values
     * @return updated reason code
     * @throws EntityNotFoundException when no reason code exists for the id
     */
    public DifferenceReasonCode updateDifferenceReasonCode(Long id, DifferenceReasonCode updatedReasonCode) {
        DifferenceReasonCode existingCode = differenceReasonCodeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DifferenceReasonCode not found with id: " + id));

        existingCode.setCode(updatedReasonCode.getCode());
        existingCode.setName(updatedReasonCode.getName());
        existingCode.setDescription(updatedReasonCode.getDescription());
        existingCode.setAdjustable(updatedReasonCode.isAdjustable());
        existingCode.setActive(updatedReasonCode.isActive());
        return differenceReasonCodeRepository.save(existingCode);
    }

    /**
     * Deletes a difference reason code.
     *
     * @param id reason code id
     */
    public void deleteDifferenceReasonCode(Long id) {
        // TODO: define cleanup policy for differences that reference this reason code.
        differenceReasonCodeRepository.deleteById(id);
    }

    /**
     * Assigns a reconciliation difference and sets its SLA due date.
     *
     * @param differenceId difference id
     * @param assignedToUser assignee id or name
     * @param slaDueDate SLA due date
     * @return updated difference
     * @throws EntityNotFoundException when no difference exists for the id
     */
    public ReconciliationDifference assignDifference(Long differenceId, String assignedToUser, LocalDateTime slaDueDate) {
        ReconciliationDifference difference = reconciliationDifferenceRepository.findById(differenceId)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationDifference not found with id: " + differenceId));

        difference.setAssignedToUser(assignedToUser);
        difference.setSlaDueDate(slaDueDate);
        difference.setStatus(ReconciliationDifference.ReconciliationDifferenceStatus.ASSIGNED);
        return reconciliationDifferenceRepository.save(difference);
    }

    /**
     * Finalizes a reconciliation difference as resolved or ignored.
     * Adjustable reason codes require an adjustment journal entry id.
     *
     * @param differenceId difference id
     * @param reasonCodeId reason code id
     * @param adjustmentJournalEntryId adjustment journal entry id
     * @param status final status, RESOLVED or IGNORED
     * @param resolvedBy resolver id or name
     * @return updated difference
     */
    public ReconciliationDifference resolveDifference(Long differenceId, Long reasonCodeId, Long adjustmentJournalEntryId,
                                                      ReconciliationDifference.ReconciliationDifferenceStatus status,
                                                      String resolvedBy) {
        ReconciliationDifference difference = reconciliationDifferenceRepository.findById(differenceId)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationDifference not found with id: " + differenceId));
        DifferenceReasonCode reasonCode = differenceReasonCodeRepository.findById(reasonCodeId)
                .orElseThrow(() -> new EntityNotFoundException("DifferenceReasonCode not found with id: " + reasonCodeId));

        if (status != ReconciliationDifference.ReconciliationDifferenceStatus.RESOLVED
                && status != ReconciliationDifference.ReconciliationDifferenceStatus.IGNORED) {
            throw new IllegalArgumentException("Difference can only be finalized as RESOLVED or IGNORED.");
        }

        Long adjustmentJournalEntryIdToLink = null;
        if (adjustmentJournalEntryId != null) {
            validateAdjustmentJournalEntry(adjustmentJournalEntryId);
            adjustmentJournalEntryIdToLink = adjustmentJournalEntryId;
        } else if (difference.getAdjustmentJournalEntryId() != null) {
            adjustmentJournalEntryIdToLink = difference.getAdjustmentJournalEntryId();
        }

        if (reasonCode.isAdjustable() && adjustmentJournalEntryIdToLink == null) {
            throw new IllegalArgumentException("Adjustable reason code requires an adjustment journal entry link.");
        }

        difference.setReasonCode(reasonCode);
        difference.setAdjustmentJournalEntryId(adjustmentJournalEntryIdToLink);
        difference.setStatus(status);
        difference.setResolvedBy(resolvedBy);
        difference.setResolvedAt(LocalDateTime.now());
        difference.setAuditUser(resolvedBy);

        return reconciliationDifferenceRepository.save(difference);
    }

    /**
     * Finds differences for a reconciliation run.
     *
     * @param reconciliationRunId reconciliation run id
     * @return differences for the run
     */
    @Transactional(readOnly = true)
    public List<ReconciliationDifference> findDifferencesByReconciliationRunId(Long reconciliationRunId) {
        ReconciliationRun run = reconciliationRunRepository.findById(reconciliationRunId)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationRun not found with id: " + reconciliationRunId));
        return reconciliationDifferenceRepository.findByReconciliationRun(run);
    }

    // --- Core reconciliation execution ---

    /**
     * Executes reconciliation for a unit and date.
     *
     * @param unitId reconciliation unit id
     * @param reconciliationDate reconciliation basis date
     * @return created reconciliation run
     * @throws EntityNotFoundException when no unit exists for the id
     */
    public ReconciliationRun performReconciliation(Long unitId, LocalDate reconciliationDate) {
        ReconciliationUnit reconciliationUnit = findReconciliationUnitById(unitId);
        List<ReconciliationRule> rules = reconciliationRuleRepository.findByReconciliationUnitOrderByPriorityAsc(reconciliationUnit);

        ReconciliationRun run = new ReconciliationRun();
        run.setReconciliationUnit(reconciliationUnit);
        run.setReconciliationDate(reconciliationDate);
        run.setRunStartTime(LocalDateTime.now());
        run.setStatus(ReconciliationRunStatus.RUNNING);
        run.setRunBy("SYSTEM");
        run = reconciliationRunRepository.save(run);

        try {
            ReconciliationSnapshot sourceSnapshot = buildSourceSnapshot(reconciliationUnit);
            ReconciliationSnapshot targetSnapshot = buildTargetSnapshot(reconciliationDate);

            BigDecimal sourceAmount = sourceSnapshot.amount();
            BigDecimal targetAmount = targetSnapshot.amount();
            int sourceCount = sourceSnapshot.count();
            int targetCount = targetSnapshot.count();
            BigDecimal unmatchedAmount = BigDecimal.ZERO;
            int unmatchedCount = 0;
            BigDecimal matchedAmount = BigDecimal.ZERO;
            int matchedCount = 0;
            BigDecimal amountDifference = sourceAmount.subtract(targetAmount).abs();
            BigDecimal amountTolerance = tolerancePolicy.resolveAmountTolerance(rules, sourceAmount);

            if (amountDifference.compareTo(amountTolerance) > 0) {
                unmatchedAmount = amountDifference;
                unmatchedCount = Math.abs(sourceCount - targetCount);
                matchedAmount = sourceAmount.min(targetAmount);
                matchedCount = Math.min(sourceCount, targetCount);

                ReconciliationDifference diff = new ReconciliationDifference();
                diff.setReconciliationRun(run);
                diff.setDifferenceType(ReconciliationDifference.DifferenceType.AMOUNT_MISMATCH);
                diff.setAmountExpected(sourceAmount);
                diff.setAmountActual(targetAmount);
                diff.setDifferenceAmount(unmatchedAmount);
                diff.setDescription(reconciliationUnit.getName() + " - amount mismatch (date " + reconciliationDate + ")");
                diff.setSourceItemRef("{\"type\":\"SUMMARY\",\"date\":\"" + reconciliationDate + "\",\"unit\":\"" + reconciliationUnit.getName() + "\"}");
                diff.setTargetItemRef("{\"type\":\"SUMMARY\",\"date\":\"" + reconciliationDate + "\",\"unit\":\"" + reconciliationUnit.getName() + "\"}");

                DifferenceReasonCode defaultReason = differenceReasonCodeRepository.findByCode("GENERIC_MISMATCH")
                        .orElseGet(() -> differenceReasonCodeRepository.save(createDefaultReasonCode()));

                diff.setReasonCode(defaultReason);
                diff.setStatus(ReconciliationDifference.ReconciliationDifferenceStatus.PENDING);
                diff.setAuditUser("SYSTEM");

                if (defaultReason.isAdjustable()) {
                    AdjustmentAccountCodes accountCodes = resolveAdjustmentAccountCodes(reconciliationUnit);

                    Long adjustmentEntryId = createAdjustmentJournalEntry(
                            reconciliationDate,
                            unmatchedAmount,
                            reconciliationUnit.getName() + " reconciliation difference adjustment (" + defaultReason.getName() + ")",
                            accountCodes,
                            "SYSTEM"
                    );
                    diff.setAdjustmentJournalEntryId(adjustmentEntryId);
                }
                reconciliationDifferenceRepository.save(diff);
            } else {
                matchedAmount = sourceAmount.min(targetAmount);
                matchedCount = Math.min(sourceCount, targetCount);
            }

            run.setTotalItemsSource((long)sourceCount);
            run.setTotalAmountSource(sourceAmount);
            run.setTotalItemsTarget((long)targetCount);
            run.setTotalAmountTarget(targetAmount);
            run.setMatchedItemsCount((long)matchedCount);
            run.setMatchedAmount(matchedAmount);
            run.setUnmatchedItemsCount((long)unmatchedCount);
            run.setUnmatchedAmount(unmatchedAmount);
            run.setStatus(ReconciliationRunStatus.SUCCESS);


        } catch (Exception e) {
            run.setStatus(ReconciliationRunStatus.FAILED);
            throw new RuntimeException("Reconciliation failed for unit " + unitId, e);
        } finally {
            run.setRunEndTime(LocalDateTime.now());
            reconciliationRunRepository.save(run);
        }

        return run;
    }

    private ReconciliationSnapshot buildSourceSnapshot(ReconciliationUnit reconciliationUnit) {
        JsonNode root = parseCriteriaJson(reconciliationUnit);
        BigDecimal amount = readDecimal(root, "sourceAmount");
        int count = readInt(root, "sourceCount");
        return new ReconciliationSnapshot(count, amount);
    }

    private ReconciliationSnapshot buildTargetSnapshot(LocalDate reconciliationDate) {
        BigDecimal targetAmount = BigDecimal.ZERO;
        int targetCount = 0;

        List<JournalSummary> journalSummaries = journalQueryPort.getJournalSummaries(reconciliationDate, reconciliationDate);
        for (JournalSummary journalSummary : journalSummaries) {
            if (journalSummary.getId() == null) {
                continue;
            }
            for (JournalDetailSummary detail : journalQueryPort.getJournalDetails(journalSummary.getId())) {
                if (detail.getSide() == com.ho.account.contracts.journal.JournalSide.DEBIT) {
                    targetAmount = targetAmount.add(resolveDetailAmount(detail));
                    targetCount++;
                }
            }
        }

        return new ReconciliationSnapshot(targetCount, targetAmount);
    }

    private BigDecimal resolveDetailAmount(JournalDetailSummary detail) {
        if (detail.getBaseAmount() != null) {
            return detail.getBaseAmount();
        }
        if (detail.getAmount() != null) {
            return detail.getAmount();
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal readDecimal(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return BigDecimal.ZERO;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        if (node.isTextual() && !node.asText().isBlank()) {
            return new BigDecimal(node.asText());
        }
        return BigDecimal.ZERO;
    }

    private int readInt(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return 0;
        }
        if (node.isNumber()) {
            return node.intValue();
        }
        if (node.isTextual() && !node.asText().isBlank()) {
            return Integer.parseInt(node.asText());
        }
        return 0;
    }

    private AdjustmentAccountCodes resolveAdjustmentAccountCodes(ReconciliationUnit reconciliationUnit) {
        JsonNode root = parseCriteriaJson(reconciliationUnit);
        String debitAccountCode = readText(root, "adjustmentDebitAccountCode");
        String creditAccountCode = readText(root, "adjustmentCreditAccountCode");
        if (debitAccountCode == null || creditAccountCode == null) {
            throw new IllegalArgumentException(
                    "Adjustable reconciliation unit " + reconciliationUnit.getId()
                            + " requires adjustmentDebitAccountCode and adjustmentCreditAccountCode in criteriaJson.");
        }
        return new AdjustmentAccountCodes(debitAccountCode, creditAccountCode);
    }

    private String readText(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText();
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim();
    }

    private JsonNode parseCriteriaJson(ReconciliationUnit reconciliationUnit) {
        String criteriaJson = reconciliationUnit.getCriteriaJson();
        if (criteriaJson == null || criteriaJson.isBlank()) {
            return objectMapper.createObjectNode();
        }

        try {
            return objectMapper.readTree(criteriaJson);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid reconciliation criteriaJson for unit " + reconciliationUnit.getId(), e);
        }
    }

    /**
     * Creates an adjustment journal entry through the journal posting port.
     *
     * @param accountingDate accounting date
     * @param amount adjustment amount
     * @param description journal description
     * @param accountCodes debit and credit account codes
     * @param createdBy creator id or name
     * @return created journal entry id
     */
    private Long createAdjustmentJournalEntry(LocalDate accountingDate, BigDecimal amount, String description,
                                              AdjustmentAccountCodes accountCodes, String createdBy) {
        JournalEntryCommand command = new JournalEntryCommand(
                LocalDate.now(),
                accountingDate,
                description,
                "ADJUSTMENT",
                "KRW",
                BigDecimal.ONE,
                createdBy,
                createdBy,
                "RECONCILIATION",
                "RECON_ADJ-" + System.currentTimeMillis(),
                List.of(
                        new JournalLineCommand("DEBIT", accountCodes.debitAccountCode(), amount, amount, null, null, description + " (debit)"),
                        new JournalLineCommand("CREDIT", accountCodes.creditAccountCode(), amount, amount, null, null, description + " (credit)")
                )
        );

        JournalPostingResult result = journalPostingPort.createDraftEntry(command);
        if (result.journalEntryId() == null) {
            throw new IllegalStateException("JournalPostingPort returned no journalEntryId for reconciliation adjustment.");
        }
        return result.journalEntryId();
    }

    private void validateAdjustmentJournalEntry(Long adjustmentJournalEntryId) {
        try {
            if (journalQueryPort.getJournalSummary(adjustmentJournalEntryId) == null) {
                throw new EntityNotFoundException("JournalEntry not found with id: " + adjustmentJournalEntryId);
            }
        } catch (RuntimeException e) {
            throw new EntityNotFoundException("JournalEntry not found with id: " + adjustmentJournalEntryId);
        }
    }

    private record ReconciliationSnapshot(int count, BigDecimal amount) {
    }

    private record AdjustmentAccountCodes(String debitAccountCode, String creditAccountCode) {
    }

    private DifferenceReasonCode createDefaultReasonCode() {
        DifferenceReasonCode defaultReason = new DifferenceReasonCode();
        defaultReason.setCode("GENERIC_MISMATCH");
        defaultReason.setName("Generic Mismatch");
        defaultReason.setDescription("General mismatch found during reconciliation");
        defaultReason.setAdjustable(false);
        defaultReason.setActive(true);
        return defaultReason;
    }
}
