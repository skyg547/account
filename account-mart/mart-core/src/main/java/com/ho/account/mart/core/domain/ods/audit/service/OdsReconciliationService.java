package com.ho.account.mart.core.domain.ods.audit.service;

import com.ho.account.mart.core.application.port.out.OdsBalanceHistRepository;
import com.ho.account.mart.core.application.port.out.OdsGeneralLedgerRepository;
import com.ho.account.mart.core.application.port.out.OdsReconcileHistRepository;
import com.ho.account.mart.core.domain.ods.audit.OdsReconcileHist;
import com.ho.account.mart.core.application.port.out.AllowanceInputPositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * [ODS 서비스] 원장 대사(Reconciliation) 및 정합성 검증 서비스
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OdsReconciliationService {

    private static final BigDecimal GL_SL_TOLERANCE = new BigDecimal("0.01");

    private final OdsGeneralLedgerRepository glRepository;
    private final OdsBalanceHistRepository balanceRepository;
    private final OdsReconcileHistRepository reconcileRepository;
    private final AllowanceInputPositionRepository martRepository;

    /**
     * 기준일의 GL/SL 계정·통화 합집합을 대사하고 불일치 건수를 반환한다.
     * 이력은 호출한 배치 Step의 실패 롤백과 분리해 커밋한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int reconcileGlToSl(LocalDate baseDate) {
        log.info("🔍 [대사 시작] GL vs SL 정합성 검증 (기준일: {})", baseDate);

        Map<ReconciliationKey, BigDecimal> slBalances = Objects.requireNonNull(balanceRepository
                .findBalanceSummaryByBaseDate(baseDate)).stream()
                .collect(Collectors.toMap(
                        sl -> new ReconciliationKey(sl.getSubjectCode(), sl.getCurrencyCode()),
                        OdsBalanceHistRepository.BalanceSummary::getBalanceAmount,
                        BigDecimal::add));

        Map<ReconciliationKey, BigDecimal> glBalances = Objects.requireNonNull(
                glRepository.getBalanceSummaryByBaseDate(baseDate))
                .stream()
                .collect(Collectors.toMap(
                        gl -> new ReconciliationKey(gl.getSubjectCode(), gl.getCurrencyCode()),
                        OdsGeneralLedgerRepository.SubjectCurrencyBalanceSummary::getBalanceAmount,
                        BigDecimal::add
                ));

        Set<ReconciliationKey> keys = new HashSet<>(slBalances.keySet());
        keys.addAll(glBalances.keySet());
        int mismatchCount = 0;

        for (ReconciliationKey key : keys) {
            BigDecimal glAmt = glBalances.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal slAmt = slBalances.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal diff = glAmt.subtract(slAmt);

            // 금액이 0이어도 한쪽에만 존재하는 계정·통화는 원천 누락이므로 실패한다.
            boolean mismatched = !glBalances.containsKey(key) || !slBalances.containsKey(key)
                    || diff.abs().compareTo(GL_SL_TOLERANCE) > 0;
            if (mismatched) {
                mismatchCount++;
                log.warn("🚨 [대사 불일치] 키: {}, GL: {}, SL: {}, 차이: {}", key.item(), glAmt, slAmt, diff);
            }

            OdsReconcileHist hist = OdsReconcileHist.builder()
                    .baseDate(baseDate)
                    .sourceSystem("GL")
                    .targetSystem("SL")
                    .reconcileType("GL_SL_AUDIT")
                    .reconcileItem(key.item())
                    .sourceAmount(glAmt)
                    .targetAmount(slAmt)
                    .diffAmount(diff)
                    .status(mismatched ? "MISMATCH" : "정상(MATCH)")
                    .auditTimestamp(LocalDateTime.now())
                    .build();
            reconcileRepository.save(hist);
        }
        return mismatchCount;
    }

    // 기존 호출자의 별칭도 프록시 진입점이므로 독립 트랜잭션을 명시한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int reconcileGlVsSl(LocalDate baseDate) {
        return reconcileGlToSl(baseDate);
    }

    private record ReconciliationKey(String subjectCode, String currencyCode) {
        String item() {
            return subjectCode + "_" + currencyCode;
        }
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

        for (AllowanceInputPositionRepository.ProductCurrencyBalanceSummary mart
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
