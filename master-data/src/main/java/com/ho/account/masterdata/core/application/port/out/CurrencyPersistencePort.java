package com.ho.account.masterdata.core.application.port.out;

import com.ho.account.masterdata.core.domain.model.Currency;
import java.util.Optional;

/**
 * 통화 정보 영속성 포트
 */
public interface CurrencyPersistencePort {
    Optional<Currency> findByCode(String code);
}
