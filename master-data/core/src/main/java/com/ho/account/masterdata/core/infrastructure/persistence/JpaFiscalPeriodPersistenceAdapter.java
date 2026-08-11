package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.application.port.out.FiscalPeriodPersistencePort;
import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import com.ho.account.masterdata.core.infrastructure.persistence.entity.FiscalPeriodEntity;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.FiscalPeriodMapper;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.FiscalPeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaFiscalPeriodPersistenceAdapter implements FiscalPeriodPersistencePort {

    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final FiscalPeriodMapper fiscalPeriodMapper;

    @Override
    public Optional<FiscalPeriod> findById(Long id) {
        return fiscalPeriodRepository.findById(id).map(fiscalPeriodMapper::toDomain);
    }

    @Override
    public Optional<FiscalPeriod> findByIdForUpdate(Long id) {
        return fiscalPeriodRepository.findByIdForUpdate(id).map(fiscalPeriodMapper::toDomain);
    }

    @Override
    public Optional<FiscalPeriod> findByFiscalYearAndFiscalPeriod(String fiscalYear, String fiscalPeriod) {
        return fiscalPeriodRepository.findByFiscalYearAndFiscalPeriod(fiscalYear, fiscalPeriod).map(fiscalPeriodMapper::toDomain);
    }

    @Override
    public FiscalPeriod save(FiscalPeriod fiscalPeriod) {
        FiscalPeriodEntity entity = fiscalPeriodMapper.toEntity(fiscalPeriod);
        FiscalPeriodEntity saved = fiscalPeriodRepository.save(entity);
        return fiscalPeriodMapper.toDomain(saved);
    }
}

