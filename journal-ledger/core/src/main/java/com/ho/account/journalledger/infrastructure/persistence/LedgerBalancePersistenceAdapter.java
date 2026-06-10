package com.ho.account.journalledger.infrastructure.persistence;

import com.ho.account.journalledger.application.port.out.LedgerBalancePersistencePort;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.repository.JournalDetailRepository;
import com.ho.account.journalledger.domain.ledger.domain.GlBalance;
import com.ho.account.journalledger.domain.ledger.domain.SlBalance;
import com.ho.account.journalledger.domain.ledger.repository.GlBalanceRepository;
import com.ho.account.journalledger.domain.ledger.repository.SlBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * 원장 잔액 출력 포트를 Spring Data JPA 조회와 bulk 삭제에 연결하는 어댑터입니다.
 */
@Component
@ConditionalOnProperty(name = "journal-ledger.ledger.persistence-mode", havingValue = "jpa", matchIfMissing = true)
@RequiredArgsConstructor
public class LedgerBalancePersistenceAdapter implements LedgerBalancePersistencePort {

    private final GlBalanceRepository glBalanceRepository;
    private final SlBalanceRepository slBalanceRepository;
    private final JournalDetailRepository journalDetailRepository;

    @Override
    public Optional<GlBalance> findGlBalance(
            String accountCode, String currencyCode, LocalDate balanceDate, YearMonth period) {
        return glBalanceRepository.findByAccountCodeAndCurrencyCodeAndBalanceDateAndPeriod(
                accountCode, currencyCode, balanceDate, period);
    }

    @Override
    public Optional<GlBalance> findPreviousGlBalance(
            String accountCode, String currencyCode, LocalDate balanceDate) {
        return glBalanceRepository.findFirstByAccountCodeAndCurrencyCodeAndBalanceDateBeforeOrderByBalanceDateDesc(
                accountCode, currencyCode, balanceDate);
    }

    @Override
    public GlBalance saveGlBalance(GlBalance balance) {
        return glBalanceRepository.save(balance);
    }

    @Override
    public void saveGlBalances(List<GlBalance> balances) {
        glBalanceRepository.saveAll(balances);
    }

    @Override
    public Optional<SlBalance> findSlBalance(
            String accountCode, String businessPartnerCode, String departmentCode,
            String currencyCode, LocalDate balanceDate, YearMonth period) {
        return slBalanceRepository.findByAccountCodeAndBusinessPartnerCodeAndDepartmentCodeAndCurrencyCodeAndBalanceDateAndPeriod(
                accountCode, businessPartnerCode, departmentCode, currencyCode, balanceDate, period);
    }

    @Override
    public Optional<SlBalance> findPreviousSlBalance(
            String accountCode, String businessPartnerCode, String departmentCode,
            String currencyCode, LocalDate balanceDate) {
        return slBalanceRepository.findFirstByAccountCodeAndBusinessPartnerCodeAndDepartmentCodeAndCurrencyCodeAndBalanceDateBeforeOrderByBalanceDateDesc(
                accountCode, businessPartnerCode, departmentCode, currencyCode, balanceDate);
    }

    @Override
    public SlBalance saveSlBalance(SlBalance balance) {
        return slBalanceRepository.save(balance);
    }

    @Override
    public void saveSlBalances(List<SlBalance> balances) {
        slBalanceRepository.saveAll(balances);
    }

    @Override
    public void deleteBalancesBetween(LocalDate startDate, LocalDate endDate) {
        glBalanceRepository.deleteAllInBatch(glBalanceRepository.findByBalanceDateBetween(startDate, endDate));
        slBalanceRepository.deleteAllInBatch(slBalanceRepository.findByBalanceDateBetween(startDate, endDate));
    }

    @Override
    public List<JournalDetail> findPostedJournalDetailsBetween(LocalDate startDate, LocalDate endDate) {
        return journalDetailRepository.findPostedJournalDetailsByAccountingDateBetween(startDate, endDate);
    }

    @Override
    public List<GlBalance> findGlBalances(
            LocalDate startDate, LocalDate endDate, String accountCode, String currencyCode) {
        return glBalanceRepository.findForQuery(startDate, endDate, accountCode, currencyCode);
    }

    @Override
    public List<SlBalance> findSlBalances(
            LocalDate startDate, LocalDate endDate, String accountCode,
            String businessPartnerCode, String departmentCode, String currencyCode) {
        return slBalanceRepository.findForQuery(
                startDate, endDate, accountCode, businessPartnerCode, departmentCode, currencyCode);
    }
}
