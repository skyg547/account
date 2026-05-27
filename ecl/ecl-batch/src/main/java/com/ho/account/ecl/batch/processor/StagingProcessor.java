package com.ho.account.ecl.batch.processor;

import com.ho.account.shared.finance.enums.CalculationStatus;
import com.ho.account.shared.finance.enums.CrStaging;
import com.ho.account.ecl.batch.support.BatchParameterUtils;
import com.ho.account.ecl.core.application.service.calculation.IrbParameterService;
import com.ho.account.ecl.core.application.service.calculation.LgdCalculationService;
import com.ho.account.ecl.core.application.service.calculation.PdCalculationService;
import com.ho.account.ecl.core.application.service.calculation.StagingService;
import com.ho.account.ecl.core.domain.calculator.IrbRegulatoryParams;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import com.ho.account.ecl.core.domain.result.CrRiskResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [Step 1] 스테이징 및 기초 PD/LGD 매핑 프로세서
 * 
 * 💡 [초보자 가이드]
 * 배치 공정의 첫 번째 단계로, 차주의 '건강 상태(등급)'를 확인하고 
 * 부도 확률(PD)이라는 기초 데이터를 준비하는 역할을 합니다.
 * 
 * 비즈니스 용어 설명:
 * - Staging: IFRS 9 기준에 따른 자산 건전성 분류(Stage 1: 정상, Stage 2: 유의적 증가, Stage 3: 손상).
 * - PD (Probability of Default): 차주가 향후 1년 내 부도날 확률.
 * - IRB (Internal Ratings-Based): 은행 내부 등급을 활용하여 리스크를 산출하는 방식.
 */
@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class StagingProcessor implements ItemProcessor<CrAccount, CrRiskResult> {

    /** 💡 [초보자 가이드] 연체 정보 등을 분석하여 정상/주의/손상 단계(Stage)를 판정하는 서비스입니다. */
    private final StagingService stagingService;
    
    /** 💡 [초보자 가이드] 등급 마스터 정보를 조회하여 부도 확률(PD)을 가져오는 서비스입니다. */
    private final PdCalculationService pdCalculationService;
    
    /** 💡 [초보자 가이드] 부도 시 손실률(LGD)의 기초 값을 매핑해주는 서비스입니다. */
    private final LgdCalculationService lgdCalculationService;
    
    /** 💡 [초보자 가이드] 국제 금융 규제에서 정한 최소 부도 확률(PD Floor) 등을 관리하는 서비스입니다. */
    private final IrbParameterService irbParameterService;

    /** 💡 [초보자 가이드] 배치 실행 시 입력받는 '기준 일자'입니다. */
    @Value("#{jobParameters['baseDate'] ?: jobParameters['baseDt']}")
    private String baseDateStr;

    /**
     * 개별 계좌(CrAccount)를 대손충당금(IFRS9) 산출 시작 객체(CrRiskResult)로 변환합니다.
     * 
     * @param account 원천 계좌 데이터
     * @return 초기화된 대손충당금(IFRS9) 산출 결과 객체
     */
    @Override
    public CrRiskResult process(CrAccount account) {
        LocalDate baseDate = BatchParameterUtils.resolveBaseDate(baseDateStr, null);
        IrbRegulatoryParams irbParams = irbParameterService.getParameters();

        log.debug("⚡ [Phase 1] 스테이징 및 PD 산출 시작 - 계좌: {}", account.getAccountNo());

        // 🟢 Phase 1-1. 스테이징(Staging) 판정
        // [비즈니스 로직] IFRS 9 기준에 따라 연체발생, 조기경보, 채무조정 여부를 종합하여 Stage 1~3를 판정합니다.
        CrStaging stage = stagingService.determineStage(
                account,
                account.getCustomer().getWarningLevel(),
                Boolean.TRUE.equals(account.getIsDebtRestructured())
        );

        // 🟢 Phase 1-2. 기초 PD(Probability of Default) 산출
        // [비즈니스 로직] 차주 또는 계좌 등급을 기반으로 부도확률을 매핑하고 규제 할증(Penalty)을 적용합니다.
        BigDecimal initialPd = pdCalculationService.calculatePd(account, irbParams);

        // 🟢 Phase 1-3. 초기 대손충당금(IFRS9) 산출 결과 객체(CrRiskResult) 생성
        // [기술적 설계] 이후 단계(EAD, LGD, RWA)에서 이 객체를 업데이트하며 최종 산출을 완성합니다.
        return CrRiskResult.builder()
                .id(account.getId()) // [v3.0] 계좌 ID를 결과 ID로 사용 (파티셔닝 및 단계별 연동 안정성 확보)
                .baseDate(baseDate)
                .account(account)
                .staging(stage)
                .pd(initialPd)
                .status(CalculationStatus.RUNNING)
                .calculationCompletedAt(LocalDateTime.now())
                .build();
    }
}
