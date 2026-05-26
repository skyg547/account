package com.risk.mart.core.infrastructure.persistence;

import com.risk.mart.core.application.port.out.MarketRateRepository;
import com.risk.mart.core.domain.marketdata.MarketRate;
import com.risk.mart.core.infrastructure.persistence.entity.marketdata.MarketRateEntity;
import com.risk.mart.core.infrastructure.persistence.jpa.JpaMarketRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MarketRatePersistenceAdapter implements MarketRateRepository {

    private final JpaMarketRateRepository jpaRepository;

    @Override
    public List<MarketRate> findByBaseDateAndIsActiveTrue(LocalDate baseDate) {
        return jpaRepository.findByBaseDateAndIsActiveTrue(baseDate).stream()
                .map(e -> toDomain(e))
                .collect(Collectors.toList());
    }

    @Override
    public List<MarketRate> findByBaseDateAndRateTypeAndIsActiveTrue(LocalDate baseDate, String rateType) {
        return jpaRepository.findByBaseDateAndRateTypeAndIsActiveTrue(baseDate, rateType).stream()
                .map(e -> toDomain(e))
                .collect(Collectors.toList());
    }

    @Override
    public List<MarketRate> findByRateNameAndIsActiveTrue(String rateName) {
        return jpaRepository.findByRateNameAndIsActiveTrue(rateName).stream()
                .map(e -> toDomain(e))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<MarketRate> findByBaseDateAndRateNameAndTenorMonths(LocalDate baseDate, String rateName, Integer tenorMonths) {
        return jpaRepository.findByBaseDateAndRateNameAndTenorMonths(baseDate, rateName, tenorMonths)
                .map(e -> toDomain(e));
    }

    @Override
    public List<MarketRate> findHistorical(String rateName, Integer tenorMonths, LocalDate startDate, LocalDate endDate) {
        return jpaRepository.findHistorical(rateName, tenorMonths, startDate, endDate).stream()
                .map(e -> toDomain(e))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> findDistinctActiveRateNames() {
        return jpaRepository.findDistinctActiveRateNames();
    }

    @Override
    public MarketRate save(MarketRate domain) {
        return toDomain(jpaRepository.save(toEntity(domain)));
    }

    @Override
    public List<MarketRate> saveAll(List<MarketRate> domains) {
        if (domains == null) return List.of();
        List<MarketRateEntity> entities = domains.stream()
                .map(d -> toEntity(d))
                .collect(Collectors.toList());
        return jpaRepository.saveAll(entities).stream()
                .map(e -> toDomain(e))
                .collect(Collectors.toList());
    }

    private MarketRate toDomain(MarketRateEntity entity) {
        if (entity == null) return null;
        return MarketRate.builder()
                .rateCode(entity.getId() != null ? String.valueOf(entity.getId()) : null) // id를 rateCode로 매핑
                .baseDate(entity.getBaseDate())
                .rateName(entity.getRateName())
                .rateType(entity.getRateType())
                .currency(entity.getCurrency())
                .tenorMonths(entity.getTenorMonths())
                .tenorLabel(entity.getTenorLabel())
                .rate(entity.getRate())
                .build();
    }

    private MarketRateEntity toEntity(MarketRate domain) {
        if (domain == null) return null;
        return MarketRateEntity.builder()
                .id(domain.getRateCode() != null ? Long.valueOf(domain.getRateCode()) : null)
                .baseDate(domain.getBaseDate())
                .rateName(domain.getRateName())
                .rateType(domain.getRateType())
                .currency(domain.getCurrency())
                .tenorMonths(domain.getTenorMonths())
                .tenorLabel(domain.getTenorLabel())
                .rate(domain.getRate())
                .build();
    }
}
