package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.application.port.out.PeriodLockPersistencePort;
import com.ho.account.closing.domain.PeriodLock;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaPeriodLockPersistenceAdapter implements PeriodLockPersistencePort {
    private final PeriodLockRepository repository;

    @Override
    public PeriodLock save(PeriodLock value) {
        return ClosingEntityMapper.toDomain(
                repository.saveAndFlush(ClosingEntityMapper.toEntity(value)));
    }

    @Override
    public Optional<PeriodLock> findByFiscalPeriodId(Long id) {
        return repository.findByFiscalPeriodId(id).map(ClosingEntityMapper::toDomain);
    }

    @Override
    public void delete(PeriodLock value) {
        repository.deleteById(value.getId());
    }
}
