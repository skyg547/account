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
        return repository.findById(businessDate);
    }

    @Override
    public Optional<DailyClosingStatus> findByBusinessDateForUpdate(LocalDate businessDate) {
        return withConflictTranslation(
                () -> repository.findByBusinessDateForUpdate(businessDate),
                businessDate);
    }

    @Override
    public Optional<DailyClosingStatus> findLatestForUpdate() {
        return withConflictTranslation(repository::findLatestForUpdate, null);
    }

    @Override
    public Optional<DailyClosingStatus> findPreviousForUpdate(LocalDate businessDate) {
        return withConflictTranslation(
                () -> repository.findPreviousForUpdate(businessDate),
                businessDate);
    }

    @Override
    public DailyClosingStatus save(DailyClosingStatus status) {
        return withConflictTranslation(
                () -> repository.saveAndFlush(status),
                status.getBusinessDate());
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
