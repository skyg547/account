package com.risk.credit.batch.processor;

import com.risk.common.enums.CalculationStatus;
import com.risk.credit.batch.support.BatchParameterUtils;
import com.risk.credit.core.application.service.calculation.IrbParameterService;
import com.risk.credit.core.domain.calculator.CreditRiskCalculator;
import com.risk.credit.core.domain.calculator.IrbRegulatoryParams;
import com.risk.credit.core.domain.model.SaRwMapper;
import com.risk.credit.core.domain.result.CrRiskResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * [Step 4] 최종 RWA(위험가중자산) 산출 및 상태 마감 프로세서
 * 
 * 💡 [초보자 가이드]
 * 모든 리스크 요소(PD, LGD, EAD)가 모였을 때, 은행이 이 대출을 위해 보유해야 하는 '안비계(자본)'의 기준인 RWA를 산출합니다.
 * 표준방법(누구나 아는 기준)과 내부등급법(은행 자체 알고리즘) 두 가지 방식으로 모두 계산하여 리포팅에 활용합니다.
 * 
 * 비즈니스 용어 설명:
 * - RWA (Risk Weighted Assets): 위험가중자산. 자산의 위험도에 따라 산출된 가치로, 높을수록 자본을 더 많이 쌓아야 합니다.
 * - SA (Standardized Approach): 금융당국이 정한 고정된 위험가중치를 사용하는 표준 방식.
 * - IRB (Internal Ratings-Based): 은행이 자체 개발한 모형(PD, LGD 등)을 사용하여 산출하는 고도화 방식.
 */
@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class RwaProcessor implements ItemProcessor<CrRiskResult, CrRiskResult> {

    /** 💡 [초보자 가이드] 바젤 III 규제 산식을 실제로 구현한 복잡한 계산 엔진입니다. */
    private final CreditRiskCalculator riskCalculator;
    
    /** 💡 [초보자 가이드] 정부가 정한 법적인 표준 위험 가중치를 찾아주는 서비스입니다. */
    private final SaRwMapper saRwMapper;
    
    /** 💡 [초보자 가이드] 산출에 필요한 규제 파라미터(마진률, 만기조정 등)를 가져옵니다. */
    private final IrbParameterService irbParameterService;

    /** 💡 [초보자 가이드] 배치 실행 시 입력받는 '기준 일자'입니다. */
    @Value("#{jobParameters['baseDate'] ?: jobParameters['baseDt']}")
    private String baseDateStr;

    /**
     * 최종 산출 단계로, RWA와 비기대손실을 산출하고 배치를 마감 상태로 변경합니다.
     * 
     * @param result 이전 단계(ECL)가 완료된 산출 결과 객체
     * @return 모든 산출이 완료되어 'COMPLETED' 상태가 된 최종 객체
     */
    @Override
    public CrRiskResult process(CrRiskResult result) {
        // 💡 [로그] 마지막 산출 단계인 RWA 공정 시작을 알립니다.
        log.info("🚀 [Step 4] RWA 산출 및 마감 시작 - 계좌: {}, 기존상태: {}", result.getAccount().getAccountNo(), result.getStatus());
        
        // 💡 [날짜/설정] 기준 일자와 규제 파라미터를 로드합니다.
        LocalDate baseDate = BatchParameterUtils.resolveBaseDate(baseDateStr, null);
        IrbRegulatoryParams irbParams = irbParameterService.getParameters();

        // 1. 잔존 만기 재산출 (RWA 수식용)
        // 💡 [만기] 바젤 규정상 만기조정(Maturity Adjustment)을 위해 남은 기간을 년 단위로 구합니다.
        double maturityYears = 2.5d;
        if (result.getAccount().getMaturityDate() != null) {
            long remainingDays = ChronoUnit.DAYS.between(baseDate, result.getAccount().getMaturityDate());
            maturityYears = Math.max(remainingDays / 365.0d, 1.0d);
        }

        // 2. 표준방법(SA) RWA 산출
        // 💡 [SA] 고객의 유형(개인/기업)과 외부 등급에 따라 정해진 위험가중치(RW)를 적용합니다.
        BigDecimal appliedRw = saRwMapper.getStandardRw(
                result.getAccount().getCustomer().getCustomerType(),
                result.getAccount().getCustomer().getInternalRating()
        );
        BigDecimal rwaSa = riskCalculator.calculateRwaSa(result.getEadStar(), appliedRw);

        // 3. 내부등급법(IRB) RWA 산출
        // 💡 [IRB] 은행 내부 모형으로 구한 PD, LGD, EAD를 바젤 공식에 넣어 산출합니다. 가장 정밀한 단계입니다.
        CreditRiskCalculator.IrbResult irbResult = riskCalculator.calculateRwaIrb(
                result.getPd(),
                result.getLgd(),
                result.getEadStar(),
                maturityYears,
                irbParams,
                result.getAccount().getCustomer().getCustomerType(),
                result.getAccount().getCustomer().getAnnualSales(),
                result.getAccount().getCustomer().getFinancialSectorCode() // [고도화] 업권 코드 추가 반영
        );

        // 4. 비기대 손실(UL) 산출
        // 💡 [UL] 평상시(평균) 예측을 벗어나는 갑작스러운 대규모 손실에 대한 대비액입니다.
        BigDecimal unexpectedLoss = riskCalculator.calculateUnexpectedLoss(
                result.getPd(), 
                result.getLgd(), 
                result.getEadStar()
        );

        // 결과 업데이트 및 최종 마감
        // 💡 [데이터 저장] 계산된 모든 리스크 값들을 결과 객체에 담습니다.
        result.setAppliedRw(appliedRw);
        result.setRwaSa(rwaSa);
        result.setRwaIrb(irbResult.getRwa());
        result.setKValue(irbResult.getKValue());
        result.setRValue(irbResult.getRValue());
        result.setMaturityAdj(irbResult.getMaturityAdj());
        result.setUnexpectedLoss(unexpectedLoss);
        
        // 💡 [상태 변경] 이 계좌의 모든 리스크 산출 공정이 끝났음을 표시합니다.
        result.setStatus(CalculationStatus.COMPLETED);
        result.setCalculationCompletedAt(LocalDateTime.now());

        return result;
    }
}
