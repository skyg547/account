package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.application.port.out.ProvisionBatchPersistencePort;
import com.ho.account.closing.domain.ProvisionBatch;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaProvisionBatchPersistenceAdapter implements ProvisionBatchPersistencePort {
    private final ProvisionBatchRepository repository;

    @Override
    public ProvisionBatch save(ProvisionBatch value) {
        return ClosingEntityMapper.toDomain(
                repository.saveAndFlush(ClosingEntityMapper.toEntity(value)));
    }

    @Override
    public Optional<ProvisionBatch> findById(Long id) {
        return repository.findById(id).map(ClosingEntityMapper::toDomain);
    }
}
