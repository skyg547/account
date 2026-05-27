package com.risk.mart.core.domain.ods.audit.service;

import com.risk.mart.core.application.port.out.OdsBalanceHistRepository;
import com.risk.mart.core.application.port.out.OdsGeneralLedgerRepository;
import com.risk.mart.core.application.port.out.OdsReconcileHistRepository;
import com.risk.mart.core.domain.ods.audit.OdsReconcileHist;
import com.risk.mart.core.application.port.out.IntegratedRiskPositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * [ODS 서비스] 원장 대사(Reconciliation) 및 정합성 검증 서비스
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OdsReconciliationService {

    private final OdsGeneralLedgerRepository glRepository;
    private final OdsBalanceHistRepository balanceRepository;
    private final OdsReconcileHistRepository reconcileRepository;
    private final IntegratedRiskPositionRepository martRepository;

    /**
     * 특정 기준일의 총계정원장(GL)과 보조원장(SL, 잔액이력)을 비교 대사한다.
     */
    public void reconcileGlToSl(LocalDate baseDate) {
        log.info("🔍 [대사 시작] GL vs SL 정합성 검증 (기준일: {})", baseDate);

        List<OdsBalanceHistRepository.BalanceSummary> slSummaries = Objects.requireNonNull(balanceRepository
                .findBalanceSummaryByBaseDate(baseDate));

        Map<String, OdsGeneralLedgerRepository.SubjectCurrencyBalanceSummary> glMap = Objects.requireNonNull(
                glRepository.getBalanceSummaryByBaseDate(baseDate))
                .stream()
                .collect(Collectors.toMap(
                        gl -> gl.getSubjectCode() + "_" + gl.getCurrencyCode(),
                        gl -> gl
                ));

        for (OdsBalanceHistRepository.BalanceSummary sl : slSummaries) {
            String key = sl.getSubjectCode() + "_" + sl.getCurrencyCode();
            BigDecimal glAmt = glMap.containsKey(key) ? glMap.get(key).getBalanceAmount() : BigDecimal.ZERO;
            BigDecimal slAmt = sl.getBalanceAmount();
            BigDecimal diff = glAmt.subtract(slAmt);

            boolean mismatched = diff.abs().compareTo(new BigDecimal("0.01")) > 0;
            if (mismatched) {
                log.warn("🚨 [대사 불일치] 키: {}, GL: {}, SL: {}, 차이: {}", key, glAmt, slAmt, diff);
            }

            OdsReconcileHist hist = OdsReconcileHist.builder()
                    .baseDate(baseDate)
                    .sourceSystem("GL")
                    .targetSystem("SL")
                    .reconcileType("GL_SL_AUDIT")
                    .reconcileItem(key)
                    .sourceAmount(glAmt)
                    .targetAmount(slAmt)
                    .diffAmount(diff)
                    .status(mismatched ? "MISMATCH" : "정상(MATCH)")
                    .auditTimestamp(LocalDateTime.now())
                    .build();
            reconcileRepository.save(hist);
        }
    }

    public void reconcileGlVsSl(LocalDate baseDate) {
        reconcileGlToSl(baseDate);
    }

    public void reconcileMartVsGl(LocalDate baseDate) {
        log.info("🔍 [대사 시작] GL vs MART 정합성 검증 (기준일: {})", baseDate);

        Map<String, OdsGeneralLedgerRepository.SubjectCurrencyBalanceSummary> glMap = Objects.requireNonNull(
                glRepository.getBalanceSummaryByBaseDate(baseDate))
                .stream()
                .collect(Collectors.toMap(
                        gl -> gl.getSubjectCode() + "_" + gl.getCurrencyCode(),
                        gl -> gl,
                        (left, right) -> left
                ));

        for (IntegratedRiskPositionRepository.ProductCurrencyBalanceSummary mart
                : Objects.requireNonNull(martRepository.getBalanceSummaryByBaseDate(baseDate))) {
            String key = mart.getProductCode() + "_" + mart.getCurrencyCode();
            BigDecimal glAmt = glMap.containsKey(key) ? glMap.get(key).getBalanceAmount() : BigDecimal.ZERO;
            BigDecimal martAmt = mart.getBalanceAmount() != null ? mart.getBalanceAmount() : BigDecimal.ZERO;
            BigDecimal diff = glAmt.subtract(martAmt);

            if (diff.abs().compareTo(new BigDecimal("0.01")) > 0) {
                log.warn("🚨 [대사 불일치] 키: {}, GL: {}, MART: {}, 차이: {}", key, glAmt, martAmt, diff);

                OdsReconcileHist hist = OdsReconcileHist.builder()
                        .baseDate(baseDate)
                        .sourceSystem("GL")
                        .targetSystem("MART")
                        .reconcileType("GL_MART_AUDIT")
                        .reconcileItem(key)
                        .sourceAmount(glAmt)
                        .targetAmount(martAmt)
                        .diffAmount(diff)
                        .status("MISMATCH")
                        .auditTimestamp(LocalDateTime.now())
                        .build();
                reconcileRepository.save(hist);
            }
        }
    }
}
