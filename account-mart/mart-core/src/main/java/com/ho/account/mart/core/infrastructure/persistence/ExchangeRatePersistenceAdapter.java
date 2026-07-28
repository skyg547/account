package com.ho.account.mart.core.infrastructure.persistence;

import com.ho.account.shared.finance.enums.CurrencyCode;
import com.ho.account.mart.core.application.port.out.ExchangeRateRepository;
import com.ho.account.mart.core.domain.marketdata.ExchangeRate;
import com.ho.account.mart.core.infrastructure.persistence.entity.marketdata.ExchangeRateEntity;
import com.ho.account.mart.core.infrastructure.persistence.jpa.JpaExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ExchangeRatePersistenceAdapter implements ExchangeRateRepository {

    private final JpaExchangeRateRepository jpaRepository;

    @Override
    public Optional<ExchangeRate> findByBaseDateAndBaseCurrencyAndQuoteCurrency(
            LocalDate baseDate, CurrencyCode baseCurrency, CurrencyCode quoteCurrency) {
        return jpaRepository.findByBaseDateAndBaseCurrencyAndQuoteCurrency(baseDate, baseCurrency, quoteCurrency)
                .map(e -> toDomain(e));
    }

    @Override
    public List<ExchangeRate> findByBaseDate(LocalDate baseDate) {
        return jpaRepository.findByBaseDate(baseDate).stream()
                .map(e -> toDomain(e))
                .collect(Collectors.toList());
    }

    @Override
    public List<ExchangeRate> findByBaseDateAndQuoteCurrency(LocalDate baseDate, CurrencyCode quoteCurrency) {
        return jpaRepository.findByBaseDateAndQuoteCurrency(baseDate, quoteCurrency).stream()
                .map(e -> toDomain(e))
                .collect(Collectors.toList());
    }

    @Override
    public List<ExchangeRate> findHistorical(CurrencyCode baseCurrency, CurrencyCode quoteCurrency, LocalDate startDate, LocalDate endDate) {
        return jpaRepository.findByBaseCurrencyAndQuoteCurrencyAndBaseDateBetweenOrderByBaseDateAsc(baseCurrency, quoteCurrency, startDate, endDate)
                .stream()
                .map(e -> toDomain(e))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ExchangeRate> findLatest(CurrencyCode baseCurrency, CurrencyCode quoteCurrency) {
        return jpaRepository.findLatest(baseCurrency, quoteCurrency)
                .map(e -> toDomain(e));
    }

    @Override
    public ExchangeRate save(ExchangeRate domain) {
        return toDomain(jpaRepository.save(toEntity(domain)));
    }

    @Override
    public void saveAll(List<ExchangeRate> domains) {
        if (domains == null) return;
        List<ExchangeRateEntity> entities = domains.stream()
                .map(d -> toEntity(d))
                .collect(Collectors.toList());
        jpaRepository.saveAll(entities);
    }

    @Override
    public List<ExchangeRate> findAll() {
        return jpaRepository.findAll().stream()
                .map(e -> toDomain(e))
                .collect(Collectors.toList());
    }

    private ExchangeRate toDomain(ExchangeRateEntity entity) {
        if (entity == null) return null;
        return ExchangeRate.builder()
                .id(entity.getId())
                .baseDate(entity.getBaseDate())
                .baseCurrency(entity.getBaseCurrency())
                .quoteCurrency(entity.getQuoteCurrency())
                .baseRate(entity.getBaseRate())
                .build();
    }

    private ExchangeRateEntity toEntity(ExchangeRate domain) {
        if (domain == null) return null;
        return ExchangeRateEntity.builder()
                .id(domain.getId())
                .baseDate(domain.getBaseDate())
                .baseCurrency(domain.getBaseCurrency())
                .quoteCurrency(domain.getQuoteCurrency())
                .baseRate(domain.getBaseRate())
                .build();
    }
}
