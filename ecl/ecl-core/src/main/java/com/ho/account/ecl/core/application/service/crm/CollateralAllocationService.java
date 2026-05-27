package com.ho.account.ecl.core.application.service.crm;

import com.ho.account.ecl.core.application.port.out.CrAccountCollateralRepository;
import com.ho.account.ecl.core.application.port.out.CrBulkOperationPort;
import com.ho.account.ecl.core.application.port.out.CrAccountRepository;
import com.ho.account.ecl.core.application.port.out.CrCollateralRepository;
import com.ho.account.ecl.core.application.port.out.CrCustomerRepository;
import com.ho.account.ecl.core.domain.collateral.CrAccountCollateral;
import com.ho.account.ecl.core.domain.collateral.CrCollateral;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * [Service] [CRM] 수학적 최적화 기반 담보배분 서비스 (Mathematical LP CRM Optimization)
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 은행에는 여러 개의 대출을 가진 고객이 하나의 아파트나 보증서를 여러 대출에
 * 동시에 담보로 잡는 경우가 많습니다.
 * 이때 "어떤 대출에 담보를 얼마나 나눠줘야 전체 은행의 위험도(RWA)가 가장 낮아질까?"를
 * 사람이 일일이 계산하기는 매우 어렵습니다.
 * 본 서비스는 '선형계획법(LP)'이라는 수학적 기법을 사용하여,
 * 제약 조건(담보 한도, 대출 잔액) 내에서 RWA를 최소화하는 최적의 배분 비율을 자동으로 찾아냅니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollateralAllocationService {

    private final CrCustomerRepository customerRepository;
    private final CrAccountRepository accountRepository;
    private final CrCollateralRepository collateralRepository;
    private final CrAccountCollateralRepository accountCollateralRepository;
    private final CrBulkOperationPort bulkOperationPort;
    private final ApartmentCollateralService apartmentCollateralService;

    /**
     * [고도화] 모든 활성 고객에 대해 담보 배분을 수행한다.
     * 배치 작업에서 개별 고객별로 호출하던 로직을 서비스 내부로 캡슐화했습니다.
     */
    @Transactional
    public void allocateAllCollaterals() {
        log.info("🚀 [CRM 전체 최적화] 전사 고객 대상 담보 배분 프로세스를 시작합니다.");
        accountRepository.findByIsActiveTrue().stream()
                .map(CrAccount::getCustomer)
                .filter(java.util.Objects::nonNull)
                .map(customer -> customer.getId())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .forEach(customerId -> {
            try {
                allocateCollateralsForCustomer(customerId);
            } catch (Exception e) {
                log.error("❌ [CRM 최적화 실패] 고객(ID: {}) 배분 중 오류 발생: {}", customerId, e.getMessage());
            }
        });
        log.info("✅ [CRM 전체 최적화] 전사 담보 배분 프로세스가 완료되었습니다.");
    }

    /**
     * [고도화] 선형계획법(LP Solver)을 사용하여 고객의 담보를 RWA 최소화 관점에서 최적 배분한다.
     */
    @Transactional
    public void allocateCollateralsForCustomer(Long customerId) {
        log.info("⚖️ [CRM 최적화] 고객(ID: {})의 수학적 담보 배분을 시작합니다.", customerId);

        List<CrAccount> accounts = accountRepository.findByCustomer_IdAndIsActiveTrue(customerId);
        List<CrCollateral> collaterals = collateralRepository.findByCustomer_IdAndIsActiveTrue(customerId);

        if (accounts.isEmpty() || collaterals.isEmpty()) {
            log.info("  - 배분할 계좌 또는 담보가 없어 작업을 종료합니다.");
            return;
        }

        // 1. 기존 배분 정보 초기화
        for (CrAccount account : accounts) {
            bulkOperationPort.deleteAllocationByAccountId(account.getId());
        }

        try {
            // 2. LP 최적화 모델 구축 및 실행
            executeLinearProgrammingOptimization(accounts, collaterals);
            log.info("✅ [CRM 최적화] 고객(ID: {})의 최적 배분 결과를 저장했습니다.", customerId);
        } catch (Exception e) {
            log.error("❌ [CRM 최적화] 최적화 엔진 실행 중 오류 발생. 단순 Waterfall 방식으로 폴백합니다.", e);
            runSimpleWaterfallAllocation(accounts, collaterals);
        }
    }

    /**
     * [Mathematical Optimization] 심플렉스 솔버(Simplex Solver)를 사용하여 리스크 가중자산(RWA) 최소화
     * 문제를 해결한다.
     * 
     * 💡 [비즈니스 심화 설명]
     * 이 로직의 핵심은 '가장 비싼(위험가중치가 높은) 대출'에 '담보'라는 소중한 자원을 우선 배정하는 것입니다.
     * 수학적으로는 "Σ (배분액_ij * 위험가중치_i)"를 최대화하여, 결과적으로 담보 반영 후의 전체 RWA를 최소화합니다.
     */
    private void executeLinearProgrammingOptimization(List<CrAccount> accounts, List<CrCollateral> collaterals) {
        int totalAccountCount = accounts.size();
        int totalCollateralCount = collaterals.size();

        // 결정 변수(Decision Variables)의 총 개수 = (계좌 수 * 담보 수)
        // 예: 계좌 2개, 담보 3개면 총 6개의 배분 조합(Variable)이 나옵니다.
        int totalDecisionVariables = totalAccountCount * totalCollateralCount;

        // 1. 목적 함수(Objective Function) 정의
        // 각 변수의 가중치(Coefficients)는 해당 대출 계좌의 '위험가중치(Risk Weight)'입니다.
        double[] rwaReductionWeights = new double[totalDecisionVariables];
        for (int accountIdx = 0; accountIdx < totalAccountCount; accountIdx++) {
            double riskWeight = estimateRiskWeight(accounts.get(accountIdx));

            for (int collateralIdx = 0; collateralIdx < totalCollateralCount; collateralIdx++) {
                // 선형 배열 내에서 (계좌, 담보) 조합의 고유 인덱스를 계산합니다.
                int variableIdx = (accountIdx * totalCollateralCount) + collateralIdx;
                rwaReductionWeights[variableIdx] = riskWeight;
            }
        }

        // 상수항 0인 선형 목적 함수 생성 (위험 절감액 최대화가 목표)
        LinearObjectiveFunction rwaMinimizationGoal = new LinearObjectiveFunction(rwaReductionWeights, 0);

        // 2. 제약 조건(Constraints) 설정
        List<LinearConstraint> constraintList = new ArrayList<>();

        // [제약 조건 A] 계좌별 한도 (Account Capacity Constraints)
        // "특정 대출 계좌 i에 배분된 모든 담보액의 합은 해당 대출의 잔액(EAD)을 초과할 수 없다."
        for (int accountIdx = 0; accountIdx < totalAccountCount; accountIdx++) {
            double[] coefficients = new double[totalDecisionVariables];
            for (int collateralIdx = 0; collateralIdx < totalCollateralCount; collateralIdx++) {
                int variableIdx = (accountIdx * totalCollateralCount) + collateralIdx;
                coefficients[variableIdx] = 1.0;
            }
            double loanBalanceLimit = accounts.get(accountIdx).getOutstandingAmount().doubleValue();
            constraintList.add(new LinearConstraint(coefficients, Relationship.LEQ, loanBalanceLimit));
        }

        // [제약 조건 B] 담보별 한도 (Collateral Capacity Constraints)
        // "특정 담보 j가 여러 대출에 나눠준 모든 배분액의 합은 담보의 유효 가액을 초과할 수 없다."
        for (int collateralIdx = 0; collateralIdx < totalCollateralCount; collateralIdx++) {
            double[] coefficients = new double[totalDecisionVariables];
            for (int accountIdx = 0; accountIdx < totalAccountCount; accountIdx++) {
                int variableIdx = (accountIdx * totalCollateralCount) + collateralIdx;
                coefficients[variableIdx] = 1.0;
            }
            double collateralEffectiveValue = calculateEffectiveValue(collaterals.get(collateralIdx)).doubleValue();
            constraintList.add(new LinearConstraint(coefficients, Relationship.LEQ, collateralEffectiveValue));
        }

        // 3. 최적화 엔진(Simplex Solver) 기동
        // 목표: rwaMinimizationGoal을 최대화(MAXIMIZE) 하면서 모든 제약을 만족하는 '해'를 찾음
        SimplexSolver solver = new SimplexSolver();
        PointValuePair optimalSolution = solver.optimize(
                new MaxIter(1000), // 최대 반복 횟수 (안전장치)
                rwaMinimizationGoal, // 최적화 목표
                new LinearConstraintSet(constraintList), // 준수해야 할 제약 조건들
                GoalType.MAXIMIZE, // 절감 효과 최대화 선택
                new NonNegativeConstraint(true) // 모든 배분액은 0 이상이어야 함 (음수 배분 방지)
        );

        // 4. 최적화 결과(해)를 도메인 모델로 변환하여 저장
        double[] optimizedAllocations = optimalSolution.getPoint();
        for (int accountIdx = 0; accountIdx < totalAccountCount; accountIdx++) {
            for (int collateralIdx = 0; collateralIdx < totalCollateralCount; collateralIdx++) {
                int variableIdx = (accountIdx * totalCollateralCount) + collateralIdx;
                double allocatedAmount = optimizedAllocations[variableIdx];

                // 부동 소수점 오차를 고려하여 0.0001원 이상의 의미 있는 금액만 DB에 기록
                if (allocatedAmount > 0.0001) {
                    saveAllocation(
                            accounts.get(accountIdx),
                            collaterals.get(collateralIdx),
                            BigDecimal.valueOf(allocatedAmount));
                }
            }
        }
    }

    /**
     * 담보 유형별로 유효 가치를 산출한다.
     */
    private BigDecimal calculateEffectiveValue(CrCollateral collateral) {
        if ("REAL_ESTATE".equals(collateral.getCollateralType())
                || "APARTMENT".equals(collateral.getCollateralType())) {
            return apartmentCollateralService.calculateEffectiveValue(collateral);
        }
        // 일반 담보 (예금, 유가증권 등)는 헤어컷만 적용
        return collateral.getAppraisalAmount().multiply(BigDecimal.ONE.subtract(collateral.getBaseHaircut()));
    }

    private void saveAllocation(CrAccount account, CrCollateral collateral, BigDecimal amount) {
        CrAccountCollateral mapping = CrAccountCollateral.builder()
                .account(account)
                .collateral(collateral)
                .allocationAmount(amount)
                .build();
        accountCollateralRepository.save(java.util.Objects.requireNonNull(mapping));
    }

    /**
     * [Fallback Logic] 단순 Waterfall(폭포수) 배분 로직
     * 
     * 💡 [초보자를 위한 개념 설명]
     * 수학적 최적화 엔진(Simplex)이 예상치 못한 이유로 실패할 경우를 대비한 '비상용' 로직입니다.
     * 1. 우선순위: 가장 위험한 대출(위험가중치가 높은 대출)을 1순위로 보호합니다.
     * 2. 배분방식: 마치 폭포수가 위에서 아래로 흐르듯, 1순위 대출에 담보를 꽉 채우고
     * 남은 담보가 있다면 다음 순위 대출로 넘겨주는 단순하지만 확실한 방식입니다.
     */
    private void runSimpleWaterfallAllocation(List<CrAccount> accounts, List<CrCollateral> collaterals) {
        // 1. 대출 계좌를 위험도(Risk Weight)가 높은 순서대로 정렬합니다. (위험한 놈부터 먼저 방어!)
        accounts.sort((a, b) -> Integer.compare(estimateRiskWeight(b), estimateRiskWeight(a)));

        for (CrCollateral collateral : collaterals) {
            // 해당 담보가 가진 실제 가용 금액을 계산합니다.
            BigDecimal remainingAmt = calculateEffectiveValue(collateral);

            for (CrAccount account : accounts) {
                // 담보를 다 썼다면 다음 담보로 넘어갑니다.
                if (remainingAmt.compareTo(BigDecimal.ZERO) <= 0)
                    break;

                // 해당 계좌가 이미 다른 담보로부터 배분받은 총액을 조회합니다.
                BigDecimal alreadyAllocated = bulkOperationPort.sumAllocationByAccountId(account.getId());
                if (alreadyAllocated == null)
                    alreadyAllocated = BigDecimal.ZERO;

                // 아직 담보로 보호받지 못한 순수 대출 잔액(Uncollateralized)을 계산합니다.
                BigDecimal uncollateralized = account.getOutstandingAmount().subtract(alreadyAllocated);

                if (uncollateralized.compareTo(BigDecimal.ZERO) > 0) {
                    // 남은 담보액과 대출 잔액 중 '작은 금액'만큼 배분합니다.
                    BigDecimal alloc = remainingAmt.min(uncollateralized);

                    // 배분 결과 저장
                    saveAllocation(account, collateral, alloc);

                    // 방금 써버린 만큼 담보 가용액을 차감합니다. (폭포수가 다음 칸으로 흐름)
                    remainingAmt = remainingAmt.subtract(alloc);
                }
            }
        }
    }

    /**
     * 계좌의 예상 위험가중치(Risk Weight)를 산출한다.
     */
    private int estimateRiskWeight(CrAccount account) {
        String prod = (account.getProductCode() != null) ? account.getProductCode().toUpperCase() : "";
        if (prod.contains("CORP"))
            return 100;
        if (prod.contains("RETAIL"))
            return 75;
        if (prod.contains("MORTGAGE"))
            return 35;
        if (prod.contains("SOV"))
            return 0;
        return 100;
    }
}
