package com.ho.account.reconciliation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.contracts.journal.JournalDetailAggregateSummary;
import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.contracts.journal.JournalSide;
import com.ho.account.reconciliation.application.port.in.AssignDifferenceCommand;
import com.ho.account.reconciliation.application.port.in.ResolveDifferenceCommand;
import com.ho.account.reconciliation.application.port.in.RunReconciliationCommand;
import com.ho.account.reconciliation.application.port.out.ExternalReconSnapshotPort;
import com.ho.account.reconciliation.domain.*;
import com.ho.account.reconciliation.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * [단일 책임 원칙 (SRP) - 대사 실행 및 오케스트레이션 서비스]
 * 원천/대상 데이터 수집, N:M 매칭 알고리즘 실행, 대사 차액(Difference) 산출 및 담당자 배정/해결 조율,
 * 대사 실행 이력(Run) 저장을 전담하는 오케스트레이션 서비스입니다.
 * 
 * 🐣 [초보자를 위한 설명: Service Orchestration & Rich Domain Pattern]
 * 대사 실행은 여러 외부 시스템과 데이터베이스 포트들이 복합적으로 협력하는 핵심 작업입니다.
 * 이 클래스는 대사 작업의 '현장 지휘관'으로서 다음 흐름을 총괄합니다:
 * 
 * 1. **외부/원장 데이터 스냅샷 조회**: {@link ExternalReconSnapshotPort}와 {@link JournalQueryPort}를 연동하여 원천/대상 데이터를 수집.
 * 2. **매칭 엔진 연동**: {@link ReconciliationMatchingEngine}을 통해 항목 단위 매칭 수행.
 * 3. **차액 자동 판정 및 조정 전표 처리**: 허용오차 초과 시 {@link ReconciliationDifference}를 생성하고 필요 시 자동 조정 전표 생성.
 * 4. **차액 배정 및 해결**: 대사 차액에 대한 담당자 지정 및 사유 결정을 통한 최종 해결(Resolve) 프로세스 조율.
 * 
 * [단일 책임 원칙(SRP) 적용 이점]
 * - 단순 CRUD 로직({@link ReconciliationUnitService}, {@link ReconciliationRuleService})을 분리함으로써,
 *   복잡한 대사 수집/매칭/전표 발행 오케스트레이션 로직만 이 클래스에 명확히 캡슐화되었습니다.
 */
@Service
@Transactional
public class ReconciliationExecutionService {

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
    private final ReconciliationMatchingEngine matchingEngine;

    @Autowired
    public ReconciliationExecutionService(ReconciliationUnitRepository reconciliationUnitRepository,
                                         ReconciliationRuleRepository reconciliationRuleRepository,
                                         DifferenceReasonCodeRepository differenceReasonCodeRepository,
                                         ReconciliationRunRepository reconciliationRunRepository,
                                         ReconciliationDifferenceRepository reconciliationDifferenceRepository,
                                         JournalQueryPort journalQueryPort,
                                         JournalPostingPort journalPostingPort,
                                         ObjectMapper objectMapper,
                                         ReconciliationAdjustmentPolicy adjustmentPolicy,
                                         ExternalReconSnapshotPort externalReconSnapshotPort,
                                         ReconciliationMatchingEngine matchingEngine) {
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
        this.matchingEngine = matchingEngine != null ? matchingEngine : new ReconciliationMatchingEngine(new ItemLevelMatcher());
    }

    public ReconciliationExecutionService(ReconciliationUnitRepository reconciliationUnitRepository,
                                         ReconciliationRuleRepository reconciliationRuleRepository,
                                         DifferenceReasonCodeRepository differenceReasonCodeRepository,
                                         ReconciliationRunRepository reconciliationRunRepository,
                                         ReconciliationDifferenceRepository reconciliationDifferenceRepository,
                                         JournalQueryPort journalQueryPort,
                                         JournalPostingPort journalPostingPort,
                                         ObjectMapper objectMapper,
                                         ReconciliationAdjustmentPolicy adjustmentPolicy,
                                         ExternalReconSnapshotPort externalReconSnapshotPort) {
        this(reconciliationUnitRepository, reconciliationRuleRepository, differenceReasonCodeRepository,
             reconciliationRunRepository, reconciliationDifferenceRepository, journalQueryPort,
             journalPostingPort, objectMapper, adjustmentPolicy, externalReconSnapshotPort,
             new ReconciliationMatchingEngine(new ItemLevelMatcher()));
    }

    /**
     * 대사 차액에 담당자를 배정하고 SLA 만료일을 설정합니다.
     *
     * @param command 담당자와 SLA를 포함한 차이 배정 커맨드
     * @return 배정 업데이트된 차액 엔티티
     * @throws EntityNotFoundException 해당 차액이 존재하지 않는 경우 발생
     */
    public ReconciliationDifference assignDifference(AssignDifferenceCommand command) {
        ReconciliationDifference difference = reconciliationDifferenceRepository.findById(command.differenceId())
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationDifference not found with id: " + command.differenceId()));

        difference.assignOwner(command.assignedToUser(), command.slaDueDate());
        return reconciliationDifferenceRepository.save(difference);
    }

    /**
     * 대사 차액을 최종 해결(RESOLVED) 또는 무시(IGNORED) 상태로 종결합니다.
     * 사유 코드가 조정 전표 연계가 필요한 경우(adjustable) 조정 전표 ID 검증 후 완료 처리됩니다.
     *
     * @param command 해결 사유, 전표 ID, 담당자 정보 커맨드
     * @return 해결 처리된 차액 엔티티
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
     * 특정 대사 실행(ReconciliationRun)에속한 차액 목록을 조회합니다.
     *
     * @param reconciliationRunId 대사 실행 ID
     * @return 차액 목록
     */
    @Transactional(readOnly = true)
    public List<ReconciliationDifference> findDifferencesByReconciliationRunId(Long reconciliationRunId) {
        ReconciliationRun run = reconciliationRunRepository.findById(reconciliationRunId)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationRun not found with id: " + reconciliationRunId));
        return reconciliationDifferenceRepository.findByReconciliationRun(run);
    }

    /**
     * 지정된 대사 단위와 기준일에 대해 대사 작업을 실행합니다.
     * 항목 수준(Item-Level) N:M 매칭 알고리즘 엔진을 통해 원천과 대상 데이터 건별 대사를 수행합니다.
     *
     * @param command 대사 단위 ID, 기준일, 실행자 정보 커맨드
     * @return 완료/실패 처리된 대사 실행 엔티티
     */
    public ReconciliationRun performReconciliation(RunReconciliationCommand command) {
        Long unitId = command.reconciliationUnitId();
        LocalDate reconciliationDate = command.reconciliationDate();
        String runBy = command.runBy();
        ReconciliationUnit reconciliationUnit = reconciliationUnitRepository.findById(unitId)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationUnit not found with id: " + unitId));

        // 1. Idempotency Guard: Check if a completed run or run with posted adjustments already exists for the given unit and date
        List<ReconciliationRun> existingRuns = reconciliationRunRepository.findByReconciliationUnitAndReconciliationDate(reconciliationUnit, reconciliationDate);
        Optional<ReconciliationRun> alreadyCompletedRun = existingRuns.stream()
                .filter(this::isRunCompletedOrAdjusted)
                .reduce((first, second) -> second);

        if (alreadyCompletedRun.isPresent()) {
            return alreadyCompletedRun.get();
        }

        List<ReconciliationRule> rules = reconciliationRuleRepository.findByReconciliationUnitOrderByPriorityAsc(reconciliationUnit);

        ReconciliationRun run = ReconciliationRun.startRun(reconciliationUnit, reconciliationDate, runBy);
        run = reconciliationRunRepository.save(run);

        try {
            ReconciliationSnapshot sourceSnapshot = buildSourceSnapshot(reconciliationUnit, reconciliationDate);
            ReconciliationSnapshot targetSnapshot = buildTargetSnapshot(reconciliationUnit, reconciliationDate);

            List<ReconciliationItem> sourceItems = buildSourceItems(reconciliationUnit, reconciliationDate, sourceSnapshot);
            List<ReconciliationItem> targetItems = buildTargetItems(reconciliationUnit, reconciliationDate, targetSnapshot);

            BigDecimal sourceAmount = sourceSnapshot.amount();
            BigDecimal amountTolerance = tolerancePolicy.resolveAmountTolerance(rules, sourceAmount);

            ReconciliationMatchingEngine.ExecutionResult result = matchingEngine.matchItems(sourceItems, targetItems, amountTolerance);

            if (!result.discrepancyGroups().isEmpty()) {
                DifferenceReasonCode defaultReason = differenceReasonCodeRepository.findByCode("GENERIC_MISMATCH")
                        .orElseThrow(() -> new IllegalStateException("Required generic mismatch reason code is missing. Please seed reference data."));

                boolean hasExistingAdjustmentForPeriod = existingRuns.stream().anyMatch(this::hasPostedAdjustments);

                for (ItemLevelMatcher.ItemMatchGroup discrepancy : result.discrepancyGroups()) {
                    ReconciliationDifference.DifferenceType diffType = switch (discrepancy.matchType()) {
                        case MISSING_TARGET -> ReconciliationDifference.DifferenceType.MISSING_TARGET;
                        case MISSING_SOURCE -> ReconciliationDifference.DifferenceType.MISSING_SOURCE;
                        default -> ReconciliationDifference.DifferenceType.AMOUNT_MISMATCH;
                    };

                    String sourceRef = buildItemRefJson(discrepancy.sourceItems(), reconciliationDate, reconciliationUnit.getName());
                    String targetRef = buildItemRefJson(discrepancy.targetItems(), reconciliationDate, reconciliationUnit.getName());

                    ReconciliationDifference diff = ReconciliationDifference.createDifference(
                            run,
                            diffType,
                            discrepancy.sourceTotalAmount(),
                            discrepancy.targetTotalAmount(),
                            discrepancy.differenceAmount(),
                            reconciliationUnit.getName() + " - " + discrepancy.matchReason() + " (date " + reconciliationDate + ")",
                            sourceRef,
                            targetRef,
                            defaultReason,
                            "SYSTEM"
                    );

                    if (defaultReason.isAdjustable() && discrepancy.differenceAmount().compareTo(BigDecimal.ZERO) > 0) {
                        if (adjustmentPolicy.canGenerateAdjustment(hasExistingAdjustmentForPeriod)) {
                            ReconciliationAdjustmentPolicy.AdjustmentAccountCodes accountCodes = adjustmentPolicy.resolveAdjustmentAccountCodes(reconciliationUnit);
                            diff = reconciliationDifferenceRepository.save(diff);

                            Long adjustmentEntryId = createAdjustmentJournalEntry(
                                    reconciliationDate,
                                    discrepancy.differenceAmount(),
                                    reconciliationUnit.getName() + " reconciliation difference adjustment (" + defaultReason.getName() + ")",
                                    accountCodes,
                                    "SYSTEM",
                                    run,
                                    diff
                            );
                            diff.attachAdjustmentJournalEntry(adjustmentEntryId);
                            hasExistingAdjustmentForPeriod = true;
                        }
                    }
                    reconciliationDifferenceRepository.save(diff);
                }
            }

            run.completeRun(
                    sourceSnapshot.count(), sourceSnapshot.amount(),
                    targetSnapshot.count(), targetSnapshot.amount(),
                    result.matchedItemsCount(), result.matchedAmount(),
                    result.unmatchedItemsCount(), result.unmatchedAmount()
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
        JsonNode root = parseCriteriaJson(reconciliationUnit);
        String targetAccountCode = readText(root, "targetAccountCode");
        JournalSide targetSide = readTargetSide(root);

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

    private String readText(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return "";
        }
        String text = node.asText();
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.trim();
    }

    private JournalSide readTargetSide(JsonNode root) {
        String side = readText(root, "targetSide");
        return side.isBlank() ? JournalSide.DEBIT : JournalSide.valueOf(side.toUpperCase());
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

    private List<ReconciliationItem> buildSourceItems(ReconciliationUnit reconciliationUnit, LocalDate reconciliationDate, ReconciliationSnapshot snapshot) {
        JsonNode root = parseCriteriaJson(reconciliationUnit);
        List<ReconciliationItem> items = externalReconSnapshotPort.loadItems(
                com.ho.account.reconciliation.application.port.out.ExternalReconSnapshotRequest.of(
                        String.valueOf(reconciliationUnit.getId()),
                        com.ho.account.reconciliation.application.port.out.ExternalReconSnapshotRequest.SOURCE_STAGE,
                        reconciliationDate,
                        readText(root, "sourceProductCode"),
                        readText(root, "sourceCurrencyCode"),
                        readText(root, "legalEntityCode")
                )
        );
        List<ReconciliationItem> sourceItems = items == null ? List.of() : items;
        BigDecimal sourceAmount = sourceItems.stream()
                .map(ReconciliationItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Matching must use the same complete source population represented by the snapshot.
        if (sourceItems.size() != snapshot.count() || sourceAmount.compareTo(snapshot.amount()) != 0) {
            throw new IllegalStateException("Source reconciliation item count or amount differs from snapshot for unit "
                    + reconciliationUnit.getId() + " on " + reconciliationDate);
        }
        return sourceItems;
    }

    private List<ReconciliationItem> buildTargetItems(ReconciliationUnit reconciliationUnit, LocalDate reconciliationDate, ReconciliationSnapshot snapshot) {
        JsonNode root = parseCriteriaJson(reconciliationUnit);
        String targetAccountCode = readText(root, "targetAccountCode");
        JournalSide targetSide = readTargetSide(root);
        List<String> accountCodes = !targetAccountCode.isBlank() ? List.of(targetAccountCode) : List.of();
        List<JournalDetailSummary> details = journalQueryPort.getJournalDetailsByAccountCodes(
                reconciliationDate, reconciliationDate, accountCodes
        );
        // The detail port selects accounts only; apply the aggregate's side before matching.
        List<JournalDetailSummary> selectedDetails = details == null ? List.of() : details.stream()
                .filter(d -> d.getSide() == targetSide)
                .toList();
        BigDecimal selectedAmount = selectedDetails.stream()
                .map(d -> d.getBaseAmount() != null ? d.getBaseAmount() : d.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (selectedDetails.size() != snapshot.count() || selectedAmount.compareTo(snapshot.amount()) != 0) {
            throw new IllegalStateException("Target journal detail count or amount differs from aggregate for reconciliation unit "
                    + reconciliationUnit.getId() + " on " + reconciliationDate + " (side " + targetSide + ")");
        }
        return selectedDetails.stream()
                .map(d -> ReconciliationItem.ofTarget(
                        String.valueOf(d.getId()),
                        d.getAccountingDate(),
                        d.getSlipNo(),
                        d.getBusinessPartnerCode(),
                        d.getAccountCode(),
                        d.getBaseAmount() != null ? d.getBaseAmount() : d.getAmount(),
                        d.getDetailDescription()
                ))
                .collect(Collectors.toList());
    }

    private String buildItemRefJson(List<ReconciliationItem> items, LocalDate date, String unitName) {
        if (items == null || items.isEmpty()) {
            return buildItemRefJson("SUMMARY", date, unitName);
        }
        if (items.size() == 1 && (items.get(0).getId().contains("SUMMARY") || "SUMMARY".equals(items.get(0).getReferenceId()))) {
            return buildItemRefJson("SUMMARY", date, unitName);
        }
        try {
            List<java.util.Map<String, String>> refList = items.stream().map(item -> java.util.Map.of(
                    "type", item.getSide().name(),
                    "id", item.getId() != null ? item.getId() : "",
                    "refId", item.getReferenceId() != null ? item.getReferenceId() : "",
                    "partner", item.getPartnerCode() != null ? item.getPartnerCode() : "",
                    "account", item.getAccountCode() != null ? item.getAccountCode() : "",
                    "amount", item.getAmount() != null ? item.getAmount().toPlainString() : "0",
                    "date", item.getTransactionDate() != null ? item.getTransactionDate().toString() : (date != null ? date.toString() : ""),
                    "unit", unitName != null ? unitName : ""
            )).collect(Collectors.toList());
            return objectMapper.writeValueAsString(refList.size() == 1 ? refList.get(0) : refList);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize item reference to JSON", e);
        }
    }

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

    private Long createAdjustmentJournalEntry(LocalDate accountingDate, BigDecimal amount, String description,
                                              ReconciliationAdjustmentPolicy.AdjustmentAccountCodes accountCodes,
                                              String createdBy,
                                              ReconciliationRun run,
                                              ReconciliationDifference difference) {
        ReconciliationUnit unit = run.getReconciliationUnit();
        JsonNode root = parseCriteriaJson(unit);
        String currencyCode = readText(root, "adjustmentCurrencyCode");
        currencyCode = (currencyCode != null && !currencyCode.isBlank()) ? currencyCode : "KRW";
        String sourceDocumentId = adjustmentPolicy.buildAdjustmentSourceDocumentId(run, difference, accountingDate, amount, accountCodes);

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

    private boolean isRunCompletedOrAdjusted(ReconciliationRun run) {
        if (run == null) {
            return false;
        }
        if (run.getStatus() == ReconciliationRun.ReconciliationRunStatus.SUCCESS
                || run.getStatus() == ReconciliationRun.ReconciliationRunStatus.PARTIAL) {
            return true;
        }
        return hasPostedAdjustments(run);
    }

    private boolean hasPostedAdjustments(ReconciliationRun run) {
        if (run == null || run.getId() == null) {
            return false;
        }
        List<ReconciliationDifference> differences = reconciliationDifferenceRepository.findByReconciliationRun(run);
        return differences.stream().anyMatch(d -> d.getAdjustmentJournalEntryId() != null);
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
