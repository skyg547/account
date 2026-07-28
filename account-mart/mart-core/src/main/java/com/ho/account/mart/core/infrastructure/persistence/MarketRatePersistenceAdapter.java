package com.ho.account.mart.core.infrastructure.persistence;

import com.ho.account.mart.core.application.port.out.MarketRateRepository;
import com.ho.account.mart.core.domain.marketdata.MarketRate;
import com.ho.account.mart.core.infrastructure.persistence.entity.marketdata.MarketRateEntity;
import com.ho.account.mart.core.infrastructure.persistence.jpa.JpaMarketRateRepository;
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
                .map(e -> main(e))
                .collect(Collectors.toList());
    }

    @Override
    public List<MarketRate> findByBaseDateAndRateTypeAndIsActiveTrue(LocalDate baseDate, String rateType) {
        return jpaRepository.findByBaseDateAndRateTypeAndIsActiveTrue(baseDate, rateType).stream()
                .map(e -> main(e))
                .collect(Collectors.toList());
    }

    @Override
    public List<MarketRate> findByRateNameAndIsActiveTrue(String rateName) {
        return jpaRepository.findByRateNameAndIsActiveTrue(rateName).stream()
                .map(e -> main(e))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<MarketRate> findByBaseDateAndRateNameAndTenorMonths(LocalDate baseDate, String rateName, Integer tenorMonths) {
        return jpaRepository.findByBaseDateAndRateNameAndTenorMonths(baseDate, rateName, tenorMonths)
                .map(e -> main(e));
    }

    @Override
    public List<MarketRate> findHistorical(String rateName, Integer tenorMonths, LocalDate startDate, LocalDate endDate) {
        return jpaRepository.findHistorical(rateName, tenorMonths, startDate, endDate).stream()
                .map(e -> main(e))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> findDistinctActiveRateNames() {
        return jpaRepository.findDistinctActiveRateNames();
    }

    @Override
    public MarketRate save(MarketRate domain) {
        return main(jpaRepository.save(toEntity(domain)));
    }

    @Override
    public List<MarketRate> saveAll(List<MarketRate> domains) {
        if (domains == null) return List.of();
        List<MarketRateEntity> entities = domains.stream()
                .map(d -> toEntity(d))
                .collect(Collectors.toList());
        return jpaRepository.saveAll(entities).stream()
                .map(e -> main(e))
                .collect(Collectors.toList());
    }

    private MarketRate main(MarketRateEntity entity) {
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
