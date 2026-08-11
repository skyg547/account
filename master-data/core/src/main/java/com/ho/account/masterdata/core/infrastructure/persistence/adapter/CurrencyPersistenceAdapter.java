package com.ho.account.masterdata.core.infrastructure.persistence.adapter;

import com.ho.account.masterdata.core.application.port.out.CurrencyPersistencePort;
import com.ho.account.masterdata.core.domain.model.Currency;
import com.ho.account.masterdata.core.infrastructure.persistence.entity.CurrencyEntity;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.CurrencyMapper;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CurrencyPersistenceAdapter implements CurrencyPersistencePort {

    private final CurrencyRepository currencyRepository;
    private final CurrencyMapper currencyMapper;

    @Override
    public Currency save(Currency currency) {
        CurrencyEntity entity = currencyMapper.toEntity(currency);
        CurrencyEntity saved = currencyRepository.save(entity);
        return currencyMapper.toDomain(saved);
    }

    @Override
    public Optional<Currency> findByCode(String code) {
        return currencyRepository.findByCurrencyCode(code)
                .map(currencyMapper::toDomain);
    }

    @Override
    public Optional<Currency> findByCodeAt(String code, LocalDate asOfDate) {
        return currencyRepository.findActiveByCurrencyCode(code, asOfDate)
                .map(currencyMapper::toDomain);
    }
}

