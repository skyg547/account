package com.ho.account.mart.core.domain.mart.processor;

import com.ho.account.mart.core.domain.mart.AllowanceInputPosition;
import com.ho.account.mart.core.domain.marketdata.ExchangeRate;
import com.ho.account.shared.finance.enums.CurrencyCode;
import com.ho.account.shared.finance.enums.CrStaging;
import com.ho.account.mart.core.application.port.out.OdsAccountRateRepository;
import com.ho.account.mart.core.application.port.out.OdsCustomerMstRepository;
import com.ho.account.mart.core.application.port.out.OdsEarlyWarningRepository;
import com.ho.account.mart.core.application.port.out.ExchangeRateRepository;
import com.ho.account.mart.core.domain.ods.common.OdsCustomerMst;
import com.ho.account.mart.core.domain.ods.loan.OdsAccountLedger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
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
    private OdsEarlyWarningRepository earlyWarningRepository;
    @Mock
    private OdsAccountRateRepository accountRateRepository;
    @Mock
    private OdsCustomerMstRepository customerMstRepository;
    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @InjectMocks
    private IntegratedPositionProcessor processor;

    @BeforeEach
    void setUp() {
        when(earlyWarningRepository.findTopByCustomerCodeAndBaseDateOrderByBaseDateDesc(
                anyString(), nullable(LocalDate.class))).thenReturn(Optional.empty());
        when(accountRateRepository.findById(anyString())).thenReturn(Optional.empty());
        when(customerMstRepository.findByCustomerCode(anyString())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("정상 계좌 데이터가 STAGE 1으로 올바르게 분류되는지 확인")
    void shouldClassifyAsStage1WhenDelinquentDaysIsZero() throws Exception {
        // given
        OdsAccountLedger ledger = createBaseLedger("ACC001", 0);

        // when
        AllowanceInputPosition result = processor.process(Objects.requireNonNull(ledger), LocalDate.of(2026, 4, 30));

        // then
        assertNotNull(result);
        assertEquals(CrStaging.STAGE1, result.getStaging());
        assertEquals("ACC001", result.getAccNo());
    }

    @Test
    @DisplayName("연체일수 35일인 경우 STAGE 2(SICR)로 분류되는지 확인")
    void shouldClassifyAsStage2WhenDelinquentDaysIs35() throws Exception {
        // given
        OdsAccountLedger ledger = createBaseLedger("ACC002", 35);

        // when
        AllowanceInputPosition result = processor.process(Objects.requireNonNull(ledger), LocalDate.of(2026, 4, 30));

        // then
        assertNotNull(result);
        assertEquals(CrStaging.STAGE2, result.getStaging());
    }

    @Test
    @DisplayName("연체일수 95일인 경우 STAGE 3(Default)으로 분류되는지 확인")
    void shouldClassifyAsStage3WhenDelinquentDaysIs95() throws Exception {
        // given
        OdsAccountLedger ledger = createBaseLedger("ACC003", 95);

        // when
        AllowanceInputPosition result = processor.process(Objects.requireNonNull(ledger), LocalDate.of(2026, 4, 30));

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
                .internalRating("AA")
                .countryCode("US")
                .build();

        when(customerMstRepository.findByCustomerCode("CUST001")).thenReturn(Optional.of(customer));

        // when
        AllowanceInputPosition result = processor.process(Objects.requireNonNull(ledger), LocalDate.of(2026, 4, 30));

        // then
        assertNotNull(result);
        assertEquals("AA", result.getInternalRating());
        assertEquals("US", result.getCountryCode());
    }

    @Test
    @DisplayName("기준일 USD/KRW 환율로 평가하고 소수점 넷째 자리에서 HALF_UP 반올림한다")
    void shouldConvertUsdWithBaseDateRate() {
        OdsAccountLedger ledger = createBaseLedger("USD001", 0, "USD", new BigDecimal("1.0000"));
        when(exchangeRateRepository.findByBaseDateAndBaseCurrencyAndQuoteCurrency(
                LocalDate.of(2026, 4, 30), CurrencyCode.USD, CurrencyCode.KRW))
                .thenReturn(Optional.of(ExchangeRate.builder().baseRate(new BigDecimal("1350.12345")).build()));

        AllowanceInputPosition result = processor.process(ledger, LocalDate.of(2026, 4, 30));

        assertEquals(new BigDecimal("1350.1235"), result.getMarketValue());
        assertEquals(new BigDecimal("1.0000"), result.getOutstandingAmount());
    }

    @Test
    @DisplayName("기준일 USD/KRW 환율이 없으면 외화 원금을 원화 평가액으로 사용하지 않는다")
    void shouldFailWhenUsdRateIsMissing() {
        OdsAccountLedger ledger = createBaseLedger("USD002", 0, "USD", new BigDecimal("10000.0000"));
        when(exchangeRateRepository.findByBaseDateAndBaseCurrencyAndQuoteCurrency(
                LocalDate.of(2026, 4, 30), CurrencyCode.USD, CurrencyCode.KRW))
                .thenReturn(Optional.empty());

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> processor.process(ledger, LocalDate.of(2026, 4, 30)));

        assertTrue(failure.getMessage().contains("2026-04-30 USD/KRW"));
    }

    @Test
    @DisplayName("USD/KRW 환율이 null, 0, 음수이면 원화 평가액 생성을 실패시킨다")
    void shouldFailWhenUsdRateIsInvalid() {
        OdsAccountLedger ledger = createBaseLedger("USD003", 0, "USD", new BigDecimal("10000.0000"));
        for (BigDecimal invalidRate : Arrays.asList(null, BigDecimal.ZERO, new BigDecimal("-1"))) {
            when(exchangeRateRepository.findByBaseDateAndBaseCurrencyAndQuoteCurrency(
                    LocalDate.of(2026, 4, 30), CurrencyCode.USD, CurrencyCode.KRW))
                    .thenReturn(Optional.of(ExchangeRate.builder().baseRate(invalidRate).build()));

            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> processor.process(ledger, LocalDate.of(2026, 4, 30)));
            assertTrue(failure.getMessage().contains("2026-04-30 USD/KRW"));
        }
    }

    @Test
    @DisplayName("외화 평가 기준일이 없으면 최신 환율이나 1:1 값으로 대체하지 않는다")
    void shouldFailWhenUsdBaseDateIsMissing() {
        OdsAccountLedger ledger = createBaseLedger("USD004", 0, "USD", new BigDecimal("10000.0000"));

        assertThrows(IllegalStateException.class, () -> processor.process(ledger, null));
    }

    private OdsAccountLedger createBaseLedger(String accNo, Integer dpd) {
        return createBaseLedger(accNo, dpd, "KRW", new BigDecimal("1000000"));
    }

    private OdsAccountLedger createBaseLedger(String accNo, Integer dpd, String currency, BigDecimal outstandingAmount) {
        return OdsAccountLedger.builder()
                .accountNo(accNo)
                .customerCode("CUST001")
                .productCode("PROD001")
                .currency(currency)
                .outstandingAmount(outstandingAmount)
                .limitAmount(new BigDecimal("2000000"))
                .delinquentDays(dpd)
                .isActive(true)
                .build();
    }
}
