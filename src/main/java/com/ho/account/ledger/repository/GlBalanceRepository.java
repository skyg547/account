package com.ho.account.ledger.repository;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.Currency;
import com.ho.account.basic.domain.Department;
import com.ho.account.ledger.domain.GlBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface GlBalanceRepository extends JpaRepository<GlBalance, Long> {
    Optional<GlBalance> findByAccountAndFiscalYearAndFiscalPeriodAndDepartmentAndCurrency(
            AccountSubject account, String fiscalYear, String fiscalPeriod, Department department, Currency currency);

    @Query("SELECT COALESCE(SUM(b.beginBalanceDr - b.beginBalanceCr), 0) FROM GlBalance b " +
            "WHERE b.account = :account AND b.fiscalYear = :year AND b.fiscalPeriod = :period")
    BigDecimal sumNetBeginBalance(@Param("account") AccountSubject account,
            @Param("year") String year,
            @Param("period") String period);

    @Query("SELECT b FROM GlBalance b " +
            "JOIN FETCH b.account " +
            "WHERE b.fiscalYear = :year AND b.fiscalPeriod = :period")
    List<GlBalance> findForTrialBalance(@Param("year") String year, @Param("period") String period);
}
