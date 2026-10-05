package com.ho.account.mart.core.domain.mart.processor;

import com.ho.account.mart.core.domain.mart.AllowanceInputPosition;
import com.ho.account.mart.core.domain.marketdata.ExchangeRate;
import com.ho.account.shared.finance.enums.CrStaging;
import com.ho.account.shared.finance.enums.CurrencyCode;
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
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * [QA] IntegratedPositionProcessor 단위 테스트
 * ETL 변환 로직 및 IFRS 9 스테이징 판정 기준을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class IntegratedPositionProcessorTest {

    private static final LocalDate BASE_DATE = LocalDate.of(2026, 4, 30);

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
                anyString(), eq(LocalDate.of(2026, 4, 30)))).thenReturn(Optional.empty());
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
    @DisplayName("기준일 USD/KRW 환율로 외화 잔액을 반올림하여 원화 평가액을 만든다")
    void shouldConvertForeignBalanceWithBaseDateRate() {
        OdsAccountLedger ledger = createUsdLedger();
        when(exchangeRateRepository.findByBaseDateAndBaseCurrencyAndQuoteCurrency(
                BASE_DATE, CurrencyCode.USD, CurrencyCode.KRW))
                .thenReturn(Optional.of(usdKrwRate(new BigDecimal("1350.0000"))));

        AllowanceInputPosition result = processor.process(ledger, BASE_DATE);

        assertEquals(new BigDecimal("13500000.0000"), result.getMarketValue());
        verify(exchangeRateRepository).findByBaseDateAndBaseCurrencyAndQuoteCurrency(
                BASE_DATE, CurrencyCode.USD, CurrencyCode.KRW);
    }

    @Test
    @DisplayName("원화 환산은 기존 소수점 네 자리 HALF_UP 정책을 따른다")
    void shouldRoundConvertedAmountToFourPlaces() {
        OdsAccountLedger ledger = createUsdLedger(new BigDecimal("0.0001"));
        when(exchangeRateRepository.findByBaseDateAndBaseCurrencyAndQuoteCurrency(
                BASE_DATE, CurrencyCode.USD, CurrencyCode.KRW))
                .thenReturn(Optional.of(usdKrwRate(new BigDecimal("1.5555"))));

        AllowanceInputPosition result = processor.process(ledger, BASE_DATE);

        assertEquals(new BigDecimal("0.0002"), result.getMarketValue());
    }

    @Test
    @DisplayName("기준일 외화 환율이 없으면 원금을 평가액으로 사용하지 않고 실패한다")
    void shouldFailWhenForeignRateIsMissing() {
        OdsAccountLedger ledger = createUsdLedger();
        when(exchangeRateRepository.findByBaseDateAndBaseCurrencyAndQuoteCurrency(
                BASE_DATE, CurrencyCode.USD, CurrencyCode.KRW)).thenReturn(Optional.empty());

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> processor.process(ledger, BASE_DATE));

        assertTrue(failure.getMessage().contains("2026-04-30 USD/KRW"));
    }

    @Test
    @DisplayName("지원하지 않는 외화 통화 코드는 배치 skip 대상 예외가 아닌 오류로 실패한다")
    void shouldFailWhenForeignCurrencyIsUnsupported() {
        OdsAccountLedger ledger = createBaseLedger("ACC-UNKNOWN", 0, "XYZ", new BigDecimal("10000.0000"));

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> processor.process(ledger, BASE_DATE));

        assertTrue(failure.getMessage().contains("XYZ"));
    }

    @Test
    @DisplayName("0 이하 또는 null 환율은 유효하지 않아 실패한다")
    void shouldFailWhenForeignRateIsInvalid() {
        for (BigDecimal invalidRate : new BigDecimal[]{BigDecimal.ZERO, new BigDecimal("-1"), null}) {
            OdsAccountLedger ledger = createUsdLedger();
            when(exchangeRateRepository.findByBaseDateAndBaseCurrencyAndQuoteCurrency(
                    BASE_DATE, CurrencyCode.USD, CurrencyCode.KRW))
                    .thenReturn(Optional.of(usdKrwRate(invalidRate)));

            assertThrows(IllegalStateException.class, () -> processor.process(ledger, BASE_DATE));
        }
    }

    @Test
    @DisplayName("조회 결과가 다른 기준일이면 그 환율로 평가하지 않는다")
    void shouldFailWhenReturnedRateHasDifferentBaseDate() {
        OdsAccountLedger ledger = createUsdLedger();
        ExchangeRate staleRate = ExchangeRate.builder()
                .baseDate(BASE_DATE.minusDays(1))
                .baseCurrency(CurrencyCode.USD)
                .quoteCurrency(CurrencyCode.KRW)
                .baseRate(new BigDecimal("1350.0000"))
                .build();
        when(exchangeRateRepository.findByBaseDateAndBaseCurrencyAndQuoteCurrency(
                BASE_DATE, CurrencyCode.USD, CurrencyCode.KRW)).thenReturn(Optional.of(staleRate));

        assertThrows(IllegalStateException.class, () -> processor.process(ledger, BASE_DATE));
    }

    private OdsAccountLedger createUsdLedger() {
        return createUsdLedger(new BigDecimal("10000.0000"));
    }

    private OdsAccountLedger createUsdLedger(BigDecimal amount) {
        return createBaseLedger("ACC-USD", 0, "USD", amount);
    }

    private ExchangeRate usdKrwRate(BigDecimal rate) {
        return ExchangeRate.builder()
                .baseDate(BASE_DATE)
                .baseCurrency(CurrencyCode.USD)
                .quoteCurrency(CurrencyCode.KRW)
                .baseRate(rate)
                .build();
    }

    private OdsAccountLedger createBaseLedger(String accNo, Integer dpd) {
        return createBaseLedger(accNo, dpd, "KRW", new BigDecimal("1000000"));
    }

    private OdsAccountLedger createBaseLedger(String accNo, Integer dpd, String currency, BigDecimal amount) {
        return OdsAccountLedger.builder()
                .accountNo(accNo)
                .customerCode("CUST001")
                .productCode("PROD001")
                .currency(currency)
                .outstandingAmount(amount)
                .limitAmount(new BigDecimal("2000000"))
                .delinquentDays(dpd)
                .isActive(true)
                .build();
    }
}
