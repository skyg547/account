package com.ho.account.journalledger.application.port.out;

import com.ho.account.contracts.ledger.LedgerAggregateSummary;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.ledger.domain.GlBalance;
import com.ho.account.journalledger.domain.ledger.domain.SlBalance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * GL/SL 잔액 조회·저장·재집계용 출력 포트입니다.
 *
 * <p>필터 조합과 bulk 삭제 같은 저장 기술 세부사항을 애플리케이션 서비스에서 숨깁니다.</p>
 */
public interface LedgerBalancePersistencePort {

    /**
     * Lock every affected account before reading balances, including keys without a balance row.
     * Locks cover GL and all SL dimensions/dates until the caller's write transaction completes.
     */
    void lockBalanceAccounts(List<BalanceAccount> accounts);

    /** Lock the complete balance key space before deleting/rebuilding a period. */
    void lockAllBalanceAccounts();

    record BalanceAccount(String accountCode, String currencyCode) {
        public BalanceAccount {
            Objects.requireNonNull(accountCode, "accountCode");
            Objects.requireNonNull(currencyCode, "currencyCode");
        }
    }

    /** Signed debit-minus-credit movement for one GL balance identity. */
    record GlBalanceDelta(String accountCode, String currencyCode, BigDecimal signedDelta) {
        public GlBalanceDelta {
            Objects.requireNonNull(accountCode, "accountCode");
            Objects.requireNonNull(currencyCode, "currencyCode");
            Objects.requireNonNull(signedDelta, "signedDelta");
        }
    }

    /** Signed movement for one exact SL identity; nullable dimensions remain real key values. */
    record SlBalanceDelta(String accountCode, String businessPartnerCode, String departmentCode,
                          String currencyCode, BigDecimal signedDelta) {
        public SlBalanceDelta {
            Objects.requireNonNull(accountCode, "accountCode");
            Objects.requireNonNull(currencyCode, "currencyCode");
            Objects.requireNonNull(signedDelta, "signedDelta");
        }
    }

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

    /**
     * Shift beginning and ending amounts of existing rows strictly after {@code postingDate}.
     * Implementations must use set-based updates and evict stale managed balance entities.
     */
    void shiftSuccessorBalances(
            LocalDate postingDate, List<GlBalanceDelta> glDeltas, List<SlBalanceDelta> slDeltas);

    /** Latest date represented by a GL/SL projection or a POSTED source journal. */
    Optional<LocalDate> findLatestBalanceOrPostedDate();

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
