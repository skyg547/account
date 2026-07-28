package com.ho.account.masterdata.core.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.ExchangeRate;
import com.ho.account.masterdata.core.domain.model.Product;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(properties = {
        "spring.profiles.active=local",
        "spring.cloud.config.enabled=false",
        "spring.cloud.vault.enabled=false",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ActiveMasterDataRepositoryTest {

    @Autowired
    private AccountSubjectRepository accountSubjectRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Test
    void activeListsAreFilteredInDatabaseByTheRequestedDate() {
        LocalDate today = LocalDate.now();
        accountSubjectRepository.save(account("110000", "Expired cash",
                today.minusYears(1), today.minusDays(1)));
        accountSubjectRepository.save(account("110000", "Current cash",
                today, LocalDate.of(9999, 12, 31)));
        productRepository.save(product("LOAN-01", "Expired loan",
                today.minusYears(1), today.minusDays(1)));
        productRepository.save(product("LOAN-01", "Current loan",
                today, LocalDate.of(9999, 12, 31)));

        assertThat(accountSubjectRepository.findActiveVersions(today))
                .extracting(AccountSubject::getName)
                .containsExactly("Current cash");
        assertThat(productRepository.findActiveVersions(today))
                .extracting(Product::getName)
                .containsExactly("Current loan");
    }

    @Test
    void exchangeRateLookupSelectsLatestRateNotAfterRequestedDate() {
        exchangeRateRepository.save(exchangeRate(
                "USD", "KRW", "1300.00000000", LocalDate.of(2026, 1, 1)));
        exchangeRateRepository.save(exchangeRate(
                "USD", "KRW", "1400.00000000", LocalDate.of(2026, 7, 1)));

        assertThat(exchangeRateRepository.findExchangeRate(
                "USD", "KRW", LocalDate.of(2026, 6, 30)))
                .get()
                .extracting(ExchangeRate::getRate)
                .isEqualTo(new BigDecimal("1300.00000000"));
        assertThat(exchangeRateRepository.findExchangeRate(
                "USD", "KRW", LocalDate.of(2026, 7, 1)))
                .get()
                .extracting(ExchangeRate::getRate)
                .isEqualTo(new BigDecimal("1400.00000000"));
        assertThat(exchangeRateRepository.findExchangeRate(
                "USD", "KRW", LocalDate.of(2025, 12, 31)))
                .isEmpty();
    }

    private AccountSubject account(String code, String name, LocalDate validFrom, LocalDate validTo) {
        AccountSubject account = new AccountSubject();
        account.setCode(code);
        account.setName(name);
        account.setCategory(AccountSubject.AccountCategory.ASSETS);
        account.setBalanceType(AccountSubject.BalanceType.DEBIT);
        account.setValidFrom(validFrom);
        account.setValidTo(validTo);
        return account;
    }

    private Product product(String code, String name, LocalDate validFrom, LocalDate validTo) {
        Product product = new Product();
        product.setProductCode(code);
        product.setName(name);
        product.setPrice(BigDecimal.ONE);
        product.setProductType(Product.ProductType.LOAN);
        product.setValidFrom(validFrom);
        product.setValidTo(validTo);
        return product;
    }

    private ExchangeRate exchangeRate(
            String fromCurrency,
            String toCurrency,
            String rate,
            LocalDate effectiveDate) {
        ExchangeRate exchangeRate = new ExchangeRate();
        exchangeRate.setFromCurrencyCode(fromCurrency);
        exchangeRate.setToCurrencyCode(toCurrency);
        exchangeRate.setRate(new BigDecimal(rate));
        exchangeRate.setEffectiveDate(effectiveDate);
        return exchangeRate;
    }
}
