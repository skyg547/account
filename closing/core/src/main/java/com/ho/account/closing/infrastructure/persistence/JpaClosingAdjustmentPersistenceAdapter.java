package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.application.port.out.ClosingAdjustmentPersistencePort;
import com.ho.account.closing.domain.ClosingAdjustment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaClosingAdjustmentPersistenceAdapter implements ClosingAdjustmentPersistencePort {
    private final ClosingAdjustmentRepository repository;

    @Override
    public ClosingAdjustment save(ClosingAdjustment value) {
        return ClosingEntityMapper.toDomain(
                repository.saveAndFlush(ClosingEntityMapper.toEntity(value)));
    }
}
