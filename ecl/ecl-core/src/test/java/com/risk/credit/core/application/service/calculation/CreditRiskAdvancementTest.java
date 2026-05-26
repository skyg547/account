package com.risk.credit.core.application.service.calculation;

import com.risk.credit.core.application.pipeline.RiskDataQualityService;
import com.risk.credit.core.application.port.out.*;
import com.risk.credit.core.application.service.crm.CollateralAllocationService;
import com.risk.credit.core.domain.calculator.CreditRiskCalculator;
import com.risk.credit.core.domain.calculator.EadCalculator;
import com.risk.credit.core.domain.calculator.IrbRegulatoryParams;
import com.risk.credit.core.domain.exposure.CrAccount;
import com.risk.credit.core.domain.exposure.CrCustomer;
import com.risk.credit.core.domain.model.CrGradeMaster;
import com.risk.credit.core.domain.model.SaRwMapper;
import com.risk.credit.core.domain.result.CrRiskResult;
import com.risk.common.enums.CrStaging;
import com.risk.common.enums.CustomerType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

/**
 * [Test] 신용리스크 산출 고도화 기능 검증 테스트
 * 1. 계좌 레벨 등급 우선 순위 적용 확인
 * 2. 연체 발생 시 동적 PD 할증(Penalty) 확인
 */
@ExtendWith(MockitoExtension.class)
class CreditRiskAdvancementTest {

    @Mock private CrAccountRepository accountRepository;
    @Mock private CrCustomerRepository customerRepository;
    @Mock private CrProductMasterRepository productMasterRepository;
    @Mock private CrAccountCollateralRepository accountCollateralRepository;
    @Mock private CrRiskResultRepository riskResultRepository;
    @Mock private CrGradeMasterRepository gradeMasterRepository;
    @Mock private CrLgdSegmentMasterRepository lgdSegmentMasterRepository;
    @Mock private CollateralAllocationService collateralAllocationService;
    @Mock private StagingService stagingService;
    @Mock private CreditRiskCalculator riskCalculator;
    @Mock private EadCalculator eadCalculator;
    @Mock private SaRwMapper saRwMapper;
    @Mock private RiskDataQualityService dqService;
    @Mock private LifetimePdService lifetimePdService;
    @Mock private IrbParameterService irbParameterService;
    @Mock private ForwardLookingEclService forwardLookingEclService;

    @InjectMocks
    private CreditRiskService creditRiskService;

    @Test
    @DisplayName("✅ 고도화 검증: 계좌 레벨 등급이 차주 등급보다 우선 적용되어야 함")
    void verify_AccountLevelRating_Priority() {
        // given
        Long accountId = 10L;
        LocalDate baseDate = LocalDate.of(2026, 4, 16);
        
        CrCustomer customer = CrCustomer.builder()
                .internalRating("B") // 차주 등급은 낮음
                .customerType(CustomerType.CORPORATE)
                .build();

        CrAccount account = CrAccount.builder()
                .id(accountId)
                .accountNo("ADV-001")
                .customer(customer)
                .internalRating("AAA") // [고도화] 계좌 등급은 최상위
                .productCode("PROD-01")
                .isActive(true)
                .openDate(LocalDate.now().minusYears(1))
                .build();

        IrbRegulatoryParams irbParams = IrbRegulatoryParams.builder()
                .pdFloor(new BigDecimal("0.0005"))
                .unsecuredLgdFloor(new BigDecimal("0.45"))
                .build();

        setupCommonMocks(accountId, account, irbParams);

        // AAA 등급에 대한 PD가 조회되는지 확인
        given(gradeMasterRepository.findByRatingCode("AAA")).willReturn(Optional.of(
                CrGradeMaster.builder().pdValue(new BigDecimal("0.0001")).build()
        ));

        // when
        CrRiskResult result = creditRiskService.calculateAccountRisk(accountId, baseDate);

        // then
        // AAA 등급의 PD인 0.0001이 조회되었지만, PD Floor(0.0005)에 의해 최종 PD는 0.0005가 되어야 함
        assertThat(result.getPd()).isEqualTo(new BigDecimal("0.0005"));
    }

    @Test
    @DisplayName("✅ 고도화 검증: 연체 30일 이상 시 PD가 동적으로 할증되어야 함")
    void verify_DynamicPdPenalty_ForDelinquency() {
        // given
        Long accountId = 11L;
        LocalDate baseDate = LocalDate.of(2026, 4, 16);
        
        CrCustomer customer = CrCustomer.builder()
                .internalRating("BBB")
                .customerType(CustomerType.CORPORATE)
                .build();

        CrAccount account = CrAccount.builder()
                .id(accountId)
                .accountNo("PENALTY-001")
                .customer(customer)
                .delinquentDays(45) // [고도화] 연체 30일 초과 시나리오
                .productCode("PROD-01")
                .isActive(true)
                .openDate(LocalDate.now().minusYears(1))
                .build();

        IrbRegulatoryParams irbParams = IrbRegulatoryParams.builder()
                .pdFloor(new BigDecimal("0.0005"))
                .unsecuredLgdFloor(new BigDecimal("0.45"))
                .build();

        setupCommonMocks(accountId, account, irbParams);

        // BBB 등급의 일반 PD = 0.01 (1%)
        BigDecimal basePd = new BigDecimal("0.01");
        given(gradeMasterRepository.findByRatingCode("BBB")).willReturn(Optional.of(
                CrGradeMaster.builder().pdValue(basePd).build()
        ));

        // when
        CrRiskResult result = creditRiskService.calculateAccountRisk(accountId, baseDate);

        // then
        // 연체 페널티 로직: basePd * 2.0 (0.01 * 2 = 0.02) 가 적용되어야 함
        assertThat(result.getPd()).isGreaterThan(basePd);
        assertThat(result.getPd()).isEqualTo(new BigDecimal("0.0200"));
    }

    private void setupCommonMocks(Long accountId, CrAccount account, IrbRegulatoryParams irbParams) {
        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));
        given(dqService.validate(any())).willReturn(true);
        given(irbParameterService.getParameters()).willReturn(irbParams);
        given(stagingService.determineStage(any(), any(), anyBoolean())).willReturn(CrStaging.STAGE1);
        given(productMasterRepository.findByProductCode(any())).willReturn(Optional.empty());
        given(lifetimePdService.generateMarginalPdCurve(any(), anyDouble(), anyString(), any())).willReturn(new ArrayList<>());
        given(accountCollateralRepository.findByAccount(any())).willReturn(new ArrayList<>());
        given(lgdSegmentMasterRepository.findByCustomerTypeAndCollateralType(anyString(), anyString())).willReturn(Optional.empty());
        
        Object[] eadResults = new Object[]{new BigDecimal("100"), new BigDecimal("0.75"), new BigDecimal("100"), new BigDecimal("0")};
        given(eadCalculator.calculateAdvancedEAD(any(), any(), any(), any(), any(), any(), any())).willReturn(eadResults);

        given(forwardLookingEclService.calculateWeightedEcl(any(), any(), any(), any(), any(), anyInt()))
                .willReturn(ForwardLookingEclService.FlEclResult.builder().weightedEcl(BigDecimal.ZERO).build());
        
        given(saRwMapper.getStandardRw(any(), any())).willReturn(new BigDecimal("1.0"));
        given(riskCalculator.calculateRwaSa(any(), any())).willReturn(BigDecimal.ZERO);
        
        given(riskCalculator.calculateRwaIrb(any(), any(), any(), anyDouble(), any(), any(), any(), any()))
                .willReturn(CreditRiskCalculator.IrbResult.builder().rwa(BigDecimal.ZERO).build());
        
        given(riskResultRepository.save(any(CrRiskResult.class))).willAnswer(i -> i.getArgument(0));
    }
}
