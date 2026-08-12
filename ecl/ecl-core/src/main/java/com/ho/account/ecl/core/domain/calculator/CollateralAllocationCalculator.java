package com.ho.account.ecl.core.domain.calculator;

import com.ho.account.ecl.core.domain.collateral.CrCollateral;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * [도메인 계산기] 수학적 최적화 및 폭포수(Waterfall) 기법 기반 담보 배분 순수 도메인 계산기 (Pure Domain Calculator)
 *
 * 💡 [초보자를 위한 개념 설명 & DDD 설계 원칙]
 * 1. Anemic Domain Model vs Rich Domain Model:
 *    기존에는 Application Service(CollateralAllocationService) 내부에 선형계획법(Simplex LP) 연산과
 *    우선순위 산정, 폭포수 순차 배분 계산 알고리즘이 직접 노출되어 서비스가 비대해졌습니다.
 *    이러한 'Anemic Domain Model'(빈약한 도메인 모델)을 개선하기 위해 모든 금융/수학적 계산식을
 *    도메인 계층의 Pure Domain Calculator로 이관했습니다.
 *
 * 2. Pure Domain Service / Calculator의 역할:
 *    본 클래스는 외부 저장소(Repository)나 인프라 의존성 없이 순수 도메인 객체(CrAccount, CrCollateral)만을
 *    입력받아 담보 최적 배분 결과를 연산합니다.
 *    Application Service는 단지 데이터 조회 및 이 계산기를 호출하는 조율자(Orchestration) 역할만 수행합니다.
 *
 * 3. 담보 최적 배분 알고리즘:
 *    - 선형계획법 (Linear Programming - Simplex Solver): 제약조건(계좌 잔액 EAD, 담보 유효가치) 하에서
 *      손실 절감 가중치 합계를 최대화(MAXIMIZE)하는 최적 배분 비율 산출
 *    - 폭포수 (Waterfall Fallback): 최적화 엔진 예외 시 대출 우선순위에 따라 1순위부터 차례대로
 *      담보 잔액을 소진하는 보수적 안전 배분 알고리즘
 */
@Slf4j
@Component
public class CollateralAllocationCalculator {

    /**
     * [도메인 DTO] 담보 배분 계산 결과 객체
     */
    @Getter
    @Builder
    public static class AllocationResult {
        private final CrAccount account;
        private final CrCollateral collateral;
        private final BigDecimal allocatedAmount;
    }

    /**
     * [Pure Domain Calculator] 선형계획법(Simplex Solver)을 활용하여 손실 절감 효과가 최대화되도록 담보를 최적 배분합니다.
     *
     * 💡 [수학적 산식, 부동소수점 정밀도 및 제약조건]
     * - 목적 함수: Maximize Σ (배분액_ij * 우선순위가중치_i)
     * - 제약 조건 1: Σ_j (배분액_ij) <= 계좌 i의 대출 잔액(EAD)
     * - 제약 조건 2: Σ_i (배분액_ij) <= 담보 j의 유효 가액(Effective Value)
     * - 제약 조건 3: 배분액_ij >= 0 (음수 배분 불가)
     *
     * 📌 [금융 정밀도 제어 (Pedagogical Comments)]
     * - 외부 수학적 최적화 라이브러리(Apache Commons Math SimplexSolver)는 선형계획법 매트릭스 특성상 primitive double 배열을 인수로 요구합니다.
     * - 이에 따라 LP 연산 시점에 정밀 전달을 수행하고, 연산 결과 배분액을 도메인 모델 DTO로 복원할 때는
     *   `BigDecimal` 및 소수점 4자리 반올림(`setScale(4, RoundingMode.HALF_UP)`) 및 임계값(`0.0001`) 비교를 통한 정밀 검증을 수행하여
     *   부동소수점 근사 오차(IEEE 754 precision loss)가 담보 배분 잔액 및 차감 LGD 계산에 오차를 유발하지 않도록 안전 조치합니다.
     *
     * @param accounts 배분 대상 대출 계좌 목록
     * @param collaterals 배분 대상 담보 목록
     * @return 계산 완료된 담보 배분 결과 목록 (0.0001원 초과 항목만 포함)
     */
    public List<AllocationResult> calculateLpOptimization(List<CrAccount> accounts, List<CrCollateral> collaterals) {
        int totalAccountCount = accounts.size();
        int totalCollateralCount = collaterals.size();
        int totalDecisionVariables = totalAccountCount * totalCollateralCount;

        // 1. 목적 함수(Objective Function) 구성: 각 (계좌, 담보) 조합의 가중치 세팅
        double[] lossReductionWeights = new double[totalDecisionVariables];
        for (int accountIdx = 0; accountIdx < totalAccountCount; accountIdx++) {
            double priorityWeight = (double) estimateLossPriorityWeight(accounts.get(accountIdx));
            for (int collateralIdx = 0; collateralIdx < totalCollateralCount; collateralIdx++) {
                int variableIdx = (accountIdx * totalCollateralCount) + collateralIdx;
                lossReductionWeights[variableIdx] = priorityWeight;
            }
        }
        LinearObjectiveFunction lossReductionGoal = new LinearObjectiveFunction(lossReductionWeights, 0);

        // 2. 제약 조건(Constraints) 구성
        List<LinearConstraint> constraintList = new ArrayList<>();

        // [제약 A] 계좌별 대출 잔액 상한 (BigDecimal -> double 정밀 전환)
        for (int accountIdx = 0; accountIdx < totalAccountCount; accountIdx++) {
            double[] coefficients = new double[totalDecisionVariables];
            for (int collateralIdx = 0; collateralIdx < totalCollateralCount; collateralIdx++) {
                int variableIdx = (accountIdx * totalCollateralCount) + collateralIdx;
                coefficients[variableIdx] = 1.0;
            }
            BigDecimal outstandingBd = accounts.get(accountIdx).getOutstandingAmount();
            double loanBalanceLimit = (outstandingBd != null) ? outstandingBd.doubleValue() : 0.0;
            constraintList.add(new LinearConstraint(coefficients, Relationship.LEQ, loanBalanceLimit));
        }

        // [제약 B] 담보별 유효 가액 상한 (CrCollateral 엔티티 도메인 메서드 활용)
        for (int collateralIdx = 0; collateralIdx < totalCollateralCount; collateralIdx++) {
            double[] coefficients = new double[totalDecisionVariables];
            for (int accountIdx = 0; accountIdx < totalAccountCount; accountIdx++) {
                int variableIdx = (accountIdx * totalCollateralCount) + collateralIdx;
                coefficients[variableIdx] = 1.0;
            }
            BigDecimal effectiveValBd = collaterals.get(collateralIdx).calculateEffectiveValue();
            double collateralEffectiveValue = (effectiveValBd != null) ? effectiveValBd.doubleValue() : 0.0;
            constraintList.add(new LinearConstraint(coefficients, Relationship.LEQ, collateralEffectiveValue));
        }

        // 3. Simplex Solver 최적화 수행
        SimplexSolver solver = new SimplexSolver();
        PointValuePair optimalSolution = solver.optimize(
                new MaxIter(1000),
                lossReductionGoal,
                new LinearConstraintSet(constraintList),
                GoalType.MAXIMIZE,
                new NonNegativeConstraint(true)
        );

        // 4. 연산 결과 DTO 매핑 (소수점 4자리 반올림 및 BigDecimal threshold 정밀 판단)
        double[] optimizedAllocations = optimalSolution.getPoint();
        List<AllocationResult> results = new ArrayList<>();
        BigDecimal allocationThreshold = new BigDecimal("0.0001");

        for (int accountIdx = 0; accountIdx < totalAccountCount; accountIdx++) {
            for (int collateralIdx = 0; collateralIdx < totalCollateralCount; collateralIdx++) {
                int variableIdx = (accountIdx * totalCollateralCount) + collateralIdx;
                double rawAllocatedAmount = optimizedAllocations[variableIdx];

                BigDecimal allocatedAmountBd = BigDecimal.valueOf(rawAllocatedAmount)
                        .setScale(4, java.math.RoundingMode.HALF_UP);

                if (allocatedAmountBd.compareTo(allocationThreshold) > 0) {
                    results.add(AllocationResult.builder()
                            .account(accounts.get(accountIdx))
                            .collateral(collaterals.get(collateralIdx))
                            .allocatedAmount(allocatedAmountBd)
                            .build());
                }
            }
        }
        return results;
    }

    /**
     * [Pure Domain Calculator] 폭포수(Waterfall) 순차 배분 알고리즘을 수행합니다.
     *
     * 💡 [개념 설명]
     * 손실 절감 가중치가 높은 대출부터 1순위로 담보를 우선 배정하고,
     * 잔여 담보 금액을 2순위 대출로 넘기는 순차적(Cascade) 배분 방식입니다.
     *
     * @param accounts 배분 대상 계좌 목록
     * @param collaterals 배분 대상 담보 목록
     * @param existingAllocationsMap 계좌별 이미 배분된 금액 (null 안전)
     * @return 폭포수 배분 결과 목록
     */
    public List<AllocationResult> calculateWaterfallAllocation(
            List<CrAccount> accounts,
            List<CrCollateral> collaterals,
            Map<Long, BigDecimal> existingAllocationsMap) {

        List<CrAccount> sortedAccounts = new ArrayList<>(accounts);
        sortedAccounts.sort((a, b) -> Integer.compare(estimateLossPriorityWeight(b), estimateLossPriorityWeight(a)));

        List<AllocationResult> results = new ArrayList<>();

        for (CrCollateral collateral : collaterals) {
            BigDecimal remainingAmt = collateral.calculateEffectiveValue();

            for (CrAccount account : sortedAccounts) {
                if (remainingAmt.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }

                BigDecimal alreadyAllocated = (existingAllocationsMap != null)
                        ? existingAllocationsMap.getOrDefault(account.getId(), BigDecimal.ZERO)
                        : BigDecimal.ZERO;
                if (alreadyAllocated == null) {
                    alreadyAllocated = BigDecimal.ZERO;
                }

                BigDecimal uncollateralized = account.getOutstandingAmount().subtract(alreadyAllocated);

                if (uncollateralized.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal alloc = remainingAmt.min(uncollateralized);
                    results.add(AllocationResult.builder()
                            .account(account)
                            .collateral(collateral)
                            .allocatedAmount(alloc)
                            .build());

                    remainingAmt = remainingAmt.subtract(alloc);
                }
            }
        }
        return results;
    }

    /**
     * [도메인 로직] 계좌의 상품 코드별 손실 절감 배분 우선순위 가중치를 산출합니다.
     *
     * 💡 [비즈니스 가이드]
     * - CORP (기업 대출): 손실 시 파급 효과가 크므로 우선순위 100
     * - RETAIL (가계 신용 대출): 우선순위 75
     * - MORTGAGE (주택 담보 대출): 기본 담보권이 강력하므로 우선순위 35
     * - SOV (국선/공공 대출): 부도 위험이 극히 낮아 우선순위 0
     *
     * @param account 대출 계좌
     * @return 배분 우선순위 가중치 수치 (높을수록 우선 배정)
     */
    public int estimateLossPriorityWeight(CrAccount account) {
        if (account == null || account.getProductCode() == null) {
            return 100;
        }
        String prod = account.getProductCode().toUpperCase();
        if (prod.contains("CORP")) return 100;
        if (prod.contains("RETAIL")) return 75;
        if (prod.contains("MORTGAGE")) return 35;
        if (prod.contains("SOV")) return 0;
        return 100;
    }
}
