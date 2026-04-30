package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.domain.DailyClosingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyClosingStatusRepository extends JpaRepository<DailyClosingStatus, LocalDate> {
    Optional<DailyClosingStatus> findByDate(LocalDate date);
}
