package com.ho.account.ecl.core.application.service.calculation;

import com.ho.account.ecl.core.application.pipeline.RiskDataQualityService;
import com.ho.account.ecl.core.application.service.crm.CollateralAllocationService;
import com.ho.account.ecl.core.application.service.crm.ApartmentCollateralService;
import com.ho.account.ecl.core.domain.model.SaRwMapper;
import com.ho.account.ecl.core.application.port.out.*;
import com.ho.account.ecl.core.domain.calculator.CreditRiskCalculator;
import com.ho.account.ecl.core.domain.calculator.IrbRegulatoryParams;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import com.ho.account.ecl.core.domain.exposure.CrCustomer;
import com.ho.account.ecl.core.domain.result.CrRiskResult;
import com.ho.account.shared.finance.enums.CalculationStatus;
import com.ho.account.shared.finance.enums.CrStaging;
import com.ho.account.shared.finance.enums.CustomerType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CreditRiskServiceTest {

    @Mock private CrAccountRepository accountRepository;
    @Mock private CrCustomerRepository customerRepository;
    @Mock private CrProductMasterRepository productMasterRepository;
    @Mock private CrAccountCollateralRepository accountCollateralRepository;
    @Mock private CrRiskResultRepository riskResultRepository;
    @Mock private CollateralAllocationService collateralAllocationService;
    @Mock private StagingService stagingService;
    @Mock private CreditRiskCalculator riskCalculator;
    @Mock private SaRwMapper saRwMapper;
    @Mock private RiskDataQualityService dqService;
    @Mock private LifetimePdService lifetimePdService;
    @Mock private IrbParameterService irbParameterService;
    @Mock private ForwardLookingEclService forwardLookingEclService;
    @Mock private CrBulkOperationPort bulkOperationPort;

    // [v2.5] 도메인 특화 산출 서비스 Mock 추가
    @Mock private PdCalculationService pdCalculationService;
    @Mock private LgdCalculationService lgdCalculationService;
    @Mock private CcfCalculationService ccfCalculationService;
    @Mock private EadCrmCalculationService eadCrmCalculationService;
    @Mock private ApartmentCollateralService apartmentCollateralService;

    @InjectMocks
    private CreditRiskService creditRiskService;

    @Test
    @DisplayName("✅ 정상 계좌 대손충당금(IFRS9) 산출 흐름 검증")
    void calculateAccountRisk_Success() {
        // given
        Long accountId = 1L;
        LocalDate baseDate = LocalDate.of(2024, 4, 14);
        
        CrCustomer customer = CrCustomer.builder()
                .id(100L).customerCode("CUST-001")
                .customerType(CustomerType.CORPORATE)
                .internalRating("A1")
                .build();

        CrAccount account = CrAccount.builder()
                .id(accountId).accountNo("ACC-0001")
                .outstandingAmount(new BigDecimal("100000000"))
                .notionalAmount(new BigDecimal("150000000"))
                .customer(customer).isActive(true).productCode("PROD-01")
                .openDate(LocalDate.now().minusYears(1))
                .build();

        IrbRegulatoryParams irbParams = IrbRegulatoryParams.builder().build();

        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));
        given(dqService.validate(any())).willReturn(true);
        given(irbParameterService.getParameters()).willReturn(irbParams);
        given(stagingService.determineStage(any(), any(), anyBoolean())).willReturn(CrStaging.STAGE1);
        given(pdCalculationService.calculatePd(any(), any())).willReturn(new BigDecimal("0.01"));
        given(lifetimePdService.generateMarginalPdCurve(any(), anyDouble(), anyString(), any())).willReturn(new ArrayList<>());
        given(ccfCalculationService.calculateCcf(any())).willReturn(new BigDecimal("0.75"));
        
        EadCrmCalculationService.EadCrmResult eadCrm = EadCrmCalculationService.EadCrmResult.builder()
                .eadStar(new BigDecimal("100000000"))
                .appliedCcf(new BigDecimal("0.75"))
                .eadRaw(new BigDecimal("100000000"))
                .crmDeduction(BigDecimal.ZERO)
                .majorCollateralType("UNSECURED")
                .totalCollateralAmt(BigDecimal.ZERO)
                .build();
        given(eadCrmCalculationService.calculateEadCrm(any(), any(), any())).willReturn(eadCrm);
        
        given(lgdCalculationService.calculateLgd(anyString(), anyString(), anyBoolean(), any())).willReturn(new BigDecimal("0.45"));

        ForwardLookingEclService.FlEclResult flEclResult = ForwardLookingEclService.FlEclResult.builder()
                .weightedEcl(new BigDecimal("10000")).build();
        given(forwardLookingEclService.calculateWeightedEcl(any(), any(), any(), any(), any(), anyInt())).willReturn(flEclResult);
        
        given(saRwMapper.getStandardRw(any(), any())).willReturn(new BigDecimal("0.75"));
        given(riskCalculator.calculateRwaSa(any(), any())).willReturn(new BigDecimal("75000000"));
        
        CreditRiskCalculator.IrbResult irbResult = CreditRiskCalculator.IrbResult.builder()
                .rwa(new BigDecimal("70000000")).build();
        given(riskCalculator.calculateRwaIrb(any(), any(), any(), anyDouble(), any(), any(), any(), any())).willReturn(irbResult);
        given(riskCalculator.calculateUnexpectedLoss(any(), any(), any())).willReturn(new BigDecimal("5000"));

        // when
        CrRiskResult result = creditRiskService.calculateAccountRisk(accountId, baseDate);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(CalculationStatus.COMPLETED);
    }

    @Test
    @DisplayName("✅ 담보가 있는 계좌의 CRM 차감 효과 검증")
    void calculateAccountRisk_WithCollaterals_Success() {
        // given
        Long accountId = 2L;
        LocalDate baseDate = LocalDate.of(2026, 4, 15);
        
        CrCustomer customer = CrCustomer.builder()
                .id(101L).customerCode("CUST-002")
                .customerType(CustomerType.CORPORATE)
                .build();

        CrAccount account = CrAccount.builder()
                .id(accountId).accountNo("ACC-0002")
                .customer(customer).isActive(true).productCode("LN-CORP")
                .openDate(LocalDate.now().minusYears(1))
                .build();

        IrbRegulatoryParams irbParams = IrbRegulatoryParams.builder().build();

        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));
        given(dqService.validate(any())).willReturn(true);
        given(irbParameterService.getParameters()).willReturn(irbParams);
        given(stagingService.determineStage(any(), any(), anyBoolean())).willReturn(CrStaging.STAGE1);
        given(pdCalculationService.calculatePd(any(), any())).willReturn(new BigDecimal("0.01"));
        given(lifetimePdService.generateMarginalPdCurve(any(), anyDouble(), anyString(), any())).willReturn(new ArrayList<>());
        given(ccfCalculationService.calculateCcf(any())).willReturn(new BigDecimal("0.75"));
        
        EadCrmCalculationService.EadCrmResult eadCrm = EadCrmCalculationService.EadCrmResult.builder()
                .eadStar(new BigDecimal("200000000"))
                .crmDeduction(new BigDecimal("800000000"))
                .majorCollateralType("REAL_ESTATE")
                .totalCollateralAmt(new BigDecimal("800000000"))
                .build();
        given(eadCrmCalculationService.calculateEadCrm(any(), any(), any())).willReturn(eadCrm);
        given(lgdCalculationService.calculateLgd(anyString(), anyString(), anyBoolean(), any())).willReturn(new BigDecimal("0.20"));

        given(forwardLookingEclService.calculateWeightedEcl(any(), any(), any(), any(), any(), anyInt()))
                .willReturn(ForwardLookingEclService.FlEclResult.builder().weightedEcl(BigDecimal.ZERO).build());
        given(saRwMapper.getStandardRw(any(), any())).willReturn(new BigDecimal("1.0"));
        given(riskCalculator.calculateRwaSa(any(), any())).willReturn(BigDecimal.ZERO);
        given(riskCalculator.calculateRwaIrb(any(), any(), any(), anyDouble(), any(), any(), any(), any()))
                .willReturn(CreditRiskCalculator.IrbResult.builder().rwa(BigDecimal.ZERO).build());
        given(riskCalculator.calculateUnexpectedLoss(any(), any(), any())).willReturn(BigDecimal.ZERO);

        // when
        CrRiskResult result = creditRiskService.calculateAccountRisk(accountId, baseDate);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getCrmDeduction()).isEqualTo(new BigDecimal("800000000"));
        assertThat(result.getEadStar()).isEqualTo(new BigDecimal("200000000"));
    }
}
