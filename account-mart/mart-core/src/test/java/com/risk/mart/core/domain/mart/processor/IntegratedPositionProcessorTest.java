package com.risk.mart.core.domain.mart.processor;

import com.risk.common.entity.IntegratedRiskPosition;
import com.risk.common.enums.CrStaging;
import com.risk.mart.core.domain.ods.common.entity.OdsCustomerMst;
import com.risk.mart.core.domain.ods.loan.entity.OdsAccountLedger;
import com.risk.mart.core.domain.ods.common.repository.OdsBalanceHistRepository;
import com.risk.mart.core.domain.ods.common.repository.OdsCustomerMstRepository;
import com.risk.mart.core.domain.ods.common.repository.OdsProductMstRepository;
import com.risk.mart.core.domain.ods.loan.repository.OdsCollateralMstRepository;
import com.risk.mart.core.domain.ods.audit.service.DataQualityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * [QA] IntegratedPositionProcessor 단위 테스트
 * ETL 변환 로직 및 IFRS 9 스테이징 판정 기준을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class IntegratedPositionProcessorTest {

    @Mock
    private OdsCustomerMstRepository customerRepository;
    @Mock
    private OdsProductMstRepository productRepository;
    @Mock
    private OdsCollateralMstRepository collateralRepository;
    @Mock
    private OdsBalanceHistRepository balanceHistRepository;
    @Mock
    private DataQualityService dqService;

    @InjectMocks
    private IntegratedPositionProcessor processor;

    @Test
    @DisplayName("정상 계좌 데이터가 STAGE 1으로 올바르게 분류되는지 확인")
    void shouldClassifyAsStage1WhenDelinquentDaysIsZero() throws Exception {
        // given
        OdsAccountLedger ledger = createBaseLedger("ACC001", 0);
        when(dqService.validateAccountLedger(any(), any())).thenReturn(true);
        when(customerRepository.findById(anyString())).thenReturn(Optional.empty());
        when(productRepository.findById(anyString())).thenReturn(Optional.empty());
        when(collateralRepository.findFirstByCustomerCode(anyString())).thenReturn(Optional.empty());

        // when
        IntegratedRiskPosition result = processor.process(Objects.requireNonNull(ledger));

        // then
        assertNotNull(result);
        assertEquals(CrStaging.STAGE1, result.getStaging());
        assertEquals("ACC001", result.getAccountNo());
    }

    @Test
    @DisplayName("연체일수 35일인 경우 STAGE 2(SICR)로 분류되는지 확인")
    void shouldClassifyAsStage2WhenDelinquentDaysIs35() throws Exception {
        // given
        OdsAccountLedger ledger = createBaseLedger("ACC002", 35);
        when(dqService.validateAccountLedger(any(), any())).thenReturn(true);

        // when
        IntegratedRiskPosition result = processor.process(Objects.requireNonNull(ledger));

        // then
        assertNotNull(result);
        assertEquals(CrStaging.STAGE2, result.getStaging());
    }

    @Test
    @DisplayName("연체일수 95일인 경우 STAGE 3(Default)으로 분류되는지 확인")
    void shouldClassifyAsStage3WhenDelinquentDaysIs95() throws Exception {
        // given
        OdsAccountLedger ledger = createBaseLedger("ACC003", 95);
        when(dqService.validateAccountLedger(any(), any())).thenReturn(true);

        // when
        IntegratedRiskPosition result = processor.process(Objects.requireNonNull(ledger));

        // then
        assertNotNull(result);
        assertEquals(CrStaging.STAGE3, result.getStaging());
    }

    @Test
    @DisplayName("고객 정보가 있는 경우 내부 등급 및 국가 코드가 매핑되는지 확인")
    void shouldEnrichCustomerInfo() throws Exception {
        // given
        OdsAccountLedger ledger = createBaseLedger("ACC004", 0);
        OdsCustomerMst customer = OdsCustomerMst.builder()
                .customerCode("CUST001")
                .ratingCode("AA")
                .countryCode("US")
                .build();

        when(dqService.validateAccountLedger(any(), any())).thenReturn(true);
        when(customerRepository.findById("CUST001")).thenReturn(Optional.of(customer));

        // when
        IntegratedRiskPosition result = processor.process(Objects.requireNonNull(ledger));

        // then
        assertNotNull(result);
        assertEquals("AA", result.getInternalRating());
        assertEquals("US", result.getCountryCode());
    }

    private OdsAccountLedger createBaseLedger(String accNo, Integer dpd) {
        return OdsAccountLedger.builder()
                .accountNo(accNo)
                .customerCode("CUST001")
                .productCode("PROD001")
                .currency("KRW")
                .outstandingAmount(new BigDecimal("1000000"))
                .limitAmount(new BigDecimal("2000000"))
                .delinquentDays(dpd)
                .isActive(true)
                .build();
    }
}
