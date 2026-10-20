package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.application.port.out.ClosingCalendarPersistencePort;
import com.ho.account.closing.domain.ClosingCalendar;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaClosingCalendarPersistenceAdapter implements ClosingCalendarPersistencePort {
    private final ClosingCalendarRepository repository;

    @Override
    public ClosingCalendar save(ClosingCalendar value) {
        return ClosingEntityMapper.toDomain(
                repository.saveAndFlush(ClosingEntityMapper.toEntity(value)));
    }

    @Override
    public Optional<ClosingCalendar> findById(Long id) {
        return repository.findById(id).map(ClosingEntityMapper::toDomain);
    }

    @Override
    public Optional<ClosingCalendar> findByFiscalYearAndFiscalPeriod(String year, String period) {
        return repository.findByFiscalYearAndFiscalPeriod(year, period).map(ClosingEntityMapper::toDomain);
    }
}
