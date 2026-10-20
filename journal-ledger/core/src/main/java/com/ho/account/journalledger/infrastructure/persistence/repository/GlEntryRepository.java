package com.ho.account.journalledger.infrastructure.persistence.repository;

import com.ho.account.journalledger.domain.ledger.domain.GlEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface GlEntryRepository extends JpaRepository<GlEntry, Long> {

    List<GlEntry> findByPostingDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT g FROM GlEntry g WHERE g.accountCode = :accountCode AND g.postingDate BETWEEN :startDate AND :endDate")
    List<GlEntry> findByAccountAndDateBetween(@Param("accountCode") String accountCode,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(g.baseDrAmount - g.baseCrAmount) FROM GlEntry g WHERE g.accountCode = :accountCode AND g.postingDate <= :asOfDate")
    BigDecimal sumNetAmount(@Param("accountCode") String accountCode,
                            @Param("asOfDate") LocalDate asOfDate);
}