package com.ho.account.ecl.core.application.pipeline;

import com.ho.account.ecl.core.application.service.calculation.AllowanceParameterService;
import com.ho.account.ecl.core.application.service.calculation.CcfCalculationService;
import com.ho.account.ecl.core.application.service.calculation.EadCrmCalculationService;
import com.ho.account.ecl.core.application.service.calculation.LgdCalculationService;
import com.ho.account.ecl.core.domain.calculator.AllowanceModelParams;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import com.ho.account.ecl.core.domain.result.AllowanceEclResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * IFRS 9 대손충당금 산출의 EAD/CRM/LGD core pipeline.
 *
 * <p>초보자 설명: 담보와 미사용 한도를 반영하는 업무 판단은 batch가 아니라 core가 수행한다.
 * batch는 많은 데이터를 나누어 이 pipeline에 전달하는 역할만 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EadCrmCalculationPipeline {

    private final CcfCalculationService ccfCalculationService;
    private final EadCrmCalculationService eadCrmCalculationService;
    private final LgdCalculationService lgdCalculationService;
    private final AllowanceParameterService parameterService;

    public AllowanceEclResult apply(AllowanceEclResult result) {
        CrAccount account = result.getAccount();
        AllowanceModelParams modelParams = parameterService.getParameters();

        log.debug("[IFRS9 Allowance] EAD/LGD 산출 시작 - accountNo={}", account.getAccountNo());

        BigDecimal finalCcf = ccfCalculationService.calculateCcf(account.getProductCode());
        EadCrmCalculationService.EadCrmResult eadCrm =
                eadCrmCalculationService.calculateEadCrm(account, finalCcf, modelParams);

        BigDecimal finalLgd = lgdCalculationService.calculateLgd(
                account.getCustomer().getCustomerType().name(),
                eadCrm.getMajorCollateralType(),
                eadCrm.getTotalCollateralAmt().signum() > 0,
                modelParams
        );

        result.setEad(eadCrm.getEadRaw());
        result.setEadStar(eadCrm.getEadStar());
        result.setAppliedCcf(eadCrm.getAppliedCcf());
        result.setCrmDeduction(eadCrm.getCrmDeduction());
        result.setLgd(finalLgd);
        return result;
    }
}