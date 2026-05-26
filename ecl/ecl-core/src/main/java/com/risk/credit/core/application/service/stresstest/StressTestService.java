package com.risk.credit.core.application.service.stresstest;

import com.risk.credit.core.application.port.out.CrAccountRepository;
import com.risk.credit.core.application.port.out.CrRiskResultRepository;
import com.risk.credit.core.application.port.out.CrSimulationResultRepository;
import com.risk.credit.core.domain.calculator.CreditRiskCalculator;
import com.risk.credit.core.domain.exposure.CrAccount;
import com.risk.credit.core.domain.result.CrRiskResult;
import com.risk.credit.core.domain.result.CrSimulationResult;
import com.risk.common.enums.CalculationStatus;
import com.risk.common.enums.StressScenario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * [Service] 스트레스 테스트 시뮬레이션 서비스 (Stress Test Service)
 * 전사 포트폴리오를 대상으로 위기 시나리오별 시뮬레이션을 수행합니다.
 *
 * 💡 [초보자를 위한 개념 설명]
 * 스트레스 테스트란 "만약 경제 위기가 오면 우리 은행이 얼마만큼 버틸 수 있을까?"를 시뮬레이션하는 것입니다.
 * 예를 들어 실업률이 폭등하거나 집값이 폭락하는 시나리오를 설정하고,
 * 그에 따라 모든 고객들의 부도율(PD)과 손실률(LGD)을 높여서 리스크 지표가 어떻게 변하는지 미리 계산해 봅니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StressTestService {

    private final CrAccountRepository accountRepository;
    private final CrRiskResultRepository baselineRepository;
    private final CrSimulationResultRepository simulationRepository;
    private final CreditRiskCalculator riskCalculator;

    /**
     * 특정 시나리오에 따른 스트레스 테스트 시뮬레이션을 실행한다.
     * 
     * @param baseDate 기준 일자
     * @param scenario 스트레스 시나리오 (Baseline, Mild, Severe 등)
     */
    @Transactional
    public void runSimulation(LocalDate baseDate, StressScenario scenario) {
        log.info("🚀 [스트레스 테스트] 시뮬레이션 시작: 시나리오 {} / 기준일 {}", scenario.name(), baseDate);

        // 1. 해당 시나리오/기준일의 기존 결과 삭제
        simulationRepository.deleteByBaseDateAndScenario(baseDate, scenario);

        // 2. 모든 활성 계좌 조회
        List<CrAccount> accounts = accountRepository.findByIsActiveTrue();

        for (CrAccount account : accounts) {
            try {
                // 비교를 위한 기본(Baseline) 산출 결과 조회
                Optional<CrRiskResult> baselineOpt = baselineRepository.findByBaseDateAndAccountId(baseDate,
                        account.getId());

                if (baselineOpt.isEmpty()) {
                    log.warn("계좌 {} 의 기본(Baseline) 산출 결과가 없어 시뮬레이션을 건너뜁니다.", account.getAccountNo());
                    continue;
                }

                CrRiskResult baseline = baselineOpt.get();

                // 3. 충격(Stress) 반영 리스크 지표 산출
                // 시뮬레이션에서는 일반적으로 EAD*는 기본값을 사용하고 PD/LGD에 충격을 가함
                BigDecimal ead = baseline.getEadStar();
                BigDecimal pd = baseline.getPd();
                BigDecimal lgd = baseline.getLgd();

                // 시나리오별 PD/LGD 배수 적용
                BigDecimal stressedPd = pd.multiply(BigDecimal.valueOf(scenario.getPdMultiplier()));
                BigDecimal stressedLgd = lgd.multiply(BigDecimal.valueOf(scenario.getLgdMultiplier()));

                // 충격 반영 기대손실(Stress ECL) 산출
                BigDecimal stressEcl = riskCalculator.calculateEcl(
                        baseline.getStaging(),
                        List.of(stressedPd), // 시뮬레이션용 단순화된 PD 곡선
                        stressedLgd,
                        ead,
                        new BigDecimal("0.05"));

                // 충격 반영 RWA(Stress IRB) 산출
                CreditRiskCalculator.IrbResult stressIrb = riskCalculator.calculateRwaIrb(
                        stressedPd,
                        stressedLgd,
                        ead,
                        2.5 // 시뮬레이션용 기본 만기 적용
                );

                // 4. 결과 저장 및 변동분(Delta) 분석
                CrSimulationResult simResult = CrSimulationResult.builder()
                        .baseDate(baseDate)
                        .scenario(scenario)
                        .account(account)
                        .staging(baseline.getStaging())
                        .eadStar(ead)
                        .stressPd(stressedPd)
                        .stressLgd(stressedLgd)
                        .stressEcl(stressEcl)
                        .stressRwaIrb(stressIrb.getRwa())
                        .eclDelta(stressEcl.subtract(baseline.getExpectedLoss()))
                        .rwaDelta(stressIrb.getRwa().subtract(baseline.getRwaIrb()))
                        .status(CalculationStatus.COMPLETED)
                        .simulationCompletedAt(LocalDateTime.now())
                        .build();

                simulationRepository.save(java.util.Objects.requireNonNull(simResult));

            } catch (Exception e) {
                log.error("계좌 {} 의 시뮬레이션 처리 중 오류 발생: {}", account.getAccountNo(), e.getMessage());
            }
        }

        log.info("✅ [스트레스 테스트] 시뮬레이션 완료: 총 {} 건의 결과가 생성되었습니다.", accounts.size());
    }

    /**
     * 특정 시나리오의 시뮬레이션 결과를 조회한다.
     */
    @Transactional(readOnly = true)
    public List<CrSimulationResult> getSimulationResults(LocalDate baseDate, StressScenario scenario) {
        return simulationRepository.findByBaseDateAndScenario(baseDate, scenario);
    }

    /**
     * 특정 시나리오의 시뮬레이션 결과 요약을 조회한다.
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getSimulationSummary(LocalDate baseDate, StressScenario scenario) {
        List<CrSimulationResult> results = getSimulationResults(baseDate, scenario);

        BigDecimal totalEclDelta = results.stream()
                .map(CrSimulationResult::getEclDelta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRwaDelta = results.stream()
                .map(CrSimulationResult::getRwaDelta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long count = results.size();

        return java.util.Map.of(
                "scenario", scenario,
                "count", count,
                "totalEclDelta", totalEclDelta,
                "totalRwaDelta", totalRwaDelta,
                "averagePdIncrease", BigDecimal.valueOf(scenario.getPdMultiplier() - 1.0)
        );
    }
}
