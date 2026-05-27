package com.ho.account.reconciliation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.contracts.ledger.LedgerBalanceSummary;
import com.ho.account.contracts.ledger.LedgerQueryPort;
import com.ho.account.reconciliation.application.port.out.ExternalReconSnapshot;
import com.ho.account.reconciliation.application.port.out.ExternalReconSnapshotPort;
import com.ho.account.reconciliation.application.port.out.ExternalReconSnapshotRequest;
import com.ho.account.reconciliation.domain.*;
import com.ho.account.reconciliation.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ReconManagerService {

    private final ReconUnitDefinitionRepository unitRepository;
    private final ReconStageResultRepository stageResultRepository;
    private final ReconciliationResultRepository resultRepository;
    private final ReconciliationVarianceRepository varianceRepository;
    private final ExternalReconSnapshotPort externalReconSnapshotPort;
    private final JournalQueryPort journalQueryPort;
    private final LedgerQueryPort ledgerQueryPort;
    private final ObjectMapper objectMapper;

    @Autowired
    public ReconManagerService(ReconUnitDefinitionRepository unitRepository,
            ReconStageResultRepository stageResultRepository,
            ReconciliationResultRepository resultRepository,
            ReconciliationVarianceRepository varianceRepository,
            ExternalReconSnapshotPort externalReconSnapshotPort,
            JournalQueryPort journalQueryPort,
            LedgerQueryPort ledgerQueryPort,
            ObjectMapper objectMapper) {
        this.unitRepository = unitRepository;
        this.stageResultRepository = stageResultRepository;
        this.resultRepository = resultRepository;
        this.varianceRepository = varianceRepository;
        this.externalReconSnapshotPort = externalReconSnapshotPort;
        this.journalQueryPort = journalQueryPort;
        this.ledgerQueryPort = ledgerQueryPort;
        this.objectMapper = objectMapper;
    }

    /**
     * 특정 대사 단위에 대해 4단계 대사를 수행합니다.
     */
    public ReconciliationResult performDeepReconciliation(String unitId, LocalDate reconDate, String runBy) {
        // @todo Domain model boundary: this advanced ReconUnitDefinition/ReconciliationResult flow coexists with ReconciliationUnit/ReconciliationRun; define one aggregate lifecycle.
        ReconUnitDefinition unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new IllegalArgumentException("Unit not found"));

        ReconciliationResult result = new ReconciliationResult();
        result.setReconciliationDate(reconDate);
        result.setReconciliationType(unit.getReconType());
        result.setRunBy(runBy);
        result.setStatus(ReconciliationStatus.IN_PROGRESS);
        result.setTotalCountSource(0L);
        result.setTotalAmountSource(BigDecimal.ZERO);
        result.setTotalCountTarget(0L);
        result.setTotalAmountTarget(BigDecimal.ZERO);
        result.setVarianceCount(0L);
        result.setVarianceAmount(BigDecimal.ZERO);
        result.setAuditUser(runBy);
        ReconciliationResult savedResult = resultRepository.save(result);

        // 1. 단계별 집계 (Source -> Interface -> Journal -> Ledger)
        List<ReconStageResult> stages = new ArrayList<>();
        StageSnapshot sourceSnapshot = buildExternalSnapshot(unit, ExternalReconSnapshotRequest.SOURCE_STAGE, reconDate);
        StageSnapshot interfaceSnapshot = buildExternalSnapshot(unit, ExternalReconSnapshotRequest.INTERFACE_STAGE, reconDate);
        StageSnapshot journalSnapshot = buildJournalSnapshot(unit, reconDate);
        StageSnapshot ledgerSnapshot = buildLedgerSnapshot(unit, reconDate);

        stages.add(createStageResult(savedResult, "SOURCE", sourceSnapshot));
        stages.add(createStageResult(savedResult, "INTERFACE", interfaceSnapshot));
        stages.add(createStageResult(savedResult, "JOURNAL", journalSnapshot));
        stages.add(createStageResult(savedResult, "LEDGER", ledgerSnapshot));

        stageResultRepository.saveAll(stages);

        // 2. 차이 분석 및 결과 업데이트
        BigDecimal sourceAmt = stages.get(0).getTotalAmount();
        BigDecimal ledgerAmt = stages.get(3).getTotalAmount();
        BigDecimal diff = sourceAmt.subtract(ledgerAmt).abs();

        savedResult.setTotalAmountSource(sourceAmt);
        savedResult.setTotalCountSource(sourceSnapshot.count());
        savedResult.setTotalAmountTarget(ledgerAmt);
        savedResult.setTotalCountTarget(ledgerSnapshot.count());
        savedResult.setVarianceAmount(diff);
        savedResult.setVarianceCount(diff.compareTo(BigDecimal.ZERO) == 0 ? 0L : 1L);

        BigDecimal toleranceAmount = unit.getToleranceAmount() == null ? BigDecimal.ZERO : unit.getToleranceAmount();
        if (diff.compareTo(toleranceAmount) <= 0) {
            savedResult.setStatus(ReconciliationStatus.SUCCESS);
        } else {
            savedResult.setStatus(ReconciliationStatus.VARIANCE_FOUND);
            createVariance(savedResult, unit, "DEEP_MISMATCH", "Source to Ledger mismatch: " + diff, diff);
        }

        return resultRepository.save(savedResult);
    }

    private ReconStageResult createStageResult(ReconciliationResult result, String stage, StageSnapshot snapshot) {
        ReconStageResult sr = new ReconStageResult();
        sr.setReconciliationResult(result);
        sr.setStageCode(stage);
        sr.setTotalCount(snapshot.count());
        sr.setTotalAmount(snapshot.amount());
        return sr;
    }

    private void createVariance(ReconciliationResult result, ReconUnitDefinition unit, String code, String desc,
            BigDecimal amt) {
        ReconciliationVariance v = new ReconciliationVariance();
        v.setReconciliationResult(result);
        v.setVarianceCode(code);
        v.setDescription(desc);
        v.setAmount(amt);
        v.setStatus(VarianceStatus.OPEN);
        // @todo SLA policy: default SLA days must come from reconciliation policy/master data, not a hidden service-level fallback.
        int slaDays = unit.getSlaDays() == null ? 3 : unit.getSlaDays();
        v.setSlaDueDate(LocalDate.now().plusDays(slaDays));
        varianceRepository.save(v);
    }

    private StageSnapshot buildExternalSnapshot(ReconUnitDefinition unit, String stageCode, LocalDate date) {
        ExternalReconSnapshot snapshot = externalReconSnapshotPort.loadSnapshot(ExternalReconSnapshotRequest.of(
                unit.getUnitId(),
                stageCode,
                date,
                unit.getProductCode(),
                unit.getCurrencyCode(),
                unit.getLegalEntityCode()
        ));
        if (snapshot == null) {
            // @todo Integration consistency: distinguish "no source rows" from adapter failure; returning zero can hide broken upstream feeds.
            return new StageSnapshot(0L, BigDecimal.ZERO);
        }
        return new StageSnapshot(snapshot.count(), safe(snapshot.amount()));
    }

    private StageSnapshot buildJournalSnapshot(ReconUnitDefinition unit, LocalDate date) {
        String accountCode = readText(parseMatchingRules(unit), "journalAccountCode");

        // DB 직접 집계 호출 (대용량 성능 최적화)
        com.ho.account.contracts.journal.JournalDetailAggregateSummary aggregate = 
                journalQueryPort.getJournalDetailAggregateByAccount(date, date, com.ho.account.contracts.journal.JournalSide.DEBIT, accountCode);

        if (aggregate == null) {
            return new StageSnapshot(0L, BigDecimal.ZERO);
        }

        return new StageSnapshot(aggregate.getDetailCount(), safe(aggregate.getTotalAmount()));
    }

    private StageSnapshot buildLedgerSnapshot(ReconUnitDefinition unit, LocalDate date) {
        JsonNode rules = parseMatchingRules(unit);
        String accountCode = readText(rules, "ledgerAccountCode");
        String currencyCode = readText(rules, "ledgerCurrencyCode");
        String amountBasis = readText(rules, "ledgerAmountBasis");

        BigDecimal amount = BigDecimal.ZERO;
        long count = 0L;
        for (LedgerBalanceSummary summary : ledgerQueryPort.getGlBalanceSummaries(date, date, accountCode, currencyCode)) {
            amount = amount.add(resolveLedgerAmount(summary, amountBasis));
            count++;
        }

        return new StageSnapshot(count, amount);
    }

    private BigDecimal resolveJournalAmount(JournalDetailSummary detail) {
        if (detail.getBaseAmount() != null) {
            return detail.getBaseAmount();
        }
        if (detail.getAmount() != null) {
            return detail.getAmount();
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal resolveLedgerAmount(LedgerBalanceSummary summary, String amountBasis) {
        String basis = amountBasis == null ? "DEBIT" : amountBasis.trim().toUpperCase();
        return switch (basis) {
            case "CREDIT" -> safe(summary.getCreditAmount());
            case "ENDING_BALANCE" -> safe(summary.getEndingBalance());
            case "ABS_ENDING_BALANCE" -> safe(summary.getEndingBalance()).abs();
            default -> safe(summary.getDebitAmount());
        };
    }

    private boolean matchesAccount(String actualAccountCode, String expectedAccountCode) {
        return expectedAccountCode == null || expectedAccountCode.equals(actualAccountCode);
    }

    private JsonNode parseMatchingRules(ReconUnitDefinition unit) {
        // @todo DDD hardening: replace untyped matchingRulesJson keys with a validated policy object so account/currency/basis errors fail early.
        String matchingRulesJson = unit.getMatchingRulesJson();
        if (matchingRulesJson == null || matchingRulesJson.isBlank()) {
            return objectMapper.createObjectNode();
        }

        try {
            return objectMapper.readTree(matchingRulesJson);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid matchingRulesJson for unit " + unit.getUnitId(), e);
        }
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

    private BigDecimal safe(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private record StageSnapshot(long count, BigDecimal amount) {
    }
}
