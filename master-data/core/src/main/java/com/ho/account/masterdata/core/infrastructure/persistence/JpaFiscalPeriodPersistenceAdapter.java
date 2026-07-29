package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.application.port.out.FiscalPeriodPersistencePort;
import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.FiscalPeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaFiscalPeriodPersistenceAdapter implements FiscalPeriodPersistencePort {

    private final FiscalPeriodRepository fiscalPeriodRepository;

    @Override
    public Optional<FiscalPeriod> findById(Long id) {
        return fiscalPeriodRepository.findById(id);
    }

    @Override
    public Optional<FiscalPeriod> findByIdForUpdate(Long id) {
        return fiscalPeriodRepository.findByIdForUpdate(id);
    }

    @Override
    public Optional<FiscalPeriod> findByFiscalYearAndFiscalPeriod(String fiscalYear, String fiscalPeriod) {
        return fiscalPeriodRepository.findByFiscalYearAndFiscalPeriod(fiscalYear, fiscalPeriod);
    }

    @Override
    public FiscalPeriod save(FiscalPeriod fiscalPeriod) {
        return fiscalPeriodRepository.save(fiscalPeriod);
    }
}
