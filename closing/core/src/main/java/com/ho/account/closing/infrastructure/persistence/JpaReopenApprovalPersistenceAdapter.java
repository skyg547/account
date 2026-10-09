package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.application.port.out.ReopenApprovalPersistencePort;
import com.ho.account.closing.domain.ReopenApproval;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaReopenApprovalPersistenceAdapter implements ReopenApprovalPersistencePort {
    private final ReopenApprovalRepository repository;

    @Override
    public ReopenApproval save(ReopenApproval value) {
        return ClosingEntityMapper.toDomain(
                repository.saveAndFlush(ClosingEntityMapper.toEntity(value)));
    }

    @Override
    public Optional<ReopenApproval> findById(Long id) {
        return repository.findById(id).map(ClosingEntityMapper::toDomain);
    }

    @Override
    public boolean existsByFiscalPeriodIdAndStatus(Long id, ReopenApproval.ReopenApprovalStatus status) {
        return repository.existsByFiscalPeriodIdAndStatus(id, status);
    }
}
