package com.ho.account.reconciliation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.contracts.journal.JournalDetailAggregateSummary;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.contracts.journal.JournalSide;
import com.ho.account.reconciliation.application.port.in.AssignDifferenceCommand;
import com.ho.account.reconciliation.application.port.in.DifferenceReasonCodeCommand;
import com.ho.account.reconciliation.application.port.in.ReconciliationRuleCommand;
import com.ho.account.reconciliation.application.port.in.ReconciliationUnitCommand;
import com.ho.account.reconciliation.application.port.in.ResolveDifferenceCommand;
import com.ho.account.reconciliation.application.port.in.RunReconciliationCommand;
import com.ho.account.reconciliation.application.port.out.ExternalReconSnapshotPort;
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
 * [헥사고날 아키텍처 - 애플리케이션 서비스 (Application Service)]
 * 대사(Reconciliation) 업무의 핵심 유즈케이스 흐름을 조율하는 서비스입니다.
 * 
 * 🐣 [초보자를 위한 설명: Rich Domain Model 패턴 적용]
 * 이 서비스는 대사 업무의 '현장 소장 (Orchestrator)'입니다.
 * 과거 Anemic Domain Model에서는 서비스가 엔티티의 상태(status), 담당자, 일시, 집계 금액 등을 일일이 setter로 조작하고
 * 비즈니스 검증(예: 조정 전표 연계 필요 여부, 완료 상태 전이 등)을 직접 담당하여 서비스 코드가 거대하고 파편화되었습니다.
 * 
 * Rich Domain Model로 전환된 현재,
 * 1. 상태 검증 및 비즈니스 룰은 엔티티(`ReconciliationDifference`, `ReconciliationRun`) 내부 도메인 메서드에 캡슐화되었습니다.
 * 2. 서비스는 외부 포트(데이터 조회, 전표 생성, 저장소 접근)와의 연동 및 도메인 객체의 호출 순서를 관리하는
 *    **포트 조율(Orchestration)** 본연의 책임에 집중합니다.
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
    private final ReconciliationAdjustmentPolicy adjustmentPolicy;
    private final ReconciliationTolerancePolicy tolerancePolicy = new ReconciliationTolerancePolicy();
    private final ExternalReconSnapshotPort externalReconSnapshotPort;

    @Autowired
    public ReconciliationService(ReconciliationUnitRepository reconciliationUnitRepository,
                                 ReconciliationRuleRepository reconciliationRuleRepository,
                                 DifferenceReasonCodeRepository differenceReasonCodeRepository,
                                 ReconciliationRunRepository reconciliationRunRepository,
                                 ReconciliationDifferenceRepository reconciliationDifferenceRepository,
                                 JournalQueryPort journalQueryPort,
                                 JournalPostingPort journalPostingPort,
                                 ObjectMapper objectMapper,
                                 ReconciliationAdjustmentPolicy adjustmentPolicy,
                                 ExternalReconSnapshotPort externalReconSnapshotPort) {
        this.reconciliationUnitRepository = reconciliationUnitRepository;
        this.reconciliationRuleRepository = reconciliationRuleRepository;
        this.differenceReasonCodeRepository = differenceReasonCodeRepository;
        this.reconciliationRunRepository = reconciliationRunRepository;
        this.reconciliationDifferenceRepository = reconciliationDifferenceRepository;
        this.journalQueryPort = journalQueryPort;
        this.journalPostingPort = journalPostingPort;
        this.objectMapper = objectMapper;
        this.adjustmentPolicy = adjustmentPolicy;
        this.externalReconSnapshotPort = externalReconSnapshotPort;
    }

    // --- ReconciliationUnit methods ---

    /**
     * Creates a reconciliation unit.
     *
     * @param command API/Batch 공통 입력을 담은 대사 단위 생성 command
     * @return created reconciliation unit
     */
    public ReconciliationUnit createReconciliationUnit(ReconciliationUnitCommand command) {
        ReconciliationUnit reconciliationUnit = toReconciliationUnit(command);
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
     * @param command API/Batch 공통 입력을 담은 대사 단위 수정 command
     * @return updated reconciliation unit
     * @throws EntityNotFoundException when no unit exists for the id
     */
    public ReconciliationUnit updateReconciliationUnit(Long id, ReconciliationUnitCommand command) {
        ReconciliationUnit existingUnit = reconciliationUnitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationUnit not found with id: " + id));

        existingUnit.setName(command.name());
        existingUnit.setDescription(command.description());
        existingUnit.setFrequency(command.frequency());
        existingUnit.setReconciliationType(command.reconciliationType());
        existingUnit.setCriteriaJson(command.criteriaJson());
        existingUnit.setActive(command.active());
        return reconciliationUnitRepository.save(existingUnit);
    }


    private ReconciliationUnit toReconciliationUnit(ReconciliationUnitCommand command) {
        ReconciliationUnit unit = new ReconciliationUnit();
        unit.setName(command.name());
        unit.setDescription(command.description());
        unit.setFrequency(command.frequency());
        unit.setReconciliationType(command.reconciliationType());
        unit.setCriteriaJson(command.criteriaJson());
        unit.setActive(command.active());
        return unit;
    }
    /**
     * Deletes a reconciliation unit.
     *
     * @param id reconciliation unit id
     */
    public void deleteReconciliationUnit(Long id) {
        // T36 fixed: DDD consistency: Units with historical data should be soft-deleted (archived) rather than hard-deleted.
        ReconciliationUnit existingUnit = findReconciliationUnitById(id);
        existingUnit.setActive(false);
        reconciliationUnitRepository.save(existingUnit);
    }

    // --- ReconciliationRule methods ---

    /**
     * Creates a reconciliation rule.
     *
     * @param command API/Batch 공통 입력을 담은 대사 규칙 생성 command
     * @return created reconciliation rule
     */
    public ReconciliationRule createReconciliationRule(ReconciliationRuleCommand command) {
        ReconciliationRule reconciliationRule = toReconciliationRule(command);
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
     * @param command API/Batch 공통 입력을 담은 대사 규칙 수정 command
     * @return updated reconciliation rule
     * @throws EntityNotFoundException when no rule exists for the id
     */
    public ReconciliationRule updateReconciliationRule(Long id, ReconciliationRuleCommand command) {
        ReconciliationRule existingRule = reconciliationRuleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationRule not found with id: " + id));
        ReconciliationUnit reconciliationUnit = findReconciliationUnitById(command.reconciliationUnitId());

        existingRule.setName(command.name());
        existingRule.setRuleDefinitionJson(command.ruleDefinitionJson());
        existingRule.setToleranceType(command.toleranceType());
        existingRule.setToleranceValue(command.toleranceValue());
        existingRule.setPriority(command.priority());
        existingRule.setActive(command.active());
        existingRule.setReconciliationUnit(reconciliationUnit);
        return reconciliationRuleRepository.save(existingRule);
    }


    private ReconciliationRule toReconciliationRule(ReconciliationRuleCommand command) {
        ReconciliationUnit reconciliationUnit = findReconciliationUnitById(command.reconciliationUnitId());
        ReconciliationRule rule = new ReconciliationRule();
        rule.setReconciliationUnit(reconciliationUnit);
        rule.setName(command.name());
        rule.setRuleDefinitionJson(command.ruleDefinitionJson());
        rule.setToleranceType(command.toleranceType());
        rule.setToleranceValue(command.toleranceValue());
        rule.setPriority(command.priority());
        rule.setActive(command.active());
        return rule;
    }
    /**
     * Deletes a reconciliation rule.
     *
     * @param id reconciliation rule id
     */
    public void deleteReconciliationRule(Long id) {
        ReconciliationRule existingRule = reconciliationRuleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationRule not found with id: " + id));
        // 과거 대사 실행 이력이 어떤 규칙으로 판단됐는지 추적할 수 있도록 물리 삭제하지 않습니다.
        existingRule.setActive(false);
        reconciliationRuleRepository.save(existingRule);
    }

    // --- DifferenceReasonCode methods ---

    /**
     * Creates a difference reason code.
     *
     * @param command API/Batch 공통 입력을 담은 차이 사유 코드 생성 command
     * @return created reason code
     */
    public DifferenceReasonCode createDifferenceReasonCode(DifferenceReasonCodeCommand command) {
        DifferenceReasonCode reasonCode = toDifferenceReasonCode(command);
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
     * @param command API/Batch 공통 입력을 담은 차이 사유 코드 수정 command
     * @return updated reason code
     * @throws EntityNotFoundException when no reason code exists for the id
     */
    public DifferenceReasonCode updateDifferenceReasonCode(Long id, DifferenceReasonCodeCommand command) {
        DifferenceReasonCode existingCode = differenceReasonCodeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DifferenceReasonCode not found with id: " + id));

        existingCode.setCode(command.code());
        existingCode.setName(command.name());
        existingCode.setDescription(command.description());
        existingCode.setAdjustable(command.adjustable());
        existingCode.setActive(command.active());
        return differenceReasonCodeRepository.save(existingCode);
    }


    private DifferenceReasonCode toDifferenceReasonCode(DifferenceReasonCodeCommand command) {
        DifferenceReasonCode reasonCode = new DifferenceReasonCode();
        reasonCode.setCode(command.code());
        reasonCode.setName(command.name());
        reasonCode.setDescription(command.description());
        reasonCode.setAdjustable(command.adjustable());
        reasonCode.setActive(command.active());
        return reasonCode;
    }
    /**
     * Deletes a difference reason code.
     *
     * @param id reason code id
     */
    public void deleteDifferenceReasonCode(Long id) {
        DifferenceReasonCode reasonCode = differenceReasonCodeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DifferenceReasonCode not found with id: " + id));
        // 과거 차이가 참조하는 사유 코드는 삭제하지 않고 신규 사용만 막습니다.
        reasonCode.setActive(false);
        differenceReasonCodeRepository.save(reasonCode);
    }

    /**
     * Assigns a reconciliation difference and sets its SLA due date.
     *
     * @param command 담당자와 SLA를 포함한 차이 배정 command
     * @return updated difference
     * @throws EntityNotFoundException when no difference exists for the id
     */
    public ReconciliationDifference assignDifference(AssignDifferenceCommand command) {
        ReconciliationDifference difference = reconciliationDifferenceRepository.findById(command.differenceId())
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationDifference not found with id: " + command.differenceId()));

        difference.assignOwner(command.assignedToUser(), command.slaDueDate());
        return reconciliationDifferenceRepository.save(difference);
    }

    /**
     * Finalizes a reconciliation difference as resolved or ignored.
     * Adjustable reason codes require an adjustment journal entry id.
     *
     * @param command 사유 코드, 조정 전표, 해결자, 최종 상태를 포함한 차이 해결 command
     * @return updated difference
     */
    public ReconciliationDifference resolveDifference(ResolveDifferenceCommand command) {
        Long differenceId = command.differenceId();
        Long reasonCodeId = command.reasonCodeId();
        Long adjustmentJournalEntryId = command.adjustmentJournalEntryId();
        ReconciliationDifference.ReconciliationDifferenceStatus status = command.status();
        String resolvedBy = command.resolvedBy();

        ReconciliationDifference difference = reconciliationDifferenceRepository.findById(differenceId)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationDifference not found with id: " + differenceId));
        DifferenceReasonCode reasonCode = differenceReasonCodeRepository.findById(reasonCodeId)
                .orElseThrow(() -> new EntityNotFoundException("DifferenceReasonCode not found with id: " + reasonCodeId));

        if (adjustmentJournalEntryId != null) {
            validateAdjustmentJournalEntry(adjustmentJournalEntryId);
        }

        difference.resolve(reasonCode, adjustmentJournalEntryId, status, resolvedBy);

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
     * @param command 대사 단위, 기준일, 실행자를 포함한 대사 실행 command
     * @return created reconciliation run
     * @throws EntityNotFoundException when no unit exists for the id
     */
    public ReconciliationRun performReconciliation(RunReconciliationCommand command) {
        Long unitId = command.reconciliationUnitId();
        LocalDate reconciliationDate = command.reconciliationDate();
        String runBy = command.runBy();
        ReconciliationUnit reconciliationUnit = findReconciliationUnitById(unitId);
        List<ReconciliationRule> rules = reconciliationRuleRepository.findByReconciliationUnitOrderByPriorityAsc(reconciliationUnit);

        ReconciliationRun run = ReconciliationRun.startRun(reconciliationUnit, reconciliationDate, runBy);
        run = reconciliationRunRepository.save(run);

        try {
            ReconciliationSnapshot sourceSnapshot = buildSourceSnapshot(reconciliationUnit, reconciliationDate);
            ReconciliationSnapshot targetSnapshot = buildTargetSnapshot(reconciliationUnit, reconciliationDate);

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

                DifferenceReasonCode defaultReason = differenceReasonCodeRepository.findByCode("GENERIC_MISMATCH")
                        .orElseThrow(() -> new IllegalStateException("Required generic mismatch reason code is missing. Please seed reference data."));

                ReconciliationDifference diff = ReconciliationDifference.createDifference(
                        run,
                        ReconciliationDifference.DifferenceType.AMOUNT_MISMATCH,
                        sourceAmount,
                        targetAmount,
                        unmatchedAmount,
                        reconciliationUnit.getName() + " - amount mismatch (date " + reconciliationDate + ")",
                        buildItemRefJson("SUMMARY", reconciliationDate, reconciliationUnit.getName()),
                        buildItemRefJson("SUMMARY", reconciliationDate, reconciliationUnit.getName()),
                        defaultReason,
                        "SYSTEM"
                );

                if (defaultReason.isAdjustable()) {
                    ReconciliationAdjustmentPolicy.AdjustmentAccountCodes accountCodes = adjustmentPolicy.resolveAdjustmentAccountCodes(reconciliationUnit);
                    // 조정 전표의 멱등 키에 사용할 차이 ID를 확보하기 위해 같은 트랜잭션 안에서 먼저 저장합니다.
                    diff = reconciliationDifferenceRepository.save(diff);

                    Long adjustmentEntryId = createAdjustmentJournalEntry(
                            reconciliationDate,
                            unmatchedAmount,
                            reconciliationUnit.getName() + " reconciliation difference adjustment (" + defaultReason.getName() + ")",
                            accountCodes,
                            "SYSTEM",
                            run,
                            diff
                    );
                    diff.attachAdjustmentJournalEntry(adjustmentEntryId);
                }
                reconciliationDifferenceRepository.save(diff);
            } else {
                matchedAmount = sourceAmount.min(targetAmount);
                matchedCount = Math.min(sourceCount, targetCount);
            }

            run.completeRun(
                    (long) sourceCount, sourceAmount,
                    (long) targetCount, targetAmount,
                    (long) matchedCount, matchedAmount,
                    (long) unmatchedCount, unmatchedAmount
            );

        } catch (Exception e) {
            run.failRun();
            throw new RuntimeException("Reconciliation failed for unit " + unitId, e);
        } finally {
            reconciliationRunRepository.save(run);
        }

        return run;
    }

    private ReconciliationSnapshot buildSourceSnapshot(ReconciliationUnit reconciliationUnit, LocalDate reconciliationDate) {
        // T24 fixed: Load real source snapshots through an outbound port instead of skeleton JSON adapter.
        JsonNode root = parseCriteriaJson(reconciliationUnit);
        com.ho.account.reconciliation.application.port.out.ExternalReconSnapshot snapshot = externalReconSnapshotPort.loadSnapshot(
                com.ho.account.reconciliation.application.port.out.ExternalReconSnapshotRequest.of(
                        String.valueOf(reconciliationUnit.getId()),
                        com.ho.account.reconciliation.application.port.out.ExternalReconSnapshotRequest.SOURCE_STAGE,
                        reconciliationDate,
                        readText(root, "sourceProductCode"),
                        readText(root, "sourceCurrencyCode"),
                        readText(root, "legalEntityCode")
                )
        );

        if (snapshot == null) {
            throw new RuntimeException("External adapter failed to return source snapshot for unit: " + reconciliationUnit.getId());
        }

        return new ReconciliationSnapshot((int) snapshot.count(), snapshot.amount() == null ? BigDecimal.ZERO : snapshot.amount());
    }

    private ReconciliationSnapshot buildTargetSnapshot(ReconciliationUnit reconciliationUnit, LocalDate reconciliationDate) {
        // T25 fixed: Apply account and side filters from the unit policy instead of hardcoded DEBIT.
        JsonNode root = parseCriteriaJson(reconciliationUnit);
        String targetAccountCode = readText(root, "targetAccountCode");
        String sideStr = readText(root, "targetSide");
        JournalSide targetSide = sideStr != null && !sideStr.isBlank() ? JournalSide.valueOf(sideStr.trim().toUpperCase()) : JournalSide.DEBIT;

        JournalDetailAggregateSummary aggregate = journalQueryPort.getJournalDetailAggregateByAccount(
                reconciliationDate,
                reconciliationDate,
                targetSide,
                targetAccountCode);

        if (aggregate == null) {
            return new ReconciliationSnapshot(0, BigDecimal.ZERO);
        }
        long detailCount = aggregate.getDetailCount();
        if (detailCount > Integer.MAX_VALUE) {
            throw new IllegalStateException("Target journal detail count exceeds supported reconciliation count range.");
        }
        BigDecimal totalAmount = aggregate.getTotalAmount() == null ? BigDecimal.ZERO : aggregate.getTotalAmount();
        return new ReconciliationSnapshot((int) detailCount, totalAmount);
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
     * [교육적 주석 - Clean Code 및 안전한 Serialization]
     * JSON 문자열을 직접 하드코딩(String concatenation)하여 생성할 경우,
     * 대사 단위 이름(unitName)이나 날짜 등의 필드에 큰따옴표("), 백슬래시(\), 줄바꿈 등
     * 특수문자가 포함될 때 문법적으로 유효하지 않은 JSON이 생성되거나 파싱 에러(JsonParseException)가 발생할 위험이 있습니다.
     * 
     * 따라서 Jackson의 {@link ObjectMapper}와 데이터 구조(Map)를 활용하여
     * 표준 규격에 맞게 안전하게 JSON으로 직렬화(Serialize)합니다.
     * 
     * @param type 대사 항목 유형 (예: SUMMARY)
     * @param date 대사 기준일
     * @param unitName 대사 단위 명칭
     * @return 직렬화된 JSON 문자열
     */
    private String buildItemRefJson(String type, LocalDate date, String unitName) {
        try {
            java.util.Map<String, String> refMap = java.util.Map.of(
                    "type", type,
                    "date", date != null ? date.toString() : "",
                    "unit", unitName != null ? unitName : ""
            );
            return objectMapper.writeValueAsString(refMap);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize item reference to JSON", e);
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
                                              ReconciliationAdjustmentPolicy.AdjustmentAccountCodes accountCodes,
                                              String createdBy,
                                              ReconciliationRun run,
                                              ReconciliationDifference difference) {
        // T26 fixed: Use policy-driven currency, idempotency key, and reason instead of hardcoded values.
        ReconciliationUnit unit = run.getReconciliationUnit();
        JsonNode root = parseCriteriaJson(unit);
        String currencyCode = readText(root, "adjustmentCurrencyCode");
        currencyCode = currencyCode != null ? currencyCode : "KRW";
        String sourceDocumentId = buildAdjustmentSourceDocumentId(run, difference, accountingDate, amount, accountCodes);

        JournalEntryCommand command = new JournalEntryCommand(
                accountingDate,
                accountingDate,
                description,
                "ADJUSTMENT",
                currencyCode,
                BigDecimal.ONE,
                createdBy,
                createdBy,
                "RECONCILIATION",
                sourceDocumentId,
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

    private String buildAdjustmentSourceDocumentId(ReconciliationRun run,
                                                   ReconciliationDifference difference,
                                                   LocalDate accountingDate,
                                                   BigDecimal amount,
                                                   ReconciliationAdjustmentPolicy.AdjustmentAccountCodes accountCodes) {
        String runKey = run.getId() != null
                ? "RUN-" + run.getId()
                : "UNIT-" + run.getReconciliationUnit().getId() + "-DATE-" + accountingDate;
        String differenceKey = difference.getId() != null
                ? "DIFF-" + difference.getId()
                : "DIFF-" + difference.getDifferenceType() + "-AMOUNT-" + normalizeAmountKey(amount);
        return "RECON_ADJ-" + runKey + "-" + differenceKey
                + "-DR-" + accountCodes.debitAccountCode()
                + "-CR-" + accountCodes.creditAccountCode();
    }

    private String normalizeAmountKey(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        return amount.stripTrailingZeros().toPlainString().replace('.', '_');
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

}
