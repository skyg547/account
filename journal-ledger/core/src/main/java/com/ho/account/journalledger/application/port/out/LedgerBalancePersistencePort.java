package com.ho.account.journalledger.application.port.out;

import com.ho.account.contracts.ledger.LedgerAggregateSummary;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.ledger.domain.GlBalance;
import com.ho.account.journalledger.domain.ledger.domain.SlBalance;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * GL/SL 잔액 조회·저장·재집계용 출력 포트입니다.
 *
 * <p>필터 조합과 bulk 삭제 같은 저장 기술 세부사항을 애플리케이션 서비스에서 숨깁니다.</p>
 */
public interface LedgerBalancePersistencePort {

    Optional<GlBalance> findGlBalance(
            String accountCode, String currencyCode, LocalDate balanceDate, YearMonth period);

    Optional<GlBalance> findPreviousGlBalance(
            String accountCode, String currencyCode, LocalDate balanceDate);

    GlBalance saveGlBalance(GlBalance balance);

    void saveGlBalances(List<GlBalance> balances);

    Optional<SlBalance> findSlBalance(
            String accountCode, String businessPartnerCode, String departmentCode,
            String currencyCode, LocalDate balanceDate, YearMonth period);

    Optional<SlBalance> findPreviousSlBalance(
            String accountCode, String businessPartnerCode, String departmentCode,
            String currencyCode, LocalDate balanceDate);

    SlBalance saveSlBalance(SlBalance balance);

    void saveSlBalances(List<SlBalance> balances);

    void deleteBalancesBetween(LocalDate startDate, LocalDate endDate);

    List<JournalDetail> findPostedJournalDetailsBetween(LocalDate startDate, LocalDate endDate);

    List<GlBalance> findGlBalances(
            LocalDate startDate, LocalDate endDate, String accountCode, String currencyCode);

    List<SlBalance> findSlBalances(
            LocalDate startDate, LocalDate endDate, String accountCode,
            String businessPartnerCode, String departmentCode, String currencyCode);

    LedgerAggregateSummary calculateGlBalanceAggregate(
            LocalDate startDate, LocalDate endDate, String accountCode, String currencyCode, String amountBasis);
}
