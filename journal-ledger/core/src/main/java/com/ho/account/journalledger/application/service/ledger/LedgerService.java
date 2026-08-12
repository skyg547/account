package com.ho.account.journalledger.application.service.ledger;

import com.ho.account.contracts.ledger.LedgerAggregateSummary;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.application.port.out.LedgerBalancePersistencePort;
import com.ho.account.journalledger.domain.ledger.domain.GlBalance;
import com.ho.account.journalledger.domain.ledger.domain.SlBalance;
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
 *
 * <p>전기된 전표 라인을 날짜·계정·통화·거래처·부서 단위 잔액으로 집계합니다.
 * 과거 날짜 전표가 들어오면 지정 기간의 POSTED 라인을 다시 읽어 잔액을 재집계할 수 있습니다.</p>
 *
 * <p>잔액 저장·조회·재집계 대상 로딩은 `LedgerBalancePersistencePort` 출력 포트에 위임합니다.
 * 조회 필터는 DB 어댑터에서 적용하므로 전체 기간 데이터를 애플리케이션 메모리에 올리지 않습니다.</p>
 */
@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerBalancePersistencePort ledgerBalancePersistencePort;

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

        Optional<GlBalance> optionalGlBalance = ledgerBalancePersistencePort.findGlBalance(
                accountCode, currencyCode, accountingDate, period);

        GlBalance glBalance = optionalGlBalance.orElseGet(() -> {
            GlBalance newGlBalance = new GlBalance();
            newGlBalance.setAccountCode(accountCode);
            newGlBalance.setCurrencyCode(currencyCode);
            newGlBalance.setBalanceDate(accountingDate);
            newGlBalance.setPeriod(period);

            ledgerBalancePersistencePort.findPreviousGlBalance(
                    accountCode, currencyCode, accountingDate)
                .ifPresent(prev -> newGlBalance.setBeginningBalance(prev.getEndingBalance()));

            return newGlBalance;
        });

        if (isDebit) {
            glBalance.addDebit(amount);
        } else {
            glBalance.addCredit(amount);
        }

        ledgerBalancePersistencePort.saveGlBalance(glBalance);
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
                ledgerBalancePersistencePort.findSlBalance(
                        accountCode, businessPartnerCode, departmentCode, currencyCode, accountingDate, period);

        SlBalance slBalance = optionalSlBalance.orElseGet(() -> {
            SlBalance newSlBalance = new SlBalance();
            newSlBalance.setAccountCode(accountCode);
            newSlBalance.setBusinessPartnerCode(businessPartnerCode);
            newSlBalance.setDepartmentCode(departmentCode);
            newSlBalance.setCurrencyCode(currencyCode);
            newSlBalance.setBalanceDate(accountingDate);
            newSlBalance.setPeriod(period);

            ledgerBalancePersistencePort.findPreviousSlBalance(
                    accountCode, businessPartnerCode, departmentCode, currencyCode, accountingDate)
                .ifPresent(prev -> newSlBalance.setBeginningBalance(prev.getEndingBalance()));

            return newSlBalance;
        });

        if (isDebit) {
            slBalance.addDebit(amount);
        } else {
            slBalance.addCredit(amount);
        }

        ledgerBalancePersistencePort.saveSlBalance(slBalance);
    }

    /**
     * 지정된 기간의 GL/SL 원장 잔액 데이터를 사전 삭제(초기화)합니다.
     *
     * <p>초보자 설명: 배치 작업 재실행 시 기존 잔액 데이터 위에 중복으로 금액이 누적되는 문제를 방지하기 위해,
     * 재집계 Step(Chunk 프로세싱) 시작 전 대상 기간의 잔액을 먼저 깨끗이 지웁니다.
     * 이 작업은 배치의 멱등성(Idempotency, 몇 번을 실행해도 동일한 결과를 보장)을 달성하는 핵심 단계입니다.</p>
     *
     * @param startDate 집계 시작일
     * @param endDate   집계 종료일
     */
    @Transactional
    public void clearLedgerBalancesForPeriod(LocalDate startDate, LocalDate endDate) {
        ledgerBalancePersistencePort.deleteBalancesBetween(startDate, endDate);
    }

    @Transactional
    public void reaggregateLedgerBalancesForPeriod(LocalDate startDate, LocalDate endDate) {
        clearLedgerBalancesForPeriod(startDate, endDate);

        List<JournalDetail> postedJournalDetails =
                ledgerBalancePersistencePort.findPostedJournalDetailsBetween(startDate, endDate);

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

        List<GlBalance> glBalancesToSave = new ArrayList<>();
        for (Map.Entry<String, GlBalanceKey> entry : glKeys.entrySet()) {
            GlBalanceKey key = entry.getValue();
            BigDecimal debitSum = glDebitMap.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            BigDecimal creditSum = glCreditMap.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            glBalancesToSave.add(createOrUpdateGlBalanceWithSums(
                    key.accountCode(), key.currencyCode(), debitSum, creditSum, date));
        }

        if (!glBalancesToSave.isEmpty()) {
            ledgerBalancePersistencePort.saveGlBalances(glBalancesToSave);
        }

        List<SlBalance> slBalancesToSave = new ArrayList<>();
        for (Map.Entry<String, SlBalanceKey> entry : slKeys.entrySet()) {
            SlBalanceKey key = entry.getValue();
            BigDecimal debitSum = slDebitMap.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            BigDecimal creditSum = slCreditMap.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            slBalancesToSave.add(createOrUpdateSlBalanceWithSums(
                    key.accountCode(), key.partnerCode(), key.deptCode(), key.currencyCode(), debitSum, creditSum, date));
        }

        if (!slBalancesToSave.isEmpty()) {
            ledgerBalancePersistencePort.saveSlBalances(slBalancesToSave);
        }
    }

    private GlBalance createOrUpdateGlBalanceWithSums(
            String accountCode, String currencyCode, BigDecimal debitSum, BigDecimal creditSum, LocalDate date) {
        YearMonth period = YearMonth.from(date);
        GlBalance glBalance = ledgerBalancePersistencePort.findGlBalance(accountCode, currencyCode, date, period)
                .orElseGet(() -> {
                    GlBalance newBal = new GlBalance();
                    newBal.setAccountCode(accountCode);
                    newBal.setCurrencyCode(currencyCode);
                    newBal.setBalanceDate(date);
                    newBal.setPeriod(period);
                    ledgerBalancePersistencePort.findPreviousGlBalance(accountCode, currencyCode, date)
                            .ifPresent(prev -> newBal.setBeginningBalance(prev.getEndingBalance()));
                    return newBal;
                });
        glBalance.addDebit(debitSum);
        glBalance.addCredit(creditSum);
        return glBalance;
    }

    private SlBalance createOrUpdateSlBalanceWithSums(
            String accountCode, String partnerCode, String deptCode, String currencyCode,
            BigDecimal debitSum, BigDecimal creditSum, LocalDate date) {
        YearMonth period = YearMonth.from(date);
        SlBalance slBalance = ledgerBalancePersistencePort.findSlBalance(accountCode, partnerCode, deptCode, currencyCode, date, period)
                .orElseGet(() -> {
                    SlBalance newBal = new SlBalance();
                    newBal.setAccountCode(accountCode);
                    newBal.setBusinessPartnerCode(partnerCode);
                    newBal.setDepartmentCode(deptCode);
                    newBal.setCurrencyCode(currencyCode);
                    newBal.setBalanceDate(date);
                    newBal.setPeriod(period);
                    ledgerBalancePersistencePort.findPreviousSlBalance(accountCode, partnerCode, deptCode, currencyCode, date)
                            .ifPresent(prev -> newBal.setBeginningBalance(prev.getEndingBalance()));
                    return newBal;
                });
        slBalance.addDebit(debitSum);
        slBalance.addCredit(creditSum);
        return slBalance;
    }

    private record GlBalanceKey(String accountCode, String currencyCode) {}
    private record SlBalanceKey(String accountCode, String partnerCode, String deptCode, String currencyCode) {}

    @Transactional(readOnly = true)
    public List<GlBalance> getGlBalances(LocalDate startDate,
                                         LocalDate endDate,
                                         String accountCode,
                                         String currencyCode) {
        List<GlBalance> balances =
                ledgerBalancePersistencePort.findGlBalances(startDate, endDate, accountCode, currencyCode);
        return aggregateGlBalances(balances, startDate);
    }

    @Transactional(readOnly = true)
    public List<SlBalance> getSlBalances(LocalDate startDate,
                                         LocalDate endDate,
                                         String accountCode,
                                         String businessPartnerCode,
                                         String departmentCode,
                                         String currencyCode) {
        List<SlBalance> results = ledgerBalancePersistencePort.findSlBalances(
                startDate, endDate, accountCode, businessPartnerCode, departmentCode, currencyCode);
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

    @Transactional(readOnly = true)
    public LedgerAggregateSummary calculateGlBalanceAggregate(LocalDate startDate,
                                                               LocalDate endDate,
                                                               String accountCode,
                                                               String currencyCode,
                                                               String amountBasis) {
        return ledgerBalancePersistencePort.calculateGlBalanceAggregate(startDate, endDate, accountCode, currencyCode, amountBasis);
    }
}
