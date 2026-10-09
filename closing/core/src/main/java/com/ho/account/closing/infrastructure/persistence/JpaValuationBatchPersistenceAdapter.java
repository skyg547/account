package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.application.port.out.ValuationBatchPersistencePort;
import com.ho.account.closing.domain.ValuationBatch;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaValuationBatchPersistenceAdapter implements ValuationBatchPersistencePort {
    private final ValuationBatchRepository repository;

    @Override
    public ValuationBatch save(ValuationBatch value) {
        return ClosingEntityMapper.toDomain(
                repository.saveAndFlush(ClosingEntityMapper.toEntity(value)));
    }

    @Override
    public Optional<ValuationBatch> findById(Long id) {
        return repository.findById(id).map(ClosingEntityMapper::toDomain);
    }
}
