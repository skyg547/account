package com.ho.account.reconciliation.service;

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

    @Autowired
    public ReconManagerService(ReconUnitDefinitionRepository unitRepository,
            ReconStageResultRepository stageResultRepository,
            ReconciliationResultRepository resultRepository,
            ReconciliationVarianceRepository varianceRepository) {
        this.unitRepository = unitRepository;
        this.stageResultRepository = stageResultRepository;
        this.resultRepository = resultRepository;
        this.varianceRepository = varianceRepository;
    }

    /**
     * 특정 대사 단위에 대해 4단계 대사를 수행합니다.
     */
    public ReconciliationResult performDeepReconciliation(String unitId, LocalDate reconDate, String runBy) {
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
        stages.add(createStageResult(savedResult, "SOURCE", fetchSourceAmount(unit, reconDate)));
        stages.add(createStageResult(savedResult, "INTERFACE", fetchInterfaceAmount(unit, reconDate)));
        stages.add(createStageResult(savedResult, "JOURNAL", fetchJournalAmount(unit, reconDate)));
        stages.add(createStageResult(savedResult, "LEDGER", fetchLedgerAmount(unit, reconDate)));

        stageResultRepository.saveAll(stages);

        // 2. 차이 분석 및 결과 업데이트
        BigDecimal sourceAmt = stages.get(0).getTotalAmount();
        BigDecimal ledgerAmt = stages.get(3).getTotalAmount();
        BigDecimal diff = sourceAmt.subtract(ledgerAmt).abs();

        savedResult.setTotalAmountSource(sourceAmt);
        savedResult.setTotalAmountTarget(ledgerAmt);
        savedResult.setVarianceAmount(diff);
        savedResult.setVarianceCount(diff.compareTo(BigDecimal.ZERO) == 0 ? 0L : 1L);

        if (diff.compareTo(unit.getToleranceAmount()) <= 0) {
            savedResult.setStatus(ReconciliationStatus.SUCCESS);
        } else {
            savedResult.setStatus(ReconciliationStatus.VARIANCE_FOUND);
            createVariance(savedResult, unit, "DEEP_MISMATCH", "Source to Ledger mismatch: " + diff, diff);
        }

        return resultRepository.save(savedResult);
    }

    private ReconStageResult createStageResult(ReconciliationResult result, String stage, BigDecimal amount) {
        ReconStageResult sr = new ReconStageResult();
        sr.setReconciliationResult(result);
        sr.setStageCode(stage);
        sr.setTotalCount(0L); // 건수 생략
        sr.setTotalAmount(amount);
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
        v.setSlaDueDate(LocalDate.now().plusDays(unit.getSlaDays()));
        varianceRepository.save(v);
    }

    // 외부 데이터 조회를 위한 Mock 메서드
    private BigDecimal fetchSourceAmount(ReconUnitDefinition unit, LocalDate date) {
        return new BigDecimal("1000");
    }

    private BigDecimal fetchInterfaceAmount(ReconUnitDefinition unit, LocalDate date) {
        return new BigDecimal("1000");
    }

    private BigDecimal fetchJournalAmount(ReconUnitDefinition unit, LocalDate date) {
        return new BigDecimal("1000");
    }

    private BigDecimal fetchLedgerAmount(ReconUnitDefinition unit, LocalDate date) {
        return new BigDecimal("950");
    }
}
