package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.application.port.out.DailyClosingStatusPersistencePort;
import com.ho.account.closing.domain.DailyClosingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class JpaDailyClosingStatusPersistenceAdapter implements DailyClosingStatusPersistencePort {

    private final DailyClosingStatusRepository repository;

    @Override
    public Optional<DailyClosingStatus> findByBusinessDate(LocalDate businessDate) {
        return repository.findById(businessDate).map(ClosingEntityMapper::toDomain);
    }

    @Override
    public Optional<DailyClosingStatus> findByBusinessDateForUpdate(LocalDate businessDate) {
        return withConflictTranslation(
                () -> repository.findByBusinessDateForUpdate(businessDate).map(ClosingEntityMapper::toDomain),
                businessDate);
    }

    @Override
    public Optional<DailyClosingStatus> findLatestForUpdate() {
        return withConflictTranslation(() -> repository.findLatestForUpdate().map(ClosingEntityMapper::toDomain), null);
    }

    @Override
    public Optional<DailyClosingStatus> findPreviousForUpdate(LocalDate businessDate) {
        return withConflictTranslation(
                () -> repository.findPreviousForUpdate(businessDate).map(ClosingEntityMapper::toDomain),
                businessDate);
    }

    @Override
    public DailyClosingStatus save(DailyClosingStatus status) {
        return withConflictTranslation(
                () -> saveStatus(status),
                status.getBusinessDate());
    }

    private DailyClosingStatus saveStatus(DailyClosingStatus status) {
        DailyClosingStatusEntity entity = repository.findById(status.getBusinessDate())
                .orElseGet(() -> ClosingEntityMapper.toEntity(status));
        if (entity.getVersion() != null && !entity.getVersion().equals(status.getVersion())) {
            throw new IllegalStateException("Daily closing status version changed for " + status.getBusinessDate());
        }
        // Keep the managed @Version value and row lock; copy only the transition fields.
        if (entity.getVersion() != null) {
            entity.setState(status.getState());
            entity.setUpdatedAt(status.getUpdatedAt());
            entity.setUpdatedBy(status.getUpdatedBy());
            entity.setPreparedAt(status.getPreparedAt());
            entity.setPreparedBy(status.getPreparedBy());
            entity.setClosingStartedAt(status.getClosingStartedAt());
            entity.setClosingStartedBy(status.getClosingStartedBy());
            entity.setClosedAt(status.getClosedAt());
            entity.setClosedBy(status.getClosedBy());
            entity.setBodStartedAt(status.getBodStartedAt());
            entity.setBodStartedBy(status.getBodStartedBy());
            entity.setOpenedAt(status.getOpenedAt());
            entity.setOpenedBy(status.getOpenedBy());
        }
        return ClosingEntityMapper.toDomain(repository.saveAndFlush(entity));
    }

    private <T> T withConflictTranslation(
            Supplier<T> operation,
            LocalDate businessDate) {
        try {
            return operation.get();
        } catch (ConcurrencyFailureException | DataIntegrityViolationException ex) {
            String suffix = businessDate == null ? "" : " for business date " + businessDate;
            throw new IllegalStateException("Daily closing status persistence conflict" + suffix, ex);
        }
    }
}
