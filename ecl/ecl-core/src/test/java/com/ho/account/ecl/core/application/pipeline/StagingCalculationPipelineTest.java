package com.ho.account.ecl.core.application.pipeline;

import com.ho.account.ecl.core.application.service.calculation.AllowanceParameterService;
import com.ho.account.ecl.core.application.service.calculation.PdCalculationService;
import com.ho.account.ecl.core.application.service.calculation.StagingService;
import com.ho.account.ecl.core.domain.calculator.AllowanceModelParams;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import com.ho.account.ecl.core.domain.exposure.CrCustomer;
import com.ho.account.ecl.core.domain.result.AllowanceEclResult;
import com.ho.account.shared.finance.enums.CalculationStatus;
import com.ho.account.shared.finance.enums.CrStaging;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StagingCalculationPipelineTest {

    @Mock private StagingService stagingService;
    @Mock private PdCalculationService pdCalculationService;
    @Mock private AllowanceParameterService parameterService;

    @InjectMocks
    private StagingCalculationPipeline pipeline;

    @Test
    void prepareDeterminesStageAndInitialPdInCore() {
        LocalDate baseDate = LocalDate.of(2026, 4, 30);
        CrCustomer customer = CrCustomer.builder()
                .warningLevel("WARNING")
                .internalRating("A")
                .build();
        CrAccount account = CrAccount.builder()
                .id(10L)
                .accountNo("ACC-10")
                .customer(customer)
                .isDebtRestructured(false)
                .build();
        AllowanceModelParams params = AllowanceModelParams.builder().build();

        when(parameterService.getParameters()).thenReturn(params);
        when(stagingService.determineStage(account, "WARNING", false)).thenReturn(CrStaging.STAGE2);
        when(pdCalculationService.calculatePd(account, params)).thenReturn(new BigDecimal("0.02000000"));

        AllowanceEclResult result = pipeline.prepare(account, baseDate);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getBaseDate()).isEqualTo(baseDate);
        assertThat(result.getStaging()).isEqualTo(CrStaging.STAGE2);
        assertThat(result.getPd()).isEqualByComparingTo(new BigDecimal("0.02000000"));
        assertThat(result.getStatus()).isEqualTo(CalculationStatus.RUNNING);
    }
}