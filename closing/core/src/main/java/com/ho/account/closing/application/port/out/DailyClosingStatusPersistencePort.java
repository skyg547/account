package com.ho.account.closing.application.port.out;

import com.ho.account.closing.domain.DailyClosingStatus;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyClosingStatusPersistencePort {

    Optional<DailyClosingStatus> findByBusinessDate(LocalDate businessDate);

    Optional<DailyClosingStatus> findByBusinessDateForUpdate(LocalDate businessDate);

    Optional<DailyClosingStatus> findLatestForUpdate();

    Optional<DailyClosingStatus> findPreviousForUpdate(LocalDate businessDate);

    DailyClosingStatus save(DailyClosingStatus status);
}
