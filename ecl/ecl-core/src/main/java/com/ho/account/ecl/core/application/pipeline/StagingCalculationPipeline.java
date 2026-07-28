package com.ho.account.ecl.core.application.pipeline;

import com.ho.account.ecl.core.application.service.calculation.AllowanceParameterService;
import com.ho.account.ecl.core.application.service.calculation.PdCalculationService;
import com.ho.account.ecl.core.application.service.calculation.StagingService;
import com.ho.account.ecl.core.domain.calculator.AllowanceModelParams;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import com.ho.account.ecl.core.domain.result.AllowanceEclResult;
import com.ho.account.shared.finance.enums.CalculationStatus;
import com.ho.account.shared.finance.enums.CrStaging;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * IFRS 9 대손충당금 산출의 첫 번째 core pipeline.
 *
 * <p>초보자 설명: batch의 ItemProcessor는 이 클래스를 호출하는 어댑터이고,
 * 이 클래스가 실제 업무 순서인 Stage 판정과 PD 산출을 책임진다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StagingCalculationPipeline {

    private final StagingService stagingService;
    private final PdCalculationService pdCalculationService;
    private final AllowanceParameterService parameterService;

    public AllowanceEclResult prepare(CrAccount account, LocalDate baseDate) {
        AllowanceModelParams modelParams = parameterService.getParameters();

        log.debug("[IFRS9 Allowance] Stage/PD 산출 시작 - accountNo={}", account.getAccountNo());

        CrStaging stage = stagingService.determineStage(
                account,
                account.getCustomer().getWarningLevel(),
                Boolean.TRUE.equals(account.getIsDebtRestructured())
        );
        BigDecimal initialPd = pdCalculationService.calculatePd(account, modelParams);

        return AllowanceEclResult.builder()
                .id(account.getId())
                .baseDate(baseDate)
                .account(account)
                .staging(stage)
                .pd(initialPd)
                .status(CalculationStatus.RUNNING)
                .calculationCompletedAt(LocalDateTime.now())
                .build();
    }
}