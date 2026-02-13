package com.ho.account.ledger.repository;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.ledger.domain.GlEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface GlEntryRepository extends JpaRepository<GlEntry, Long> {
    List<GlEntry> findByAccountAndPostingDateBetweenOrderByPostingDateAscIdAsc(
            AccountSubject account, LocalDate startDate, LocalDate endDate);

    @Query("SELECT COALESCE(SUM(e.drAmount - e.crAmount), 0) FROM GlEntry e " +
            "WHERE e.account = :account AND e.postingDate BETWEEN :startDate AND :endDate")
    BigDecimal sumNetAmount(@Param("account") AccountSubject account,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
