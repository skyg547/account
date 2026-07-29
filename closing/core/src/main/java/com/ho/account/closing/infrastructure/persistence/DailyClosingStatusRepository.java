package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.domain.DailyClosingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyClosingStatusRepository extends JpaRepository<DailyClosingStatus, LocalDate> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select status from DailyClosingStatus status where status.businessDate = :businessDate")
    Optional<DailyClosingStatus> findByBusinessDateForUpdate(
            @Param("businessDate") LocalDate businessDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select status
            from DailyClosingStatus status
            where status.businessDate = (
                select max(candidate.businessDate)
                from DailyClosingStatus candidate
            )
            """)
    Optional<DailyClosingStatus> findLatestForUpdate();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select status
            from DailyClosingStatus status
            where status.businessDate = (
                select max(candidate.businessDate)
                from DailyClosingStatus candidate
                where candidate.businessDate < :businessDate
            )
            """)
    Optional<DailyClosingStatus> findPreviousForUpdate(
            @Param("businessDate") LocalDate businessDate);
}
