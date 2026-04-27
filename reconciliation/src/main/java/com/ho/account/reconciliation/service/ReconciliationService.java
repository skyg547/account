package com.ho.account.reconciliation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.reconciliation.domain.*;
import com.ho.account.reconciliation.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journal.repository.JournalDetailRepository;
import com.ho.account.journal.repository.JournalEntryRepository;
import java.math.BigDecimal;
import com.ho.account.reconciliation.domain.ReconciliationRun.ReconciliationRunStatus; // Added import
import java.util.Collections;

/**
 * ?€??Reconciliation) ê´€??ë¹„ì¦ˆ?ˆìŠ¤ ë¡œì§??ì²˜ë¦¬?˜ëŠ” ?œë¹„???´ë˜??
 * ?€???¨ìœ„, ê·œì¹™, ì°¨ì´ ?¬ìœ  ì½”ë“œ ê´€ë¦?ë°??¤ì œ ?€???¤í–‰ ë¡œì§???¬í•¨?©ë‹ˆ??
 */
@Service
@Transactional
public class ReconciliationService {

    private final ReconciliationUnitRepository reconciliationUnitRepository;
    private final ReconciliationRuleRepository reconciliationRuleRepository;
    private final DifferenceReasonCodeRepository differenceReasonCodeRepository;
    private final ReconciliationRunRepository reconciliationRunRepository;
    private final ReconciliationDifferenceRepository reconciliationDifferenceRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalDetailRepository journalDetailRepository;
    private final AccountSubjectRepository accountSubjectRepository;
    private final ObjectMapper objectMapper; // JSON ?Œì‹±???„í•œ ObjectMapper

    @Autowired
    public ReconciliationService(ReconciliationUnitRepository reconciliationUnitRepository,
                                 ReconciliationRuleRepository reconciliationRuleRepository,
                                 DifferenceReasonCodeRepository differenceReasonCodeRepository,
                                 ReconciliationRunRepository reconciliationRunRepository,
                                 ReconciliationDifferenceRepository reconciliationDifferenceRepository,
                                 JournalEntryRepository journalEntryRepository,
                                 JournalDetailRepository journalDetailRepository,
                                 AccountSubjectRepository accountSubjectRepository,
                                 ObjectMapper objectMapper) {
        this.reconciliationUnitRepository = reconciliationUnitRepository;
        this.reconciliationRuleRepository = reconciliationRuleRepository;
        this.differenceReasonCodeRepository = differenceReasonCodeRepository;
        this.reconciliationRunRepository = reconciliationRunRepository;
        this.reconciliationDifferenceRepository = reconciliationDifferenceRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.journalDetailRepository = journalDetailRepository;
        this.accountSubjectRepository = accountSubjectRepository;
        this.objectMapper = objectMapper;
    }

    // --- ReconciliationUnit (?€???¨ìœ„) ê´€??ë©”ì„œ??---

    /**
     * ?ˆë¡œ???€???¨ìœ„ë¥??ì„±?©ë‹ˆ??
     * @param reconciliationUnit ?ì„±???€???¨ìœ„ ?”í‹°??
     * @return ?ì„±???€???¨ìœ„
     */
    public ReconciliationUnit createReconciliationUnit(ReconciliationUnit reconciliationUnit) {
        // ì¤‘ë³µ ?´ë¦„ ê²€ì¦???ì¶”ê? ë¡œì§ ?„ìš” ??êµ¬í˜„
        return reconciliationUnitRepository.save(reconciliationUnit);
    }

    /**
     * ëª¨ë“  ?€???¨ìœ„ë¥?ì¡°íšŒ?©ë‹ˆ??
     * @return ëª¨ë“  ?€???¨ìœ„ ëª©ë¡
     */
    @Transactional(readOnly = true)
    public List<ReconciliationUnit> findAllReconciliationUnits() {
        return reconciliationUnitRepository.findAll();
    }

    /**
     * IDë¡??€???¨ìœ„ë¥?ì¡°íšŒ?©ë‹ˆ??
     * @param id ?€???¨ìœ„ ID
     * @return ì¡°íšŒ???€???¨ìœ„
     * @throws EntityNotFoundException ?´ë‹¹ ID???€???¨ìœ„ê°€ ?†ì„ ê²½ìš°
     */
    @Transactional(readOnly = true)
    public ReconciliationUnit findReconciliationUnitById(Long id) {
        return reconciliationUnitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationUnit not found with id: " + id));
    }

    /**
     * ?€???¨ìœ„ë¥??…ë°?´íŠ¸?©ë‹ˆ??
     * @param id ?…ë°?´íŠ¸???€???¨ìœ„??ID
     * @param updatedUnit ?…ë°?´íŠ¸???´ìš©???´ì? ?€???¨ìœ„ ?”í‹°??
     * @return ?…ë°?´íŠ¸???€???¨ìœ„
     * @throws EntityNotFoundException ?´ë‹¹ ID???€???¨ìœ„ê°€ ?†ì„ ê²½ìš°
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
        // auditUser ë°?updatedAt?€ @PreUpdate?ì„œ ì²˜ë¦¬??
        return reconciliationUnitRepository.save(existingUnit);
    }

    /**
     * ?€???¨ìœ„ë¥??? œ?©ë‹ˆ?? (?°ê???ê·œì¹™, ?¤í–‰ ê²°ê³¼, ì°¨ì´???¨ê»˜ ì²˜ë¦¬ ?„ìš”)
     * @param id ?? œ???€???¨ìœ„??ID
     */
    public void deleteReconciliationUnit(Long id) {
        // TODO: ?°ê???ReconciliationRule, ReconciliationRun, ReconciliationDifference ì²˜ë¦¬ ë¡œì§ ì¶”ê? ?„ìš”
        reconciliationUnitRepository.deleteById(id);
    }

    // --- ReconciliationRule (?€??ê·œì¹™) ê´€??ë©”ì„œ??---

    /**
     * ?ˆë¡œ???€??ê·œì¹™???ì„±?©ë‹ˆ??
     * @param reconciliationRule ?ì„±???€??ê·œì¹™ ?”í‹°??
     * @return ?ì„±???€??ê·œì¹™
     */
    public ReconciliationRule createReconciliationRule(ReconciliationRule reconciliationRule) {
        // ì¤‘ë³µ ?´ë¦„, ? íš¨??ê²€ì¦???ì¶”ê? ë¡œì§ ?„ìš” ??êµ¬í˜„
        return reconciliationRuleRepository.save(reconciliationRule);
    }

    /**
     * ?¹ì • ?€???¨ìœ„???í•œ ëª¨ë“  ?€??ê·œì¹™??ì¡°íšŒ?©ë‹ˆ??
     * @param unitId ?€???¨ìœ„ ID
     * @return ?€??ê·œì¹™ ëª©ë¡
     */
    @Transactional(readOnly = true)
    public List<ReconciliationRule> findRulesByReconciliationUnit(Long unitId) {
        ReconciliationUnit unit = reconciliationUnitRepository.findById(unitId)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationUnit not found with id: " + unitId));
        return reconciliationRuleRepository.findByReconciliationUnitOrderByPriorityAsc(unit);
    }
    // Note: findByReconciliationUnit method will need to be added to ReconciliationRuleRepository

    /**
     * ?€??ê·œì¹™???…ë°?´íŠ¸?©ë‹ˆ??
     * @param id ?…ë°?´íŠ¸???€??ê·œì¹™??ID
     * @param updatedRule ?…ë°?´íŠ¸???´ìš©???´ì? ?€??ê·œì¹™ ?”í‹°??
     * @return ?…ë°?´íŠ¸???€??ê·œì¹™
     * @throws EntityNotFoundException ?´ë‹¹ ID???€??ê·œì¹™???†ì„ ê²½ìš°
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
        existingRule.setReconciliationUnit(updatedRule.getReconciliationUnit()); // FK ë³€ê²?ê°€?¥ì„±
        return reconciliationRuleRepository.save(existingRule);
    }

    /**
     * ?€??ê·œì¹™???? œ?©ë‹ˆ??
     * @param id ?? œ???€??ê·œì¹™??ID
     */
    public void deleteReconciliationRule(Long id) {
        reconciliationRuleRepository.deleteById(id);
    }

    // --- DifferenceReasonCode (ì°¨ì´ ?¬ìœ  ì½”ë“œ) ê´€??ë©”ì„œ??---

    /**
     * ?ˆë¡œ??ì°¨ì´ ?¬ìœ  ì½”ë“œë¥??ì„±?©ë‹ˆ??
     * @param reasonCode ?ì„±??ì°¨ì´ ?¬ìœ  ì½”ë“œ ?”í‹°??
     * @return ?ì„±??ì°¨ì´ ?¬ìœ  ì½”ë“œ
     */
    public DifferenceReasonCode createDifferenceReasonCode(DifferenceReasonCode reasonCode) {
        // ì¤‘ë³µ ì½”ë“œ/?´ë¦„ ê²€ì¦???ì¶”ê? ë¡œì§ ?„ìš” ??êµ¬í˜„
        return differenceReasonCodeRepository.save(reasonCode);
    }

    /**
     * ëª¨ë“  ì°¨ì´ ?¬ìœ  ì½”ë“œë¥?ì¡°íšŒ?©ë‹ˆ??
     * @return ëª¨ë“  ì°¨ì´ ?¬ìœ  ì½”ë“œ ëª©ë¡
     */
    @Transactional(readOnly = true)
    public List<DifferenceReasonCode> findAllDifferenceReasonCodes() {
        return differenceReasonCodeRepository.findAll();
    }

    /**
     * IDë¡?ì°¨ì´ ?¬ìœ  ì½”ë“œë¥?ì¡°íšŒ?©ë‹ˆ??
     * @param id ì°¨ì´ ?¬ìœ  ì½”ë“œ ID
     * @return ì¡°íšŒ??ì°¨ì´ ?¬ìœ  ì½”ë“œ
     * @throws EntityNotFoundException ?´ë‹¹ ID??ì°¨ì´ ?¬ìœ  ì½”ë“œê°€ ?†ì„ ê²½ìš°
     */
    @Transactional(readOnly = true)
    public DifferenceReasonCode findDifferenceReasonCodeById(Long id) {
        return differenceReasonCodeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DifferenceReasonCode not found with id: " + id));
    }

    /**
     * ì°¨ì´ ?¬ìœ  ì½”ë“œë¥??…ë°?´íŠ¸?©ë‹ˆ??
     * @param id ?…ë°?´íŠ¸??ì°¨ì´ ?¬ìœ  ì½”ë“œ??ID
     * @param updatedReasonCode ?…ë°?´íŠ¸???´ìš©???´ì? ì°¨ì´ ?¬ìœ  ì½”ë“œ ?”í‹°??
     * @return ?…ë°?´íŠ¸??ì°¨ì´ ?¬ìœ  ì½”ë“œ
     * @throws EntityNotFoundException ?´ë‹¹ ID??ì°¨ì´ ?¬ìœ  ì½”ë“œê°€ ?†ì„ ê²½ìš°
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
     * ì°¨ì´ ?¬ìœ  ì½”ë“œë¥??? œ?©ë‹ˆ??
     * @param id ?? œ??ì°¨ì´ ?¬ìœ  ì½”ë“œ??ID
     */
    public void deleteDifferenceReasonCode(Long id) {
        // TODO: ?°ê???ReconciliationDifference ì²˜ë¦¬ ë¡œì§ ì¶”ê? ?„ìš” (FK ?œì•½ ì¡°ê±´)
        differenceReasonCodeRepository.deleteById(id);
    }

    /**
     * ?€??ì°¨ì´ë¥??¹ì • ?¬ìš©?ì—ê²?? ë‹¹?˜ê³  SLA ê¸°í•œ???¤ì •?©ë‹ˆ??
     * @param differenceId ? ë‹¹???€??ì°¨ì´??ID
     * @param assignedToUser ? ë‹¹ë°›ì„ ?¬ìš©??ID ?ëŠ” ?´ë¦„
     * @param slaDueDate SLA ê¸°í•œ
     * @return ?…ë°?´íŠ¸??ReconciliationDifference
     * @throws EntityNotFoundException ?´ë‹¹ ID???€??ì°¨ì´ê°€ ?†ì„ ê²½ìš°
     */
    public ReconciliationDifference assignDifference(Long differenceId, String assignedToUser, LocalDateTime slaDueDate) {
        ReconciliationDifference difference = reconciliationDifferenceRepository.findById(differenceId)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationDifference not found with id: " + differenceId));

        difference.setAssignedToUser(assignedToUser);
        difference.setSlaDueDate(slaDueDate);
        difference.setStatus(ReconciliationDifference.ReconciliationDifferenceStatus.ASSIGNED); // ?íƒœë¥?ASSIGNEDë¡?ë³€ê²?        return reconciliationDifferenceRepository.save(difference);
    }

    /**
     * ?€??ì°¨ì´ë¥??´ê²° ?ëŠ” ë¬´ì‹œ ì²˜ë¦¬?©ë‹ˆ??
     * DoD???°ë¼ ì°¨ì´??ë°˜ë“œ???¬ìœ  ì½”ë“œë¡??˜ë ´?´ì•¼ ?˜ë©°,
     * ì¡°ì •???„ìš”???¬ìœ  ì½”ë“œ??ì¡°ì • ?„í‘œ ë§í¬ê°€ ?ˆì–´???©ë‹ˆ??
     *
     * @param differenceId ?´ê²°???€??ì°¨ì´ ID
     * @param reasonCodeId ?¬ìœ  ì½”ë“œ ID
     * @param adjustmentJournalEntryId ì¡°ì • ?„í‘œ ID
     * @param status ìµœì¢… ?íƒœ (RESOLVED ?ëŠ” IGNORED)
     * @param resolvedBy ì²˜ë¦¬??     * @return ê°±ì‹ ???€??ì°¨ì´
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

        JournalEntry adjustmentJournalEntry = null;
        if (adjustmentJournalEntryId != null) {
            adjustmentJournalEntry = journalEntryRepository.findById(adjustmentJournalEntryId)
                    .orElseThrow(() -> new EntityNotFoundException("JournalEntry not found with id: " + adjustmentJournalEntryId));
        } else if (difference.getAdjustmentJournalEntry() != null) {
            adjustmentJournalEntry = difference.getAdjustmentJournalEntry();
        }

        if (reasonCode.isAdjustable() && adjustmentJournalEntry == null) {
            throw new IllegalArgumentException("Adjustable reason code requires an adjustment journal entry link.");
        }

        difference.setReasonCode(reasonCode);
        difference.setAdjustmentJournalEntry(adjustmentJournalEntry);
        difference.setStatus(status);
        difference.setResolvedBy(resolvedBy);
        difference.setResolvedAt(LocalDateTime.now());
        difference.setAuditUser(resolvedBy);

        return reconciliationDifferenceRepository.save(difference);
    }

    /**
     * ?¹ì • ?€???¤í–‰???´ë‹¹?˜ëŠ” ëª¨ë“  ?€??ì°¨ì´ë¥?ì¡°íšŒ?©ë‹ˆ??
     * @param reconciliationRunId ?€???¤í–‰ ID
     * @return ?€??ì°¨ì´ ëª©ë¡
     */
    @Transactional(readOnly = true)
    public List<ReconciliationDifference> findDifferencesByReconciliationRunId(Long reconciliationRunId) {
        ReconciliationRun run = reconciliationRunRepository.findById(reconciliationRunId)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationRun not found with id: " + reconciliationRunId));
        return reconciliationDifferenceRepository.findByReconciliationRun(run);
    }

    // --- Core Reconciliation Logic (?€???¤í–‰ ë¡œì§) ---

    /**
     * ?¹ì • ?€???¨ìœ„ë¥?ê¸°ë°˜?¼ë¡œ ?€?¬ë? ?¤í–‰?©ë‹ˆ??
     * ??ë©”ì„œ?œëŠ” ?€??ë¡œì§??ì§„ì…?ì´ë©? ?¤ì œ ë§¤ì¹­ ë°?ì°¨ì´ ?ë³„ ë¡œì§???¸ì¶œ?©ë‹ˆ??
     * @param unitId ?€?¬ë? ?¤í–‰???€???¨ìœ„??ID
     * @param reconciliationDate ?€??ê¸°ì???
     * @return ?ì„±???€???¤í–‰ ê²°ê³¼ (ReconciliationRun)
     * @throws EntityNotFoundException ?€???¨ìœ„ë¥?ì°¾ì„ ???†ëŠ” ê²½ìš°
     */
    public ReconciliationRun performReconciliation(Long unitId, LocalDate reconciliationDate) {
        ReconciliationUnit reconciliationUnit = findReconciliationUnitById(unitId);
        List<ReconciliationRule> rules = reconciliationRuleRepository.findByReconciliationUnitOrderByPriorityAsc(reconciliationUnit);

        // ReconciliationRun ?œì‘ ê¸°ë¡
        ReconciliationRun run = new ReconciliationRun();
        run.setReconciliationUnit(reconciliationUnit);
        run.setReconciliationDate(reconciliationDate);
        run.setRunStartTime(LocalDateTime.now());
        run.setStatus(ReconciliationRunStatus.RUNNING);
        run.setRunBy("SYSTEM"); // ?ëŠ” ?„ì¬ ë¡œê·¸???¬ìš©???•ë³´
        run = reconciliationRunRepository.save(run);

        try {
            // Declare local variables for reconciliation results
            BigDecimal sourceAmount = BigDecimal.ZERO;
            BigDecimal targetAmount = BigDecimal.ZERO;
            int sourceCount = 0;
            int targetCount = 0;
            BigDecimal unmatchedAmount = BigDecimal.ZERO;
            int unmatchedCount = 0;
            BigDecimal matchedAmount = BigDecimal.ZERO;
            int matchedCount = 0;

            // 3. ë§¤ì¹­????ª©/ê¸ˆì•¡, ë¯¸ë§¤ì¹?œ ??ª©/ê¸ˆì•¡ ê³„ì‚° (?„ì‹œ ë¡œì§)
            // ?¤ì œ êµ¬í˜„?ì„œ??ê°?rule??ruleDefinitionJson???Œì‹±?˜ì—¬ ë³µì¡??ë§¤ì¹­ ë¡œì§ ?˜í–‰
            // ???ˆì‹œ?ì„œ??ëª¨ë“  ê·œì¹™???ìš©?˜ì—¬ ìµœì¢… ì°¨ì´ë¥?ê³„ì‚°?œë‹¤ê³?ê°€??

            // TODO: Replace with actual logic to fetch source/target data based on reconciliationUnit and rules.
            // ?„ì¬??ì»´íŒŒ?¼ê³¼ ê¸°ë³¸ ?ë¦„ ?•ì¸???„í•´ ?”ë? ê°’ì„ ?¬ìš©
            sourceAmount = new BigDecimal("1000.00"); // Dummy value
            targetAmount = new BigDecimal("950.00");  // Dummy value
            sourceCount = 10;
            targetCount = 9;


            // ?„ì‹œ ë§¤ì¹­ ë¡œì§: ?¨ìˆœ ê¸ˆì•¡ ë¶ˆì¼ì¹?ë°œìƒ ??ì°¨ì´ ?ì„±
            if (sourceAmount.compareTo(targetAmount) != 0) {
                // ì°¨ì´ ë°œìƒ
                unmatchedAmount = sourceAmount.subtract(targetAmount).abs();
                unmatchedCount = sourceCount - targetCount; // ê°„ë‹¨???ˆì‹œë¡?ì°¨ì´ ê°œìˆ˜ ?¤ì •
                matchedAmount = sourceAmount.min(targetAmount);
                matchedCount = Math.min(sourceCount, targetCount);

                ReconciliationDifference diff = new ReconciliationDifference();
                diff.setReconciliationRun(run);
                diff.setDifferenceType(ReconciliationDifference.DifferenceType.AMOUNT_MISMATCH);
                diff.setAmountExpected(sourceAmount);
                diff.setAmountActual(targetAmount);
                diff.setDifferenceAmount(unmatchedAmount);
                diff.setDescription(reconciliationUnit.getName() + " - ê¸ˆì•¡ ë¶ˆì¼ì¹?ë°œìƒ (ê¸°ì??? " + reconciliationDate + ")");
                // sourceItemRef?€ targetItemRef???¤ì œ ?°ì´?°ë? ë°˜ì˜?˜ë„ë¡?ë³€ê²??„ìš”
                diff.setSourceItemRef("{\"type\":\"SUMMARY\",\"date\":\"" + reconciliationDate + "\",\"unit\":\"" + reconciliationUnit.getName() + "\"}");
                diff.setTargetItemRef("{\"type\":\"SUMMARY\",\"date\":\"" + reconciliationDate + "\",\"unit\":\"" + reconciliationUnit.getName() + "\"}");

                // DoD: ì°¨ì´??"?ì¸ì½”ë“œ+ì¡°ì •?„í‘œ ë§í¬"ë¡?ë°˜ë“œ???˜ë ´
                // ê¸°ë³¸ ì°¨ì´ ?¬ìœ  ì½”ë“œë¥?ì¡°íšŒ?˜ê±°?? ê·œì¹™ ê¸°ë°˜?¼ë¡œ ?¹ì • ?¬ìœ  ì½”ë“œ ? ë‹¹
                DifferenceReasonCode defaultReason = differenceReasonCodeRepository.findByCode("GENERIC_MISMATCH")
                        .orElseGet(() -> differenceReasonCodeRepository.save(createDefaultReasonCode())); // ê¸°ë³¸ ?¬ìœ  ì½”ë“œ ?†ìœ¼ë©??ì„± ???€??

                diff.setReasonCode(defaultReason);
                diff.setStatus(ReconciliationDifference.ReconciliationDifferenceStatus.PENDING); // ì´ˆê¸° ?íƒœ
                diff.setAuditUser("SYSTEM");

                // ì¡°ì • ?„í‘œ ?ì„± ë¡œì§ (isAdjustable??true??ê²½ìš°)
                if (defaultReason.isAdjustable()) {
                    // ì°¨ë?/?€ë³€ ê³„ì •ê³¼ëª©?€ ?€???¨ìœ„??? í˜•?´ë‚˜ ?œìŠ¤???¤ì •???°ë¼ ?¬ë¼ì§?
                    // ?¬ê¸°?œëŠ” ?„ì‹œë¡??¹ì • ê³„ì •ê³¼ëª© ?¬ìš©
                    AccountSubject debitAccount = accountSubjectRepository.findById("121000") // ?? ë¯¸ê²°??ê³„ì • (?„ì‹œ)
                            .orElseThrow(() -> new EntityNotFoundException("Debit AccountSubject (121000) not found. Please create it."));
                    AccountSubject creditAccount = accountSubjectRepository.findById("999999") // ?? ?€?¬ì°¨??ì¡°ì • ê³„ì • (?„ì‹œ)
                            .orElseThrow(() -> new EntityNotFoundException("Credit AccountSubject (999999) not found. Please create it."));

                    JournalEntry adjustmentEntry = createAdjustmentJournalEntry(
                            reconciliationDate,
                            unmatchedAmount,
                            reconciliationUnit.getName() + " ?€??ì°¨ì´ ì¡°ì • (" + defaultReason.getName() + ")",
                            debitAccount,
                            creditAccount,
                            "SYSTEM"
                    );
                    diff.setAdjustmentJournalEntry(adjustmentEntry); // ì¡°ì • ?„í‘œ ë§í¬
                }
                reconciliationDifferenceRepository.save(diff);
            }

            // run ê°ì²´ ?…ë°?´íŠ¸
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
            // TODO: ?ëŸ¬ ë¡œê¹…
            throw new RuntimeException("Reconciliation failed for unit " + unitId, e);
        } finally {
            run.setRunEndTime(LocalDateTime.now());
            reconciliationRunRepository.save(run);
        }

        return run;
    }

    /**
     * ì¡°ì • ?„í‘œë¥??ì„±?˜ê³  ?€?¥í•˜???¬í¼ ë©”ì„œ??
     * @param accountingDate ?Œê³„?¼ì
     * @param amount ê¸ˆì•¡
     * @param description ?ìš”
     * @param debitAccount ì°¨ë? ê³„ì •ê³¼ëª©
     * @param creditAccount ?€ë³€ ê³„ì •ê³¼ëª©
     * @param createdBy ?ì„±??
     * @return ?ì„±??JournalEntry
     */
    private JournalEntry createAdjustmentJournalEntry(LocalDate accountingDate, BigDecimal amount, String description,
                                                      AccountSubject debitAccount, AccountSubject creditAccount, String createdBy) {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(LocalDate.now());
        entry.setAccountingDate(accountingDate);
        entry.setDescription(description);
        entry.setStatus(JournalEntryStatus.DRAFT); // ì¡°ì • ?„í‘œ??DRAFT ?íƒœë¡??ì„± ???¹ì¸ ?„ë¡œ?¸ìŠ¤ë¥?ê±°ì¹  ???ˆìŒ
        entry.setEntryType("ADJUSTMENT");
        entry.setCreatedBy(createdBy);
        entry.setAuditUser(createdBy);
        entry.setLineageSourceType("RECONCILIATION");
        entry.setLineageSourceId("RECON_ADJ-" + System.currentTimeMillis()); // ê³ ìœ ??ID ?ì„±

        // JournalDetail - ì°¨ë?
        JournalDetail debitDetail = new JournalDetail();
        debitDetail.setDrcrType("DEBIT");
        debitDetail.setAccountSubject(debitAccount);
        debitDetail.setAmount(amount);
        debitDetail.setBaseAmount(amount); // ê¸°ì? ?µí™” ê¸ˆì•¡???™ì¼?˜ë‹¤ê³?ê°€??
        debitDetail.setDetailDescription(description + " (ì°¨ë?)");
        entry.addDetail(debitDetail);

        // JournalDetail - ?€ë³€
        JournalDetail creditDetail = new JournalDetail();
        creditDetail.setDrcrType("CREDIT");
        creditDetail.setAccountSubject(creditAccount);
        creditDetail.setAmount(amount);
        creditDetail.setBaseAmount(amount); // ê¸°ì? ?µí™” ê¸ˆì•¡???™ì¼?˜ë‹¤ê³?ê°€??
        creditDetail.setDetailDescription(description + " (?€ë³€)");
        entry.addDetail(creditDetail);

        // ?„í‘œë²ˆí˜¸ ?ì„± (?ˆì‹œ)
        entry.setSlipNo(accountingDate.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + "-ADJ-" + journalEntryRepository.count());

        return journalEntryRepository.save(entry);
    }

    // ê¸°ë³¸ ì°¨ì´ ?¬ìœ  ì½”ë“œë¥??ì„±?˜ëŠ” ?¬í¼ ë©”ì„œ??(ì´ˆê¸° ?°ì´??ë¡œë”© ???¬ìš© ê°€??
    private DifferenceReasonCode createDefaultReasonCode() {
        DifferenceReasonCode defaultReason = new DifferenceReasonCode();
        defaultReason.setCode("GENERIC_MISMATCH");
        defaultReason.setName("?¼ë°˜ ë¶ˆì¼ì¹?);
        defaultReason.setDescription("?ë™ ë§¤ì¹­?˜ì? ?Šì? ?¼ë°˜?ì¸ ë¶ˆì¼ì¹?);
        defaultReason.setAdjustable(true); // ê¸°ë³¸?ìœ¼ë¡?ì¡°ì • ê°€?¥í•˜?„ë¡ ?¤ì •
        defaultReason.setActive(true);
        return defaultReason;
    }
}
