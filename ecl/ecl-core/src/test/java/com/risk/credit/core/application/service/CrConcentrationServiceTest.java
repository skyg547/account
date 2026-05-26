package com.risk.credit.core.application.service;

import com.risk.credit.core.application.port.out.CrAccountRepository;
import com.risk.credit.core.domain.exposure.CrAccount;
import com.risk.credit.core.domain.exposure.CrCustomer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * [QA] 교차 애플리케이션 서비스인 `CrConcentrationService`의 집중도 분석 테스트.
 *
 * <p>이 테스트는 계산 엔진이 아니라, 계좌 데이터를 산업별로 재구성해
 * 경고 기준을 판단하는 애플리케이션 흐름이 올바른지 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
class CrConcentrationServiceTest {

    @Mock
    private CrAccountRepository accountRepository;

    @InjectMocks
    private CrConcentrationService concentrationService;

    @Test
    @DisplayName("✅ [집중도 분석] 특정 산업 비중이 20%를 초과할 때 한도 초과 알림 확인")
    void analyzeIndustryConcentration_WhenLimitExceeded_ShouldMarkIsLimitExceeded() {
        String baseDate = "2026-04-18";

        CrAccount accountA = createMockAccount("I-01", "30000000000");
        CrAccount accountB = createMockAccount("I-02", "70000000000");

        when(accountRepository.findByIsActiveTrue()).thenReturn(List.of(accountA, accountB));

        Map<String, Object> analysis = concentrationService.analyzeIndustryConcentration(baseDate);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) analysis.get("items");

        Map<String, Object> itemA = items.stream()
                .filter(i -> "I-01".equals(i.get("industry")))
                .findFirst()
                .orElseThrow();

        assertThat((BigDecimal) itemA.get("weight")).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(itemA.get("isLimitExceeded")).isEqualTo(true);
    }

    /** 테스트용 산업 익스포저 계좌를 생성합니다. */
    private CrAccount createMockAccount(String industryCode, String amountStr) {
        CrCustomer customer = new CrCustomer();
        customer.setIndustryCode(industryCode);

        return CrAccount.builder()
                .outstandingAmount(new BigDecimal(amountStr))
                .customer(customer)
                .build();
    }
}
