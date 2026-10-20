package com.ho.account.closing.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyClosingStatusRepository extends JpaRepository<DailyClosingStatusEntity, LocalDate> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select status from DailyClosingStatus status where status.businessDate = :businessDate")
    Optional<DailyClosingStatusEntity> findByBusinessDateForUpdate(
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
    Optional<DailyClosingStatusEntity> findLatestForUpdate();

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
    Optional<DailyClosingStatusEntity> findPreviousForUpdate(
            @Param("businessDate") LocalDate businessDate);
}
