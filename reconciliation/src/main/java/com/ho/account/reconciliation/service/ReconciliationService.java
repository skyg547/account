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
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.domain.journal.repository.JournalDetailRepository;
import com.ho.account.journalledger.domain.journal.repository.JournalEntryRepository;
import java.math.BigDecimal;
import com.ho.account.reconciliation.domain.ReconciliationRun.ReconciliationRunStatus; // Added import
import java.util.Collections;

/**
 * ???Reconciliation) 愿??鍮꾩쫰?덉뒪 濡쒖쭅??泥섎━?섎뒗 ?쒕퉬???대옒??
 * ????⑥쐞, 洹쒖튃, 李⑥씠 ?ъ쑀 肄붾뱶 愿由?諛??ㅼ젣 ????ㅽ뻾 濡쒖쭅???ы븿?⑸땲??
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
    private final AccountSubjectPersistencePort accountSubjectPersistencePort;
    private final ObjectMapper objectMapper; // JSON ?뚯떛???꾪븳 ObjectMapper

    @Autowired
    public ReconciliationService(ReconciliationUnitRepository reconciliationUnitRepository,
                                 ReconciliationRuleRepository reconciliationRuleRepository,
                                 DifferenceReasonCodeRepository differenceReasonCodeRepository,
                                 ReconciliationRunRepository reconciliationRunRepository,
                                 ReconciliationDifferenceRepository reconciliationDifferenceRepository,
                                 JournalEntryRepository journalEntryRepository,
                                 JournalDetailRepository journalDetailRepository,
                                 AccountSubjectPersistencePort accountSubjectPersistencePort,
                                 ObjectMapper objectMapper) {
        this.reconciliationUnitRepository = reconciliationUnitRepository;
        this.reconciliationRuleRepository = reconciliationRuleRepository;
        this.differenceReasonCodeRepository = differenceReasonCodeRepository;
        this.reconciliationRunRepository = reconciliationRunRepository;
        this.reconciliationDifferenceRepository = reconciliationDifferenceRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.journalDetailRepository = journalDetailRepository;
        this.accountSubjectPersistencePort = accountSubjectPersistencePort;
        this.objectMapper = objectMapper;
    }

    // --- ReconciliationUnit (????⑥쐞) 愿??硫붿꽌??---

    /**
     * ?덈줈??????⑥쐞瑜??앹꽦?⑸땲??
     * @param reconciliationUnit ?앹꽦??????⑥쐞 ?뷀떚??
     * @return ?앹꽦??????⑥쐞
     */
    public ReconciliationUnit createReconciliationUnit(ReconciliationUnit reconciliationUnit) {
        // 以묐났 ?대쫫 寃利???異붽? 濡쒖쭅 ?꾩슂 ??援ы쁽
        return reconciliationUnitRepository.save(reconciliationUnit);
    }

    /**
     * 紐⑤뱺 ????⑥쐞瑜?議고쉶?⑸땲??
     * @return 紐⑤뱺 ????⑥쐞 紐⑸줉
     */
    @Transactional(readOnly = true)
    public List<ReconciliationUnit> findAllReconciliationUnits() {
        return reconciliationUnitRepository.findAll();
    }

    /**
     * ID濡?????⑥쐞瑜?議고쉶?⑸땲??
     * @param id ????⑥쐞 ID
     * @return 議고쉶??????⑥쐞
     * @throws EntityNotFoundException ?대떦 ID??????⑥쐞媛 ?놁쓣 寃쎌슦
     */
    @Transactional(readOnly = true)
    public ReconciliationUnit findReconciliationUnitById(Long id) {
        return reconciliationUnitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationUnit not found with id: " + id));
    }

    /**
     * ????⑥쐞瑜??낅뜲?댄듃?⑸땲??
     * @param id ?낅뜲?댄듃??????⑥쐞??ID
     * @param updatedUnit ?낅뜲?댄듃???댁슜???댁? ????⑥쐞 ?뷀떚??
     * @return ?낅뜲?댄듃??????⑥쐞
     * @throws EntityNotFoundException ?대떦 ID??????⑥쐞媛 ?놁쓣 寃쎌슦
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
        // auditUser 諛?updatedAt? @PreUpdate?먯꽌 泥섎━??
        return reconciliationUnitRepository.save(existingUnit);
    }

    /**
     * ????⑥쐞瑜???젣?⑸땲?? (?곌???洹쒖튃, ?ㅽ뻾 寃곌낵, 李⑥씠???④퍡 泥섎━ ?꾩슂)
     * @param id ??젣??????⑥쐞??ID
     */
    public void deleteReconciliationUnit(Long id) {
        // TODO: ?곌???ReconciliationRule, ReconciliationRun, ReconciliationDifference 泥섎━ 濡쒖쭅 異붽? ?꾩슂
        reconciliationUnitRepository.deleteById(id);
    }

    // --- ReconciliationRule (???洹쒖튃) 愿??硫붿꽌??---

    /**
     * ?덈줈?????洹쒖튃???앹꽦?⑸땲??
     * @param reconciliationRule ?앹꽦?????洹쒖튃 ?뷀떚??
     * @return ?앹꽦?????洹쒖튃
     */
    public ReconciliationRule createReconciliationRule(ReconciliationRule reconciliationRule) {
        // 以묐났 ?대쫫, ?좏슚??寃利???異붽? 濡쒖쭅 ?꾩슂 ??援ы쁽
        return reconciliationRuleRepository.save(reconciliationRule);
    }

    /**
     * ?뱀젙 ????⑥쐞???랁븳 紐⑤뱺 ???洹쒖튃??議고쉶?⑸땲??
     * @param unitId ????⑥쐞 ID
     * @return ???洹쒖튃 紐⑸줉
     */
    @Transactional(readOnly = true)
    public List<ReconciliationRule> findRulesByReconciliationUnit(Long unitId) {
        ReconciliationUnit unit = reconciliationUnitRepository.findById(unitId)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationUnit not found with id: " + unitId));
        return reconciliationRuleRepository.findByReconciliationUnitOrderByPriorityAsc(unit);
    }
    // Note: findByReconciliationUnit method will need to be added to ReconciliationRuleRepository

    /**
     * ???洹쒖튃???낅뜲?댄듃?⑸땲??
     * @param id ?낅뜲?댄듃?????洹쒖튃??ID
     * @param updatedRule ?낅뜲?댄듃???댁슜???댁? ???洹쒖튃 ?뷀떚??
     * @return ?낅뜲?댄듃?????洹쒖튃
     * @throws EntityNotFoundException ?대떦 ID?????洹쒖튃???놁쓣 寃쎌슦
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
        existingRule.setReconciliationUnit(updatedRule.getReconciliationUnit()); // FK 蹂寃?媛?μ꽦
        return reconciliationRuleRepository.save(existingRule);
    }

    /**
     * ???洹쒖튃????젣?⑸땲??
     * @param id ??젣?????洹쒖튃??ID
     */
    public void deleteReconciliationRule(Long id) {
        reconciliationRuleRepository.deleteById(id);
    }

    // --- DifferenceReasonCode (李⑥씠 ?ъ쑀 肄붾뱶) 愿??硫붿꽌??---

    /**
     * ?덈줈??李⑥씠 ?ъ쑀 肄붾뱶瑜??앹꽦?⑸땲??
     * @param reasonCode ?앹꽦??李⑥씠 ?ъ쑀 肄붾뱶 ?뷀떚??
     * @return ?앹꽦??李⑥씠 ?ъ쑀 肄붾뱶
     */
    public DifferenceReasonCode createDifferenceReasonCode(DifferenceReasonCode reasonCode) {
        // 以묐났 肄붾뱶/?대쫫 寃利???異붽? 濡쒖쭅 ?꾩슂 ??援ы쁽
        return differenceReasonCodeRepository.save(reasonCode);
    }

    /**
     * 紐⑤뱺 李⑥씠 ?ъ쑀 肄붾뱶瑜?議고쉶?⑸땲??
     * @return 紐⑤뱺 李⑥씠 ?ъ쑀 肄붾뱶 紐⑸줉
     */
    @Transactional(readOnly = true)
    public List<DifferenceReasonCode> findAllDifferenceReasonCodes() {
        return differenceReasonCodeRepository.findAll();
    }

    /**
     * ID濡?李⑥씠 ?ъ쑀 肄붾뱶瑜?議고쉶?⑸땲??
     * @param id 李⑥씠 ?ъ쑀 肄붾뱶 ID
     * @return 議고쉶??李⑥씠 ?ъ쑀 肄붾뱶
     * @throws EntityNotFoundException ?대떦 ID??李⑥씠 ?ъ쑀 肄붾뱶媛 ?놁쓣 寃쎌슦
     */
    @Transactional(readOnly = true)
    public DifferenceReasonCode findDifferenceReasonCodeById(Long id) {
        return differenceReasonCodeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DifferenceReasonCode not found with id: " + id));
    }

    /**
     * 李⑥씠 ?ъ쑀 肄붾뱶瑜??낅뜲?댄듃?⑸땲??
     * @param id ?낅뜲?댄듃??李⑥씠 ?ъ쑀 肄붾뱶??ID
     * @param updatedReasonCode ?낅뜲?댄듃???댁슜???댁? 李⑥씠 ?ъ쑀 肄붾뱶 ?뷀떚??
     * @return ?낅뜲?댄듃??李⑥씠 ?ъ쑀 肄붾뱶
     * @throws EntityNotFoundException ?대떦 ID??李⑥씠 ?ъ쑀 肄붾뱶媛 ?놁쓣 寃쎌슦
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
     * 李⑥씠 ?ъ쑀 肄붾뱶瑜???젣?⑸땲??
     * @param id ??젣??李⑥씠 ?ъ쑀 肄붾뱶??ID
     */
    public void deleteDifferenceReasonCode(Long id) {
        // TODO: ?곌???ReconciliationDifference 泥섎━ 濡쒖쭅 異붽? ?꾩슂 (FK ?쒖빟 議곌굔)
        differenceReasonCodeRepository.deleteById(id);
    }

    /**
     * ???李⑥씠瑜??뱀젙 ?ъ슜?먯뿉寃??좊떦?섍퀬 SLA 湲고븳???ㅼ젙?⑸땲??
     * @param differenceId ?좊떦?????李⑥씠??ID
     * @param assignedToUser ?좊떦諛쏆쓣 ?ъ슜??ID ?먮뒗 ?대쫫
     * @param slaDueDate SLA 湲고븳
     * @return ?낅뜲?댄듃??ReconciliationDifference
     * @throws EntityNotFoundException ?대떦 ID?????李⑥씠媛 ?놁쓣 寃쎌슦
     */
    public ReconciliationDifference assignDifference(Long differenceId, String assignedToUser, LocalDateTime slaDueDate) {
        ReconciliationDifference difference = reconciliationDifferenceRepository.findById(differenceId)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationDifference not found with id: " + differenceId));

        difference.setAssignedToUser(assignedToUser);
        difference.setSlaDueDate(slaDueDate);
        difference.setStatus(ReconciliationDifference.ReconciliationDifferenceStatus.ASSIGNED); // ?곹깭瑜?ASSIGNED濡?蹂寃?
        return reconciliationDifferenceRepository.save(difference);
    }

    /**
     * ???李⑥씠瑜??닿껐 ?먮뒗 臾댁떆 泥섎━?⑸땲??
     * DoD???곕씪 李⑥씠??諛섎뱶???ъ쑀 肄붾뱶濡??섎졃?댁빞 ?섎ŉ,
     * 議곗젙???꾩슂???ъ쑀 肄붾뱶??議곗젙 ?꾪몴 留곹겕媛 ?덉뼱???⑸땲??
     *
     * @param differenceId ?닿껐?????李⑥씠 ID
     * @param reasonCodeId ?ъ쑀 肄붾뱶 ID
     * @param adjustmentJournalEntryId 議곗젙 ?꾪몴 ID
     * @param status 理쒖쥌 ?곹깭 (RESOLVED ?먮뒗 IGNORED)
     * @param resolvedBy 泥섎━??     * @return 媛깆떊?????李⑥씠
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
     * ?뱀젙 ????ㅽ뻾???대떦?섎뒗 紐⑤뱺 ???李⑥씠瑜?議고쉶?⑸땲??
     * @param reconciliationRunId ????ㅽ뻾 ID
     * @return ???李⑥씠 紐⑸줉
     */
    @Transactional(readOnly = true)
    public List<ReconciliationDifference> findDifferencesByReconciliationRunId(Long reconciliationRunId) {
        ReconciliationRun run = reconciliationRunRepository.findById(reconciliationRunId)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationRun not found with id: " + reconciliationRunId));
        return reconciliationDifferenceRepository.findByReconciliationRun(run);
    }

    // --- Core Reconciliation Logic (????ㅽ뻾 濡쒖쭅) ---

    /**
     * ?뱀젙 ????⑥쐞瑜?湲곕컲?쇰줈 ??щ? ?ㅽ뻾?⑸땲??
     * ??硫붿꽌?쒕뒗 ???濡쒖쭅??吏꾩엯?먯씠硫? ?ㅼ젣 留ㅼ묶 諛?李⑥씠 ?앸퀎 濡쒖쭅???몄텧?⑸땲??
     * @param unitId ??щ? ?ㅽ뻾??????⑥쐞??ID
     * @param reconciliationDate ???湲곗???
     * @return ?앹꽦??????ㅽ뻾 寃곌낵 (ReconciliationRun)
     * @throws EntityNotFoundException ????⑥쐞瑜?李얠쓣 ???녿뒗 寃쎌슦
     */
    public ReconciliationRun performReconciliation(Long unitId, LocalDate reconciliationDate) {
        ReconciliationUnit reconciliationUnit = findReconciliationUnitById(unitId);
        List<ReconciliationRule> rules = reconciliationRuleRepository.findByReconciliationUnitOrderByPriorityAsc(reconciliationUnit);

        // ReconciliationRun ?쒖옉 湲곕줉
        ReconciliationRun run = new ReconciliationRun();
        run.setReconciliationUnit(reconciliationUnit);
        run.setReconciliationDate(reconciliationDate);
        run.setRunStartTime(LocalDateTime.now());
        run.setStatus(ReconciliationRunStatus.RUNNING);
        run.setRunBy("SYSTEM"); // ?먮뒗 ?꾩옱 濡쒓렇???ъ슜???뺣낫
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

            // 3. 留ㅼ묶????ぉ/湲덉븸, 誘몃ℓ移?맂 ??ぉ/湲덉븸 怨꾩궛 (?꾩떆 濡쒖쭅)
            // ?ㅼ젣 援ы쁽?먯꽌??媛?rule??ruleDefinitionJson???뚯떛?섏뿬 蹂듭옟??留ㅼ묶 濡쒖쭅 ?섑뻾
            // ???덉떆?먯꽌??紐⑤뱺 洹쒖튃???곸슜?섏뿬 理쒖쥌 李⑥씠瑜?怨꾩궛?쒕떎怨?媛??

            // TODO: Replace with actual logic to fetch source/target data based on reconciliationUnit and rules.
            // ?꾩옱??而댄뙆?쇨낵 湲곕낯 ?먮쫫 ?뺤씤???꾪빐 ?붾? 媛믪쓣 ?ъ슜
            sourceAmount = new BigDecimal("1000.00"); // Dummy value
            targetAmount = new BigDecimal("950.00");  // Dummy value
            sourceCount = 10;
            targetCount = 9;


            // ?꾩떆 留ㅼ묶 濡쒖쭅: ?⑥닚 湲덉븸 遺덉씪移?諛쒖깮 ??李⑥씠 ?앹꽦
            if (sourceAmount.compareTo(targetAmount) != 0) {
                // 李⑥씠 諛쒖깮
                unmatchedAmount = sourceAmount.subtract(targetAmount).abs();
                unmatchedCount = sourceCount - targetCount; // 媛꾨떒???덉떆濡?李⑥씠 媛쒖닔 ?ㅼ젙
                matchedAmount = sourceAmount.min(targetAmount);
                matchedCount = Math.min(sourceCount, targetCount);

                ReconciliationDifference diff = new ReconciliationDifference();
                diff.setReconciliationRun(run);
                diff.setDifferenceType(ReconciliationDifference.DifferenceType.AMOUNT_MISMATCH);
                diff.setAmountExpected(sourceAmount);
                diff.setAmountActual(targetAmount);
                diff.setDifferenceAmount(unmatchedAmount);
                diff.setDescription(reconciliationUnit.getName() + " - 湲덉븸 遺덉씪移?諛쒖깮 (湲곗??? " + reconciliationDate + ")");
                // sourceItemRef? targetItemRef???ㅼ젣 ?곗씠?곕? 諛섏쁺?섎룄濡?蹂寃??꾩슂
                diff.setSourceItemRef("{\"type\":\"SUMMARY\",\"date\":\"" + reconciliationDate + "\",\"unit\":\"" + reconciliationUnit.getName() + "\"}");
                diff.setTargetItemRef("{\"type\":\"SUMMARY\",\"date\":\"" + reconciliationDate + "\",\"unit\":\"" + reconciliationUnit.getName() + "\"}");

                // DoD: 李⑥씠??"?먯씤肄붾뱶+議곗젙?꾪몴 留곹겕"濡?諛섎뱶???섎졃
                // 湲곕낯 李⑥씠 ?ъ쑀 肄붾뱶瑜?議고쉶?섍굅?? 洹쒖튃 湲곕컲?쇰줈 ?뱀젙 ?ъ쑀 肄붾뱶 ?좊떦
                DifferenceReasonCode defaultReason = differenceReasonCodeRepository.findByCode("GENERIC_MISMATCH")
                        .orElseGet(() -> differenceReasonCodeRepository.save(createDefaultReasonCode())); // 湲곕낯 ?ъ쑀 肄붾뱶 ?놁쑝硫??앹꽦 ?????

                diff.setReasonCode(defaultReason);
                diff.setStatus(ReconciliationDifference.ReconciliationDifferenceStatus.PENDING); // 珥덇린 ?곹깭
                diff.setAuditUser("SYSTEM");

                // 議곗젙 ?꾪몴 ?앹꽦 濡쒖쭅 (isAdjustable??true??寃쎌슦)
                if (defaultReason.isAdjustable()) {
                    // 李⑤?/?蹂 怨꾩젙怨쇰ぉ? ????⑥쐞???좏삎?대굹 ?쒖뒪???ㅼ젙???곕씪 ?щ씪吏?
                    // ?ш린?쒕뒗 ?꾩떆濡??뱀젙 怨꾩젙怨쇰ぉ ?ъ슜
                    AccountSubject debitAccount = accountSubjectPersistencePort.findByCode("121000") // ?? 誘멸껐??怨꾩젙 (?꾩떆)
                            .orElseThrow(() -> new EntityNotFoundException("Debit AccountSubject (121000) not found. Please create it."));
                    AccountSubject creditAccount = accountSubjectPersistencePort.findByCode("999999") // ?? ??ъ감??議곗젙 怨꾩젙 (?꾩떆)
                            .orElseThrow(() -> new EntityNotFoundException("Credit AccountSubject (999999) not found. Please create it."));

                    JournalEntry adjustmentEntry = createAdjustmentJournalEntry(
                            reconciliationDate,
                            unmatchedAmount,
                            reconciliationUnit.getName() + " ???李⑥씠 議곗젙 (" + defaultReason.getName() + ")",
                            debitAccount,
                            creditAccount,
                            "SYSTEM"
                    );
                    diff.setAdjustmentJournalEntry(adjustmentEntry); // 議곗젙 ?꾪몴 留곹겕
                }
                reconciliationDifferenceRepository.save(diff);
            }

            // run 媛앹껜 ?낅뜲?댄듃
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
            // TODO: ?먮윭 濡쒓퉭
            throw new RuntimeException("Reconciliation failed for unit " + unitId, e);
        } finally {
            run.setRunEndTime(LocalDateTime.now());
            reconciliationRunRepository.save(run);
        }

        return run;
    }

    /**
     * 議곗젙 ?꾪몴瑜??앹꽦?섍퀬 ??ν븯???ы띁 硫붿꽌??
     * @param accountingDate ?뚭퀎?쇱옄
     * @param amount 湲덉븸
     * @param description ?곸슂
     * @param debitAccount 李⑤? 怨꾩젙怨쇰ぉ
     * @param creditAccount ?蹂 怨꾩젙怨쇰ぉ
     * @param createdBy ?앹꽦??
     * @return ?앹꽦??JournalEntry
     */
    private JournalEntry createAdjustmentJournalEntry(LocalDate accountingDate, BigDecimal amount, String description,
                                                      AccountSubject debitAccount, AccountSubject creditAccount, String createdBy) {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(LocalDate.now());
        entry.setAccountingDate(accountingDate);
        entry.setDescription(description);
        entry.setStatus(JournalEntryStatus.DRAFT); // 議곗젙 ?꾪몴??DRAFT ?곹깭濡??앹꽦 ???뱀씤 ?꾨줈?몄뒪瑜?嫄곗튌 ???덉쓬
        entry.setEntryType("ADJUSTMENT");
        entry.setCreatedBy(createdBy);
        entry.setAuditUser(createdBy);
        entry.setLineageSourceType("RECONCILIATION");
        entry.setLineageSourceId("RECON_ADJ-" + System.currentTimeMillis()); // 怨좎쑀??ID ?앹꽦

        // JournalDetail - 李⑤?
        JournalDetail debitDetail = new JournalDetail();
        debitDetail.setSide(JournalSide.DEBIT);
        debitDetail.setAccountSubject(debitAccount);
        debitDetail.setAmount(amount);
        debitDetail.setBaseAmount(amount); // 湲곗? ?듯솕 湲덉븸???숈씪?섎떎怨?媛??
        debitDetail.setDetailDescription(description + " (李⑤?)");
        entry.addDetail(debitDetail);

        // JournalDetail - ?蹂
        JournalDetail creditDetail = new JournalDetail();
        creditDetail.setSide(JournalSide.CREDIT);
        creditDetail.setAccountSubject(creditAccount);
        creditDetail.setAmount(amount);
        creditDetail.setBaseAmount(amount); // 湲곗? ?듯솕 湲덉븸???숈씪?섎떎怨?媛??
        creditDetail.setDetailDescription(description + " (?蹂)");
        entry.addDetail(creditDetail);

        // ?꾪몴踰덊샇 ?앹꽦 (?덉떆)
        entry.setSlipNo(accountingDate.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + "-ADJ-" + journalEntryRepository.count());

        return journalEntryRepository.save(entry);
    }

    // 湲곕낯 李⑥씠 ?ъ쑀 肄붾뱶瑜??앹꽦?섎뒗 ?ы띁 硫붿꽌??(珥덇린 ?곗씠??濡쒕뵫 ???ъ슜 媛??
    private DifferenceReasonCode createDefaultReasonCode() {
        DifferenceReasonCode defaultReason = new DifferenceReasonCode();
        defaultReason.setCode("GENERIC_MISMATCH");
        defaultReason.setName("Generic Mismatch");
        defaultReason.setDescription("General mismatch found during reconciliation");
        defaultReason.setAdjustable(true); // 湲곕낯?곸쑝濡?議곗젙 媛?ν븯?꾨줉 ?ㅼ젙
        defaultReason.setActive(true);
        return defaultReason;
    }
}
