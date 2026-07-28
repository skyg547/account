package com.ho.account.ecl.core.application.pipeline;

import com.ho.account.ecl.core.application.service.calculation.AllowanceParameterService;
import com.ho.account.ecl.core.application.service.calculation.CcfCalculationService;
import com.ho.account.ecl.core.application.service.calculation.EadCrmCalculationService;
import com.ho.account.ecl.core.application.service.calculation.LgdCalculationService;
import com.ho.account.ecl.core.domain.calculator.AllowanceModelParams;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import com.ho.account.ecl.core.domain.exposure.CrCustomer;
import com.ho.account.ecl.core.domain.result.AllowanceEclResult;
import com.ho.account.shared.finance.enums.CustomerType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EadCrmCalculationPipelineTest {

    @Mock private CcfCalculationService ccfCalculationService;
    @Mock private EadCrmCalculationService eadCrmCalculationService;
    @Mock private LgdCalculationService lgdCalculationService;
    @Mock private AllowanceParameterService parameterService;

    @InjectMocks
    private EadCrmCalculationPipeline pipeline;

    @Test
    void applyCalculatesEadCrmAndLgdInCore() {
        CrCustomer customer = CrCustomer.builder()
                .customerType(CustomerType.CORPORATE)
                .build();
        CrAccount account = CrAccount.builder()
                .accountNo("ACC-10")
                .productCode("LOAN-A")
                .customer(customer)
                .build();
        AllowanceEclResult result = AllowanceEclResult.builder()
                .account(account)
                .build();
        AllowanceModelParams params = AllowanceModelParams.builder().build();
        EadCrmCalculationService.EadCrmResult eadCrm = EadCrmCalculationService.EadCrmResult.builder()
                .eadRaw(new BigDecimal("1200.0000"))
                .eadStar(new BigDecimal("1000.0000"))
                .appliedCcf(new BigDecimal("0.500000"))
                .crmDeduction(new BigDecimal("200.0000"))
                .totalCollateralAmt(new BigDecimal("300.0000"))
                .majorCollateralType("APARTMENT")
                .build();

        when(parameterService.getParameters()).thenReturn(params);
        when(ccfCalculationService.calculateCcf("LOAN-A")).thenReturn(new BigDecimal("0.500000"));
        when(eadCrmCalculationService.calculateEadCrm(account, new BigDecimal("0.500000"), params)).thenReturn(eadCrm);
        when(lgdCalculationService.calculateLgd("CORPORATE", "APARTMENT", true, params))
                .thenReturn(new BigDecimal("0.25000000"));

        AllowanceEclResult applied = pipeline.apply(result);

        assertThat(applied.getEad()).isEqualByComparingTo(new BigDecimal("1200.0000"));
        assertThat(applied.getEadStar()).isEqualByComparingTo(new BigDecimal("1000.0000"));
        assertThat(applied.getAppliedCcf()).isEqualByComparingTo(new BigDecimal("0.500000"));
        assertThat(applied.getCrmDeduction()).isEqualByComparingTo(new BigDecimal("200.0000"));
        assertThat(applied.getLgd()).isEqualByComparingTo(new BigDecimal("0.25000000"));
    }
}