package com.ho.account.ecl.core.application.pipeline;

import com.ho.account.ecl.core.application.service.calculation.AllowanceParameterService;
import com.ho.account.ecl.core.application.service.calculation.ForwardLookingEclService;
import com.ho.account.ecl.core.application.service.calculation.LifetimePdService;
import com.ho.account.ecl.core.domain.calculator.AllowanceModelParams;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import com.ho.account.ecl.core.domain.result.AllowanceEclResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * IFRS 9 대손충당금 산출의 미래전망 ECL core pipeline.
 *
 * <p>초보자 설명: 이미 산출된 Stage, PD, EAD, LGD를 거시경제 시나리오와 결합해
 * closing이 사용할 weighted ECL을 채운다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ForwardLookingEclCalculationPipeline {

    private final LifetimePdService lifetimePdService;
    private final ForwardLookingEclService forwardLookingEclService;
    private final AllowanceParameterService parameterService;

    public AllowanceEclResult apply(AllowanceEclResult result, LocalDate baseDate) {
        CrAccount account = result.getAccount();
        AllowanceModelParams modelParams = parameterService.getParameters();

        log.debug("[IFRS9 Allowance] 미래전망 ECL 산출 시작 - accountNo={}", account.getAccountNo());

        BigDecimal maturityYears = account.resolveMaturityYears(baseDate);
        List<BigDecimal> marginalPds = lifetimePdService.generateMarginalPdCurve(
                result.getPd(),
                maturityYears,
                resolveRating(account),
                baseDate
        );

        ForwardLookingEclService.FlEclResult flEcl = forwardLookingEclService.calculateWeightedEcl(
                result.getStaging(),
                marginalPds,
                result.getLgd(),
                result.getEadStar(),
                modelParams.getDefaultDiscountRate(),
                baseDate.getYear()
        );

        result.setEclBoom(flEcl.getEclBoom());
        result.setEclBase(flEcl.getEclBase());
        result.setEclRecession(flEcl.getEclRecession());
        result.setWeightedEcl(flEcl.getWeightedEcl());
        result.setExpectedLoss(flEcl.getWeightedEcl());
        return result;
    }

    private String resolveRating(CrAccount account) {
        if (account.getInternalRating() != null && !account.getInternalRating().isBlank()) {
            return account.getInternalRating();
        }
        return account.getCustomer().getInternalRating();
    }
}