package com.ho.account.masterdata.core.infrastructure.persistence.adapter;

import com.ho.account.masterdata.core.application.port.out.CurrencyPersistencePort;
import com.ho.account.masterdata.core.domain.model.Currency;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CurrencyPersistenceAdapter implements CurrencyPersistencePort {

    private final CurrencyRepository currencyRepository;

    @Override
    public Optional<Currency> findByCode(String code) {
        return currencyRepository.findById(code);
    }
}
