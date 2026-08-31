package com.ho.account.reconciliation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.contracts.journal.JournalSide;
import com.ho.account.contracts.ledger.LedgerBalanceSummary;
import com.ho.account.contracts.ledger.LedgerQueryPort;
import com.ho.account.reconciliation.application.port.out.ExternalReconSnapshot;
import com.ho.account.reconciliation.application.port.out.ExternalReconSnapshotPort;
import com.ho.account.reconciliation.application.port.out.ExternalReconSnapshotRequest;
import com.ho.account.reconciliation.domain.ReconciliationDifference;
import com.ho.account.reconciliation.domain.ReconciliationRun;
import com.ho.account.reconciliation.domain.ReconciliationStageResult;
import com.ho.account.reconciliation.domain.ReconciliationUnit;
import com.ho.account.reconciliation.repository.ReconciliationDifferenceRepository;
import com.ho.account.reconciliation.repository.ReconciliationRunRepository;
import com.ho.account.reconciliation.repository.ReconciliationStageResultRepository;
import com.ho.account.reconciliation.repository.ReconciliationUnitRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 원천 → 인터페이스 → 전표 → 원장의 4단계 Deep reconciliation 서비스.
 *
 * <p>표준 aggregate 생명주기는 {@code ReconciliationUnit -> ReconciliationRun ->
 * ReconciliationDifference} 하나입니다. 단계별 결과는 Run의 진단 정보로만 저장되고, 차이의 담당자 지정,
 * 원인 분석, 조정 전표 연결은 표준 Difference 흐름에서 계속 처리됩니다.
 */
@Service
@Transactional
public class ReconManagerService {

    private final ReconciliationUnitRepository unitRepository;
    private final ReconciliationRunRepository runRepository;
    private final ReconciliationDifferenceRepository differenceRepository;
    private final ReconciliationStageResultRepository stageResultRepository;
    private final ExternalReconSnapshotPort externalReconSnapshotPort;
    private final JournalQueryPort journalQueryPort;
    private final LedgerQueryPort ledgerQueryPort;
    private final ObjectMapper objectMapper;

    public ReconManagerService(
            ReconciliationUnitRepository unitRepository,
            ReconciliationRunRepository runRepository,
            ReconciliationDifferenceRepository differenceRepository,
            ReconciliationStageResultRepository stageResultRepository,
            ExternalReconSnapshotPort externalReconSnapshotPort,
            JournalQueryPort journalQueryPort,
            LedgerQueryPort ledgerQueryPort,
            ObjectMapper objectMapper) {
        this.unitRepository = unitRepository;
        this.runRepository = runRepository;
        this.differenceRepository = differenceRepository;
        this.stageResultRepository = stageResultRepository;
        this.externalReconSnapshotPort = externalReconSnapshotPort;
        this.journalQueryPort = journalQueryPort;
        this.ledgerQueryPort = ledgerQueryPort;
        this.objectMapper = objectMapper;
    }

    public ReconciliationRun performDeepReconciliation(Long unitId, LocalDate reconDate, String runBy) {
        ReconciliationUnit unit = unitRepository.findById(unitId)
                .filter(ReconciliationUnit::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Active reconciliation unit not found: " + unitId));
        DeepReconciliationPolicy policy = parsePolicy(unit);

        ReconciliationRun run = new ReconciliationRun();
        run.setReconciliationUnit(unit);
        run.setReconciliationDate(reconDate);
        run.setRunStartTime(LocalDateTime.now());
        run.setStatus(ReconciliationRun.ReconciliationRunStatus.RUNNING);
        run.setRunBy(requireText(runBy, "runBy"));
        run.setAuditUser(run.getRunBy());
        ReconciliationRun savedRun = runRepository.save(run);

        StageSnapshot source = buildExternalSnapshot(unit, policy, ExternalReconSnapshotRequest.SOURCE_STAGE, reconDate);
        StageSnapshot interfaceStage = buildExternalSnapshot(
                unit, policy, ExternalReconSnapshotRequest.INTERFACE_STAGE, reconDate);
        StageSnapshot journal = buildJournalSnapshot(policy, reconDate);
        StageSnapshot ledger = buildLedgerSnapshot(policy, reconDate);
        stageResultRepository.saveAll(List.of(
                createStageResult(savedRun, "SOURCE", source),
                createStageResult(savedRun, "INTERFACE", interfaceStage),
                createStageResult(savedRun, "JOURNAL", journal),
                createStageResult(savedRun, "LEDGER", ledger)));

        BigDecimal differenceAmount = source.amount().subtract(ledger.amount()).abs();
        savedRun.setTotalItemsSource(source.count());
        savedRun.setTotalAmountSource(source.amount());
        savedRun.setTotalItemsTarget(ledger.count());
        savedRun.setTotalAmountTarget(ledger.amount());
        savedRun.setMatchedItemsCount(Math.min(source.count(), ledger.count()));
        savedRun.setMatchedAmount(source.amount().min(ledger.amount()));
        savedRun.setUnmatchedItemsCount(differenceAmount.signum() == 0 ? 0L : 1L);
        savedRun.setUnmatchedAmount(differenceAmount);
        savedRun.setRunEndTime(LocalDateTime.now());

        if (differenceAmount.compareTo(policy.toleranceAmount()) <= 0) {
            savedRun.setStatus(ReconciliationRun.ReconciliationRunStatus.SUCCESS);
        } else {
            savedRun.setStatus(ReconciliationRun.ReconciliationRunStatus.PARTIAL);
            differenceRepository.save(createDifference(savedRun, reconDate, policy, source.amount(), ledger.amount()));
        }
        return runRepository.save(savedRun);
    }

    private ReconciliationStageResult createStageResult(
            ReconciliationRun run, String stageCode, StageSnapshot snapshot) {
        ReconciliationStageResult result = new ReconciliationStageResult();
        result.setReconciliationRun(run);
        result.setStageCode(stageCode);
        result.setTotalCount(snapshot.count());
        result.setTotalAmount(snapshot.amount());
        result.setAuditUser(run.getRunBy());
        return result;
    }

    private ReconciliationDifference createDifference(
            ReconciliationRun run,
            LocalDate reconDate,
            DeepReconciliationPolicy policy,
            BigDecimal expected,
            BigDecimal actual) {
        ReconciliationDifference difference = new ReconciliationDifference();
        difference.setReconciliationRun(run);
        difference.setDifferenceType(ReconciliationDifference.DifferenceType.AMOUNT_MISMATCH);
        difference.setAmountExpected(expected);
        difference.setAmountActual(actual);
        difference.setDifferenceAmount(expected.subtract(actual).abs());
        difference.setDescription("Deep reconciliation source-to-ledger mismatch");
        difference.setSourceItemRef("{\"stage\":\"SOURCE\"}");
        difference.setTargetItemRef("{\"stage\":\"LEDGER\"}");
        difference.setStatus(ReconciliationDifference.ReconciliationDifferenceStatus.PENDING);
        difference.setSlaDueDate(reconDate.plusDays(policy.slaDays()).atStartOfDay());
        difference.setAuditUser(run.getRunBy());
        return difference;
    }

    private StageSnapshot buildExternalSnapshot(
            ReconciliationUnit unit, DeepReconciliationPolicy policy, String stageCode, LocalDate date) {
        ExternalReconSnapshot snapshot = externalReconSnapshotPort.loadSnapshot(ExternalReconSnapshotRequest.of(
                unit.getId().toString(),
                stageCode,
                date,
                policy.productCode(),
                policy.currencyCode(),
                policy.legalEntityCode()));
        if (snapshot == null) {
            throw new IllegalStateException("External adapter failed for stage " + stageCode + " on " + date);
        }
        return new StageSnapshot(snapshot.count(), safe(snapshot.amount()));
    }

    private StageSnapshot buildJournalSnapshot(DeepReconciliationPolicy policy, LocalDate date) {
        var aggregate = journalQueryPort.getJournalDetailAggregateByAccount(
                date, date, JournalSide.DEBIT, policy.journalAccountCode());
        return aggregate == null
                ? new StageSnapshot(0L, BigDecimal.ZERO)
                : new StageSnapshot(aggregate.getDetailCount(), safe(aggregate.getTotalAmount()));
    }

    /**
     * DB 레벨 집계(Push-Down Aggregation)를 통해 원장 잔액 스냅샷을 생성합니다.
     *
     * <p><b>[대규모 데이터 처리 시 OOM 예방 및 푸시다운 집계(Push-Down Aggregation) Rationale]</b></p>
     * <ul>
     *   <li><b>기존 문제점 (In-Memory Processing):</b> 대량의 원장/거래 전표 엔티티 전체 리스트를 JVM Heap 메모리에
     *       적재한 뒤 절차적인 for-loop로 합계를 계산했습니다. 원장 레코드가 수십만~수백만 건으로 늘어날 경우 JVM Memory Footprint가
     *       폭발적으로 증가하여 Heap Out-Of-Memory(OOM)가 발생할 수 있습니다.</li>
     *   <li><b>개선 방안 (Push-Down Aggregation):</b> 집계 연산({@code SUM(amount)}, {@code COUNT(*)}) 로직을
     *       데이터가 존재하는 RDBMS Query Engine 수준으로 밀어넣어(Push-down) 처리합니다.</li>
     *   <li><b>성능적/아키텍처적 이점:</b>
     *       1) 네트워크 I/O 패킷량 절감: 수만 건의 레코드가 네트워크를 타고 오지 않고 최종 1건의 요약 데이터만 수신합니다.<br>
     *       2) 메모리 풋프린트 관리: JVM Heap 상에 개별 객체 인스턴스를 생성하지 않으므로 GC 부담과 Memory Footprint가 O(1)에 수렴합니다.<br>
     *       3) DB 쿼리 최적화: RDBMS의 B-Tree 인덱스 scan 및 데이터베이스 집계 최적화 엔진을 최대한 활용합니다.</li>
     * </ul>
     */
    private StageSnapshot buildLedgerSnapshot(DeepReconciliationPolicy policy, LocalDate date) {
        var aggregate = ledgerQueryPort.calculateLedgerSummary(
                date, date, policy.ledgerAccountCode(), policy.ledgerCurrencyCode(), policy.ledgerAmountBasis().name());
        return aggregate == null
                ? new StageSnapshot(0L, BigDecimal.ZERO)
                : new StageSnapshot(aggregate.getCount(), safe(aggregate.getTotalAmount()));
    }

    private DeepReconciliationPolicy parsePolicy(ReconciliationUnit unit) {
        try {
            JsonNode root = objectMapper.readTree(requireText(unit.getCriteriaJson(), "criteriaJson"));
            return new DeepReconciliationPolicy(
                    readText(root, "productCode"),
                    readText(root, "currencyCode"),
                    readText(root, "legalEntityCode"),
                    requireText(root, "journalAccountCode", unit),
                    requireText(root, "ledgerAccountCode", unit),
                    readText(root, "ledgerCurrencyCode"),
                    LedgerAmountBasis.from(readText(root, "ledgerAmountBasis")),
                    readDecimal(root, "toleranceAmount", BigDecimal.ZERO),
                    readPositiveInt(root, "slaDays"));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid criteriaJson for unit " + unit.getId(), e);
        }
    }

    private String requireText(JsonNode root, String field, ReconciliationUnit unit) {
        String value = readText(root, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " is required for unit " + unit.getId());
        return value;
    }

    private String readText(JsonNode root, String field) {
        JsonNode value = root.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? "" : value.asText().trim();
    }

    private BigDecimal readDecimal(JsonNode root, String field, BigDecimal defaultValue) {
        JsonNode value = root.get(field);
        return value == null || value.isNull() ? defaultValue : value.decimalValue();
    }

    private int readPositiveInt(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || !value.canConvertToInt() || value.asInt() < 1) {
            throw new IllegalArgumentException("Positive " + field + " is required.");
        }
        return value.asInt();
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required.");
        return value.trim();
    }

    private BigDecimal safe(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private record StageSnapshot(long count, BigDecimal amount) {
    }

    private record DeepReconciliationPolicy(
            String productCode,
            String currencyCode,
            String legalEntityCode,
            String journalAccountCode,
            String ledgerAccountCode,
            String ledgerCurrencyCode,
            LedgerAmountBasis ledgerAmountBasis,
            BigDecimal toleranceAmount,
            int slaDays) {
    }

    private enum LedgerAmountBasis {
        DEBIT, CREDIT, ENDING_BALANCE, ABS_ENDING_BALANCE;

        private static LedgerAmountBasis from(String value) {
            if (value == null || value.isBlank()) return DEBIT;
            try {
                return valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unsupported ledgerAmountBasis: " + value, e);
            }
        }
    }
}
