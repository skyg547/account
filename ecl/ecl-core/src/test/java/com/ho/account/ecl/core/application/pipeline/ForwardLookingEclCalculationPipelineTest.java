package com.ho.account.ecl.core.application.pipeline;

import com.ho.account.ecl.core.application.service.calculation.AllowanceParameterService;
import com.ho.account.ecl.core.application.service.calculation.ForwardLookingEclService;
import com.ho.account.ecl.core.application.service.calculation.LifetimePdService;
import com.ho.account.ecl.core.domain.calculator.AllowanceModelParams;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import com.ho.account.ecl.core.domain.exposure.CrCustomer;
import com.ho.account.ecl.core.domain.result.AllowanceEclResult;
import com.ho.account.shared.finance.enums.CrStaging;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForwardLookingEclCalculationPipelineTest {

    @Mock private LifetimePdService lifetimePdService;
    @Mock private ForwardLookingEclService forwardLookingEclService;
    @Mock private AllowanceParameterService parameterService;

    @InjectMocks
    private ForwardLookingEclCalculationPipeline pipeline;

    @Test
    void applyCalculatesWeightedEclInCore() {
        LocalDate baseDate = LocalDate.of(2026, 4, 30);
        CrCustomer customer = CrCustomer.builder()
                .internalRating("A")
                .build();
        CrAccount account = CrAccount.builder()
                .accountNo("ACC-10")
                .customer(customer)
                .maturityDate(LocalDate.of(2028, 4, 30))
                .build();
        AllowanceEclResult result = AllowanceEclResult.builder()
                .account(account)
                .staging(CrStaging.STAGE2)
                .pd(new BigDecimal("0.02000000"))
                .lgd(new BigDecimal("0.45000000"))
                .eadStar(new BigDecimal("1200.0000"))
                .build();
        AllowanceModelParams params = AllowanceModelParams.builder()
                .defaultDiscountRate(new BigDecimal("0.05"))
                .build();
        List<BigDecimal> marginalPds = List.of(new BigDecimal("0.01000000"), new BigDecimal("0.02000000"));
        ForwardLookingEclService.FlEclResult ecl = ForwardLookingEclService.FlEclResult.builder()
                .weightedEcl(new BigDecimal("10.5000"))
                .eclBoom(new BigDecimal("8.0000"))
                .eclBase(new BigDecimal("10.0000"))
                .eclRecession(new BigDecimal("15.0000"))
                .build();

        when(parameterService.getParameters()).thenReturn(params);
        when(lifetimePdService.generateMarginalPdCurve(new BigDecimal("0.02000000"), account.resolveMaturityYears(baseDate), "A", baseDate))
                .thenReturn(marginalPds);
        when(forwardLookingEclService.calculateWeightedEcl(
                CrStaging.STAGE2,
                marginalPds,
                new BigDecimal("0.45000000"),
                new BigDecimal("1200.0000"),
                new BigDecimal("0.05"),
                2026))
                .thenReturn(ecl);

        AllowanceEclResult applied = pipeline.apply(result, baseDate);

        assertThat(applied.getWeightedEcl()).isEqualByComparingTo(new BigDecimal("10.5000"));
        assertThat(applied.getExpectedLoss()).isEqualByComparingTo(new BigDecimal("10.5000"));
        assertThat(applied.getEclBoom()).isEqualByComparingTo(new BigDecimal("8.0000"));
        assertThat(applied.getEclBase()).isEqualByComparingTo(new BigDecimal("10.0000"));
        assertThat(applied.getEclRecession()).isEqualByComparingTo(new BigDecimal("15.0000"));
    }
}