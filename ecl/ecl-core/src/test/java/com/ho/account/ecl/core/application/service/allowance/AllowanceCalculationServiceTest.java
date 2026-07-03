package com.ho.account.ecl.core.application.service.allowance;

import com.ho.account.ecl.core.application.pipeline.AllowanceDataQualityService;
import com.ho.account.ecl.core.application.pipeline.EadCrmCalculationPipeline;
import com.ho.account.ecl.core.application.pipeline.ForwardLookingEclCalculationPipeline;
import com.ho.account.ecl.core.application.pipeline.StagingCalculationPipeline;
import com.ho.account.ecl.core.application.port.out.AllowanceEclResultRepository;
import com.ho.account.ecl.core.application.port.out.CrAccountRepository;
import com.ho.account.ecl.core.application.port.out.CrBulkOperationPort;
import com.ho.account.ecl.core.application.service.calculation.AllowanceParameterService;
import com.ho.account.ecl.core.application.service.calculation.CcfCalculationService;
import com.ho.account.ecl.core.application.service.calculation.ForwardLookingEclService;
import com.ho.account.ecl.core.application.service.calculation.LifetimePdService;
import com.ho.account.ecl.core.application.service.calculation.PdCalculationService;
import com.ho.account.ecl.core.application.service.calculation.StagingService;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import com.ho.account.ecl.core.domain.result.AllowanceEclResult;
import com.ho.account.shared.finance.enums.CalculationStatus;
import com.ho.account.shared.finance.enums.CrStaging;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
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
    @Mock private CcfCalculationService ccfCalculationService;
    @Mock private LifetimePdService lifetimePdService;
    @Mock private ForwardLookingEclService forwardLookingEclService;
    @Mock private AllowanceParameterService parameterService;
    @Mock private StagingCalculationPipeline stagingCalculationPipeline;
    @Mock private EadCrmCalculationPipeline eadCrmCalculationPipeline;
    @Mock private ForwardLookingEclCalculationPipeline forwardLookingEclCalculationPipeline;

    @InjectMocks
    private AllowanceCalculationService service;

    @Test
    void calculateAccountAllowance_usesCorePipelinesAndSavesCompletedResult() {
        LocalDate baseDate = LocalDate.of(2026, 4, 30);
        CrAccount account = CrAccount.builder()
                .id(10L)
                .accountNo("ACC-10")
                .isActive(true)
                .build();
        AllowanceEclResult staged = AllowanceEclResult.builder()
                .id(10L)
                .baseDate(baseDate)
                .account(account)
                .staging(CrStaging.STAGE2)
                .pd(new BigDecimal("0.02000000"))
                .status(CalculationStatus.RUNNING)
                .build();
        AllowanceEclResult withEad = AllowanceEclResult.builder()
                .id(10L)
                .baseDate(baseDate)
                .account(account)
                .staging(CrStaging.STAGE2)
                .pd(new BigDecimal("0.02000000"))
                .eadStar(new BigDecimal("1200.0000"))
                .lgd(new BigDecimal("0.45000000"))
                .status(CalculationStatus.RUNNING)
                .build();
        AllowanceEclResult completed = AllowanceEclResult.builder()
                .id(10L)
                .baseDate(baseDate)
                .account(account)
                .staging(CrStaging.STAGE2)
                .pd(new BigDecimal("0.02000000"))
                .eadStar(new BigDecimal("1200.0000"))
                .weightedEcl(new BigDecimal("10.5000"))
                .expectedLoss(new BigDecimal("10.5000"))
                .status(CalculationStatus.RUNNING)
                .build();

        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(dataQualityService.validate(account)).thenReturn(true);
        when(stagingCalculationPipeline.prepare(account, baseDate)).thenReturn(staged);
        when(eadCrmCalculationPipeline.apply(staged)).thenReturn(withEad);
        when(forwardLookingEclCalculationPipeline.apply(withEad, baseDate)).thenReturn(completed);

        service.calculateAccountAllowance(10L, baseDate);

        InOrder order = inOrder(stagingCalculationPipeline, eadCrmCalculationPipeline, forwardLookingEclCalculationPipeline);
        order.verify(stagingCalculationPipeline).prepare(account, baseDate);
        order.verify(eadCrmCalculationPipeline).apply(staged);
        order.verify(forwardLookingEclCalculationPipeline).apply(withEad, baseDate);

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