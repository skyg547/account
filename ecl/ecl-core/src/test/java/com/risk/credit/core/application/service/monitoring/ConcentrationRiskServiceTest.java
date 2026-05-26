package com.risk.credit.core.application.service.monitoring;

import com.risk.credit.core.application.port.out.CrAccountRepository;
import com.risk.credit.core.domain.exposure.CrAccount;
import com.risk.credit.core.domain.exposure.CrCustomer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * [Test] 집중리스크 분석 서비스 단위 테스트
 */
class ConcentrationRiskServiceTest {

    @Mock
    private CrAccountRepository accountRepository;
    @InjectMocks
    private ConcentrationRiskService concentrationRiskService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("✅ 산업별 HHI 지수 산출 검증 (50:50 비중 시 HHI 5,000)")
    void calculateIndustryHhi_FiftyFifty() {
        // Given
        CrAccount acc1 = CrAccount.builder()
                .outstandingAmount(new BigDecimal("50000000"))
                .customer(CrCustomer.builder().industryCode("IND-A").build())
                .build();

        CrAccount acc2 = CrAccount.builder()
                .outstandingAmount(new BigDecimal("50000000"))
                .customer(CrCustomer.builder().industryCode("IND-B").build())
                .build();

        when(accountRepository.findByIsActiveTrue()).thenReturn(Arrays.asList(acc1, acc2));

        // When
        BigDecimal hhi = concentrationRiskService.calculateIndustryHhi();

        // Then
        assertEquals(0, hhi.compareTo(new BigDecimal("5000.00")));
    }

    @Test
    @DisplayName("✅ 산업별 HHI 지수 산출 검증 (완전 분산 시 HHI 하락)")
    void calculateIndustryHhi_Diversified() {
        // Given: 4개 산업에 25%씩 분산
        List<CrAccount> accounts = Arrays.asList(
                createAcc("IND-1", 25), createAcc("IND-2", 25),
                createAcc("IND-3", 25), createAcc("IND-4", 25));
        when(accountRepository.findByIsActiveTrue()).thenReturn(accounts);

        // When
        BigDecimal hhi = concentrationRiskService.calculateIndustryHhi();

        // Then
        assertEquals(0, hhi.compareTo(new BigDecimal("2500.00")));
    }

    private CrAccount createAcc(String indCode, long amt) {
        return CrAccount.builder()
                .outstandingAmount(new BigDecimal(amt))
                .customer(CrCustomer.builder().industryCode(indCode).build())
                .build();
    }
}