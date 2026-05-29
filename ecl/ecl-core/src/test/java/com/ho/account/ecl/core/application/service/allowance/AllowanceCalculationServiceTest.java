package com.ho.account.ecl.core.application.service.allowance;

import com.ho.account.ecl.core.application.pipeline.AllowanceDataQualityService;
import com.ho.account.ecl.core.application.port.out.CrAccountRepository;
import com.ho.account.ecl.core.application.port.out.CrBulkOperationPort;
import com.ho.account.ecl.core.application.port.out.AllowanceEclResultRepository;
import com.ho.account.ecl.core.application.service.calculation.CcfCalculationService;
import com.ho.account.ecl.core.application.service.calculation.EadCrmCalculationService;
import com.ho.account.ecl.core.application.service.calculation.ForwardLookingEclService;
import com.ho.account.ecl.core.application.service.calculation.AllowanceParameterService;
import com.ho.account.ecl.core.application.service.calculation.LgdCalculationService;
import com.ho.account.ecl.core.application.service.calculation.LifetimePdService;
import com.ho.account.ecl.core.application.service.calculation.PdCalculationService;
import com.ho.account.ecl.core.application.service.calculation.StagingService;
import com.ho.account.ecl.core.domain.calculator.AllowanceModelParams;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import com.ho.account.ecl.core.domain.exposure.CrCustomer;
import com.ho.account.ecl.core.domain.result.AllowanceEclResult;
import com.ho.account.shared.finance.enums.CalculationStatus;
import com.ho.account.shared.finance.enums.CrStaging;
import com.ho.account.shared.finance.enums.CustomerType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllowanceCalculationServiceTest {

    @Mock private CrAccountRepository accountRepository;
    @Mock private AllowanceEclResultRepository resultRepository;
    @Mock private AllowanceDataQualityService dataQualityService;
    @Mock private CrBulkOperationPort bulkOperationPort;
    @Mock private StagingService stagingService;
    @Mock private PdCalculationService pdCalculationService;
    @Mock private LgdCalculationService lgdCalculationService;
    @Mock private CcfCalculationService ccfCalculationService;
    @Mock private EadCrmCalculationService eadCrmCalculationService;
    @Mock private LifetimePdService lifetimePdService;
    @Mock private ForwardLookingEclService forwardLookingEclService;
    @Mock private AllowanceParameterService parameterService;

    @InjectMocks
    private AllowanceCalculationService service;

    @Test
    void calculateAccountAllowance_savesOnlyIfrs9AllowanceFields() {
        LocalDate baseDate = LocalDate.of(2026, 4, 30);
        CrCustomer customer = CrCustomer.builder()
                .customerType(CustomerType.CORPORATE)
                .internalRating("A")
                .warningLevel("NORMAL")
                .build();
        CrAccount account = CrAccount.builder()
                .id(10L)
                .accountNo("ACC-10")
                .customer(customer)
                .productCode("LOAN-A")
                .outstandingAmount(new BigDecimal("1000.00"))
                .notionalAmount(new BigDecimal("1200.00"))
                .maturityDate(LocalDate.of(2028, 4, 30))
                .isActive(true)
                .build();
        AllowanceModelParams params = AllowanceModelParams.builder()
                .defaultDiscountRate(new BigDecimal("0.05"))
                .build();
        EadCrmCalculationService.EadCrmResult ead = EadCrmCalculationService.EadCrmResult.builder()
                .eadRaw(new BigDecimal("1200.0000"))
                .eadStar(new BigDecimal("1200.0000"))
                .appliedCcf(new BigDecimal("0.500000"))
                .crmDeduction(BigDecimal.ZERO)
                .totalCollateralAmt(BigDecimal.ZERO)
                .majorCollateralType("UNSECURED")
                .build();
        ForwardLookingEclService.FlEclResult ecl = ForwardLookingEclService.FlEclResult.builder()
                .weightedEcl(new BigDecimal("10.5000"))
                .eclBoom(new BigDecimal("8.0000"))
                .eclBase(new BigDecimal("10.0000"))
                .eclRecession(new BigDecimal("15.0000"))
                .build();

        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(dataQualityService.validate(account)).thenReturn(true);
        when(parameterService.getParameters()).thenReturn(params);
        when(stagingService.determineStage(any(), any(), anyBoolean())).thenReturn(CrStaging.STAGE2);
        when(pdCalculationService.calculatePd(account, params)).thenReturn(new BigDecimal("0.02000000"));
        when(ccfCalculationService.calculateCcf("LOAN-A")).thenReturn(new BigDecimal("0.500000"));
        when(eadCrmCalculationService.calculateEadCrm(account, new BigDecimal("0.500000"), params)).thenReturn(ead);
        when(lgdCalculationService.calculateLgd("CORPORATE", "UNSECURED", false, params))
                .thenReturn(new BigDecimal("0.45000000"));
        double maturityYears = account.resolveMaturityYears(baseDate);
        when(lifetimePdService.generateMarginalPdCurve(
                new BigDecimal("0.02000000"), maturityYears, "A", baseDate))
                .thenReturn(List.of(new BigDecimal("0.01000000"), new BigDecimal("0.02000000")));
        when(forwardLookingEclService.calculateWeightedEcl(
                CrStaging.STAGE2,
                List.of(new BigDecimal("0.01000000"), new BigDecimal("0.02000000")),
                new BigDecimal("0.45000000"),
                new BigDecimal("1200.0000"),
                new BigDecimal("0.05"),
                2026))
                .thenReturn(ecl);

        service.calculateAccountAllowance(10L, baseDate);

        ArgumentCaptor<AllowanceEclResult> captor = ArgumentCaptor.forClass(AllowanceEclResult.class);
        verify(resultRepository).save(captor.capture());
        AllowanceEclResult saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(CalculationStatus.COMPLETED);
        assertThat(saved.getWeightedEcl()).isEqualByComparingTo(new BigDecimal("10.5000"));
        assertThat(saved.getExpectedLoss()).isEqualByComparingTo(new BigDecimal("10.5000"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllowanceSummary_usesWeightedEclAsTargetAllowance() {
        LocalDate baseDate = LocalDate.of(2026, 4, 30);
        when(resultRepository.findAllByBaseDate(baseDate)).thenReturn(List.of(
                AllowanceEclResult.builder()
                        .baseDate(baseDate)
                        .staging(CrStaging.STAGE1)
                        .eadStar(new BigDecimal("100.0000"))
                        .weightedEcl(new BigDecimal("1.0000"))
                        .pd(new BigDecimal("0.01000000"))
                        .build(),
                AllowanceEclResult.builder()
                        .baseDate(baseDate)
                        .staging(CrStaging.STAGE2)
                        .eadStar(new BigDecimal("200.0000"))
                        .weightedEcl(new BigDecimal("5.0000"))
                        .pd(new BigDecimal("0.02000000"))
                        .build()));

        Map<String, Object> summary = (Map<String, Object>) service.getAllowanceSummary(baseDate);

        assertThat((BigDecimal) summary.get("totalExposure")).isEqualByComparingTo(new BigDecimal("300.0000"));
        assertThat((BigDecimal) summary.get("targetAllowanceAmount")).isEqualByComparingTo(new BigDecimal("6.0000"));
        assertThat((Map<String, Long>) summary.get("stageSummary"))
                .containsEntry("STAGE1", 1L)
                .containsEntry("STAGE2", 1L);
    }
}

