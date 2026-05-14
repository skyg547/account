package com.ho.account.journalledger.application.service.ledger;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.domain.journal.repository.JournalDetailRepository;
import com.ho.account.journalledger.domain.ledger.domain.GlBalance;
import com.ho.account.journalledger.domain.ledger.domain.SlBalance;
import com.ho.account.journalledger.domain.ledger.repository.GlBalanceRepository;
import com.ho.account.journalledger.domain.ledger.repository.SlBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 원장 잔액 서비스 (Ledger Service).
 */
@Service
@RequiredArgsConstructor
public class LedgerService {

    private final GlBalanceRepository glBalanceRepository;
    private final SlBalanceRepository slBalanceRepository;
    private final JournalDetailRepository journalDetailRepository;

    @Transactional
    public void updateLedgerBalances(JournalDetail journalDetail, LocalDate accountingDate) {
        String accountCode = journalDetail.getAccountCode();
        String businessPartnerCode = journalDetail.getBusinessPartnerCode();
        String departmentCode = journalDetail.getDepartmentCode();
        String currencyCode = journalDetail.getJournalEntry().getCurrencyCode();
        BigDecimal amount = journalDetail.getBaseAmount();
        boolean isDebit = JournalSide.DEBIT.equals(journalDetail.getSide());

        updateGlBalance(accountCode, currencyCode, amount, isDebit, accountingDate);
        updateSlBalance(accountCode, businessPartnerCode, departmentCode, currencyCode, amount, isDebit, accountingDate);
    }

    private void updateGlBalance(String accountCode,
                                 String currencyCode,
                                 BigDecimal amount,
                                 boolean isDebit,
                                 LocalDate accountingDate) {
        YearMonth period = YearMonth.from(accountingDate);

        Optional<GlBalance> optionalGlBalance = glBalanceRepository.findByAccountCodeAndCurrencyCodeAndBalanceDateAndPeriod(
                accountCode, currencyCode, accountingDate, period);

        GlBalance glBalance = optionalGlBalance.orElseGet(() -> {
            GlBalance newGlBalance = new GlBalance();
            newGlBalance.setAccountCode(accountCode);
            newGlBalance.setCurrencyCode(currencyCode);
            newGlBalance.setBalanceDate(accountingDate);
            newGlBalance.setPeriod(period);

            glBalanceRepository.findFirstByAccountCodeAndCurrencyCodeAndBalanceDateBeforeOrderByBalanceDateDesc(
                    accountCode, currencyCode, accountingDate)
                .ifPresent(prev -> newGlBalance.setBeginningBalance(prev.getEndingBalance()));

            return newGlBalance;
        });

        if (isDebit) {
            glBalance.addDebit(amount);
        } else {
            glBalance.addCredit(amount);
        }

        glBalanceRepository.save(glBalance);
    }

    private void updateSlBalance(String accountCode,
                                 String businessPartnerCode,
                                 String departmentCode,
                                 String currencyCode,
                                 BigDecimal amount,
                                 boolean isDebit,
                                 LocalDate accountingDate) {
        YearMonth period = YearMonth.from(accountingDate);

        Optional<SlBalance> optionalSlBalance =
                slBalanceRepository.findByAccountCodeAndBusinessPartnerCodeAndDepartmentCodeAndCurrencyCodeAndBalanceDateAndPeriod(
                        accountCode, businessPartnerCode, departmentCode, currencyCode, accountingDate, period);

        SlBalance slBalance = optionalSlBalance.orElseGet(() -> {
            SlBalance newSlBalance = new SlBalance();
            newSlBalance.setAccountCode(accountCode);
            newSlBalance.setBusinessPartnerCode(businessPartnerCode);
            newSlBalance.setDepartmentCode(departmentCode);
            newSlBalance.setCurrencyCode(currencyCode);
            newSlBalance.setBalanceDate(accountingDate);
            newSlBalance.setPeriod(period);

            slBalanceRepository.findFirstByAccountCodeAndBusinessPartnerCodeAndDepartmentCodeAndCurrencyCodeAndBalanceDateBeforeOrderByBalanceDateDesc(
                    accountCode, businessPartnerCode, departmentCode, currencyCode, accountingDate)
                .ifPresent(prev -> newSlBalance.setBeginningBalance(prev.getEndingBalance()));

            return newSlBalance;
        });

        if (isDebit) {
            slBalance.addDebit(amount);
        } else {
            slBalance.addCredit(amount);
        }

        slBalanceRepository.save(slBalance);
    }

    @Transactional
    public void reaggregateLedgerBalancesForPeriod(LocalDate startDate, LocalDate endDate) {
        glBalanceRepository.deleteAllInBatch(glBalanceRepository.findByBalanceDateBetween(startDate, endDate));
        slBalanceRepository.deleteAllInBatch(slBalanceRepository.findByBalanceDateBetween(startDate, endDate));

        List<JournalDetail> postedJournalDetails =
                journalDetailRepository.findPostedJournalDetailsByAccountingDateBetween(startDate, endDate);

        updateLedgerBalancesBulk(postedJournalDetails);
    }

    @Transactional
    public void updateLedgerBalancesBulk(List<JournalDetail> journalDetails) {
        if (journalDetails == null || journalDetails.isEmpty()) return;

        Map<LocalDate, List<JournalDetail>> groupedByDate = journalDetails.stream()
                .collect(Collectors.groupingBy(d -> d.getJournalEntry().getAccountingDate(), LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<LocalDate, List<JournalDetail>> entry : groupedByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<JournalDetail> details = entry.getValue();

            updateDailyBalances(date, details);
        }
    }

    private void updateDailyBalances(LocalDate date, List<JournalDetail> details) {
        Map<String, BigDecimal> glDebitMap = new LinkedHashMap<>();
        Map<String, BigDecimal> glCreditMap = new LinkedHashMap<>();
        Map<String, GlBalanceKey> glKeys = new LinkedHashMap<>();

        Map<String, BigDecimal> slDebitMap = new LinkedHashMap<>();
        Map<String, BigDecimal> slCreditMap = new LinkedHashMap<>();
        Map<String, SlBalanceKey> slKeys = new LinkedHashMap<>();

        for (JournalDetail detail : details) {
            String accountCode = detail.getAccountCode();
            String currencyCode = detail.getJournalEntry().getCurrencyCode();
            BigDecimal amount = detail.getBaseAmount();
            boolean isDebit = JournalSide.DEBIT.equals(detail.getSide());

            String glKeyStr = accountCode + "|" + currencyCode;
            glKeys.putIfAbsent(glKeyStr, new GlBalanceKey(accountCode, currencyCode));
            if (isDebit) {
                glDebitMap.merge(glKeyStr, amount, BigDecimal::add);
            } else {
                glCreditMap.merge(glKeyStr, amount, BigDecimal::add);
            }

            String partnerCode = detail.getBusinessPartnerCode();
            String deptCode = detail.getDepartmentCode();
            String slKeyStr = glKeyStr + "|" + (partnerCode != null ? partnerCode : "NULL") + "|" + (deptCode != null ? deptCode : "NULL");
            slKeys.putIfAbsent(slKeyStr, new SlBalanceKey(accountCode, partnerCode, deptCode, currencyCode));
            if (isDebit) {
                slDebitMap.merge(slKeyStr, amount, BigDecimal::add);
            } else {
                slCreditMap.merge(slKeyStr, amount, BigDecimal::add);
            }
        }

        for (Map.Entry<String, GlBalanceKey> entry : glKeys.entrySet()) {
            GlBalanceKey key = entry.getValue();
            BigDecimal debitSum = glDebitMap.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            BigDecimal creditSum = glCreditMap.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            updateGlBalanceWithSums(key.accountCode(), key.currencyCode(), debitSum, creditSum, date);
        }

        for (Map.Entry<String, SlBalanceKey> entry : slKeys.entrySet()) {
            SlBalanceKey key = entry.getValue();
            BigDecimal debitSum = slDebitMap.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            BigDecimal creditSum = slCreditMap.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            updateSlBalanceWithSums(key.accountCode(), key.partnerCode(), key.deptCode(), key.currencyCode(), debitSum, creditSum, date);
        }
    }

    private void updateGlBalanceWithSums(String accountCode, String currencyCode, BigDecimal debitSum, BigDecimal creditSum, LocalDate date) {
        YearMonth period = YearMonth.from(date);
        GlBalance glBalance = glBalanceRepository.findByAccountCodeAndCurrencyCodeAndBalanceDateAndPeriod(accountCode, currencyCode, date, period)
                .orElseGet(() -> {
                    GlBalance newBal = new GlBalance();
                    newBal.setAccountCode(accountCode);
                    newBal.setCurrencyCode(currencyCode);
                    newBal.setBalanceDate(date);
                    newBal.setPeriod(period);
                    glBalanceRepository.findFirstByAccountCodeAndCurrencyCodeAndBalanceDateBeforeOrderByBalanceDateDesc(accountCode, currencyCode, date)
                            .ifPresent(prev -> newBal.setBeginningBalance(prev.getEndingBalance()));
                    return newBal;
                });
        glBalance.addDebit(debitSum);
        glBalance.addCredit(creditSum);
        glBalanceRepository.save(glBalance);
    }

    private void updateSlBalanceWithSums(String accountCode, String partnerCode, String deptCode, String currencyCode, BigDecimal debitSum, BigDecimal creditSum, LocalDate date) {
        YearMonth period = YearMonth.from(date);
        SlBalance slBalance = slBalanceRepository.findByAccountCodeAndBusinessPartnerCodeAndDepartmentCodeAndCurrencyCodeAndBalanceDateAndPeriod(accountCode, partnerCode, deptCode, currencyCode, date, period)
                .orElseGet(() -> {
                    SlBalance newBal = new SlBalance();
                    newBal.setAccountCode(accountCode);
                    newBal.setBusinessPartnerCode(partnerCode);
                    newBal.setDepartmentCode(deptCode);
                    newBal.setCurrencyCode(currencyCode);
                    newBal.setBalanceDate(date);
                    newBal.setPeriod(period);
                    slBalanceRepository.findFirstByAccountCodeAndBusinessPartnerCodeAndDepartmentCodeAndCurrencyCodeAndBalanceDateBeforeOrderByBalanceDateDesc(accountCode, partnerCode, deptCode, currencyCode, date)
                            .ifPresent(prev -> newBal.setBeginningBalance(prev.getEndingBalance()));
                    return newBal;
                });
        slBalance.addDebit(debitSum);
        slBalance.addCredit(creditSum);
        slBalanceRepository.save(slBalance);
    }

    private record GlBalanceKey(String accountCode, String currencyCode) {}
    private record SlBalanceKey(String accountCode, String partnerCode, String deptCode, String currencyCode) {}

    @Transactional(readOnly = true)
    public List<GlBalance> getGlBalances(LocalDate startDate,
                                         LocalDate endDate,
                                         String accountCode,
                                         String currencyCode) {
        List<GlBalance> balances;
        if (accountCode != null && currencyCode != null) {
            balances = glBalanceRepository.findByBalanceDateBetweenAndAccountCodeAndCurrencyCode(
                    startDate, endDate, accountCode, currencyCode);
        } else if (accountCode != null) {
            balances = glBalanceRepository.findByBalanceDateBetween(startDate, endDate).stream()
                    .filter(balance -> accountCode.equals(balance.getAccountCode()))
                    .collect(Collectors.toList());
        } else if (currencyCode != null) {
            balances = glBalanceRepository.findByBalanceDateBetween(startDate, endDate).stream()
                    .filter(balance -> currencyCode.equals(balance.getCurrencyCode()))
                    .collect(Collectors.toList());
        } else {
            balances = glBalanceRepository.findByBalanceDateBetween(startDate, endDate);
        }
        return aggregateGlBalances(balances, startDate);
    }

    @Transactional(readOnly = true)
    public List<SlBalance> getSlBalances(LocalDate startDate,
                                         LocalDate endDate,
                                         String accountCode,
                                         String businessPartnerCode,
                                         String departmentCode,
                                         String currencyCode) {
        List<SlBalance> results = slBalanceRepository.findByBalanceDateBetween(startDate, endDate);

        if (accountCode != null) {
            results = results.stream()
                    .filter(balance -> accountCode.equals(balance.getAccountCode()))
                    .collect(Collectors.toList());
        }
        if (businessPartnerCode != null) {
            results = results.stream()
                    .filter(balance -> businessPartnerCode.equals(balance.getBusinessPartnerCode()))
                    .collect(Collectors.toList());
        }
        if (departmentCode != null) {
            results = results.stream()
                    .filter(balance -> departmentCode.equals(balance.getDepartmentCode()))
                    .collect(Collectors.toList());
        }
        if (currencyCode != null) {
            results = results.stream()
                    .filter(balance -> currencyCode.equals(balance.getCurrencyCode()))
                    .collect(Collectors.toList());
        }

        return aggregateSlBalances(results, startDate);
    }

    private List<GlBalance> aggregateGlBalances(List<GlBalance> balances, LocalDate balanceDate) {
        Map<String, GlBalance> aggregated = new LinkedHashMap<>();
        for (GlBalance balance : balances) {
            String key = balance.getAccountCode() + "|" + (balance.getCurrencyCode() != null ? balance.getCurrencyCode() : "");
            GlBalance target = aggregated.computeIfAbsent(key, ignored -> createGlAggregate(balance, balanceDate));
            mergeIntoGlBalance(target, balance);
        }
        return new ArrayList<>(aggregated.values());
    }

    private List<SlBalance> aggregateSlBalances(List<SlBalance> balances, LocalDate balanceDate) {
        Map<String, SlBalance> aggregated = new LinkedHashMap<>();
        for (SlBalance balance : balances) {
            String key = balance.getAccountCode() + "|"
                    + (balance.getBusinessPartnerCode() != null ? balance.getBusinessPartnerCode() : "") + "|"
                    + (balance.getDepartmentCode() != null ? balance.getDepartmentCode() : "") + "|"
                    + (balance.getCurrencyCode() != null ? balance.getCurrencyCode() : "");
            SlBalance target = aggregated.computeIfAbsent(key, ignored -> createSlAggregate(balance, balanceDate));
            mergeIntoSlBalance(target, balance);
        }
        return new ArrayList<>(aggregated.values());
    }

    private GlBalance createGlAggregate(GlBalance source, LocalDate balanceDate) {
        GlBalance aggregated = new GlBalance();
        aggregated.setAccountCode(source.getAccountCode());
        aggregated.setCurrencyCode(source.getCurrencyCode());
        aggregated.setBalanceDate(balanceDate);
        aggregated.setPeriod(YearMonth.from(balanceDate));
        aggregated.setBeginningBalance(BigDecimal.ZERO);
        aggregated.setDebitAmount(BigDecimal.ZERO);
        aggregated.setCreditAmount(BigDecimal.ZERO);
        aggregated.setEndingBalance(BigDecimal.ZERO);
        return aggregated;
    }

    private SlBalance createSlAggregate(SlBalance source, LocalDate balanceDate) {
        SlBalance aggregated = new SlBalance();
        aggregated.setAccountCode(source.getAccountCode());
        aggregated.setBusinessPartnerCode(source.getBusinessPartnerCode());
        aggregated.setDepartmentCode(source.getDepartmentCode());
        aggregated.setCurrencyCode(source.getCurrencyCode());
        aggregated.setBalanceDate(balanceDate);
        aggregated.setPeriod(YearMonth.from(balanceDate));
        aggregated.setBeginningBalance(BigDecimal.ZERO);
        aggregated.setDebitAmount(BigDecimal.ZERO);
        aggregated.setCreditAmount(BigDecimal.ZERO);
        aggregated.setEndingBalance(BigDecimal.ZERO);
        return aggregated;
    }

    private void mergeIntoGlBalance(GlBalance target, GlBalance source) {
        target.setBeginningBalance(target.getBeginningBalance().add(zeroIfNull(source.getBeginningBalance())));
        target.setDebitAmount(target.getDebitAmount().add(zeroIfNull(source.getDebitAmount())));
        target.setCreditAmount(target.getCreditAmount().add(zeroIfNull(source.getCreditAmount())));
        target.recalculate();
    }

    private void mergeIntoSlBalance(SlBalance target, SlBalance source) {
        target.setBeginningBalance(target.getBeginningBalance().add(zeroIfNull(source.getBeginningBalance())));
        target.setDebitAmount(target.getDebitAmount().add(zeroIfNull(source.getDebitAmount())));
        target.setCreditAmount(target.getCreditAmount().add(zeroIfNull(source.getCreditAmount())));
        target.recalculate();
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}