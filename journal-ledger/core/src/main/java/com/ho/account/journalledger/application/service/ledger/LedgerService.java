package com.ho.account.journalledger.application.service.ledger;

import com.ho.account.contracts.ledger.LedgerAggregateSummary;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.application.port.out.LedgerBalancePersistencePort;
import com.ho.account.journalledger.application.port.out.BalanceReaggregationControlPort;
import com.ho.account.journalledger.application.port.out.LedgerBalancePersistencePort.BalanceAccount;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
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
    private final BalanceReaggregationControlPort reaggregationControlPort;

    @Transactional
    public void updateLedgerBalances(JournalDetail journalDetail, LocalDate accountingDate) {
        String accountCode = journalDetail.getAccountCode();
        String businessPartnerCode = journalDetail.getBusinessPartnerCode();
        String departmentCode = journalDetail.getDepartmentCode();
        String currencyCode = journalDetail.getJournalEntry().getCurrencyCode();
        BigDecimal amount = journalDetail.getBaseAmount();
        boolean isDebit = JournalSide.DEBIT.equals(journalDetail.getSide());

        ledgerBalancePersistencePort.lockBalanceAccounts(List.of(new BalanceAccount(accountCode, currencyCode)));
        reaggregationControlPort.assertOpen();
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
     * <p>부분 결과를 공개하는 직접 cleanup은 안전하지 않으므로 거부합니다. Batch는
     * {@link BalanceReaggregationService}의 owner 검증 경로를 사용해야 합니다.</p>
     *
     * @param startDate 집계 시작일
     * @param endDate   집계 종료일
     */
    @Transactional
    public void clearLedgerBalancesForPeriod(LocalDate startDate, LocalDate endDate) {
        throw new IllegalStateException("Direct balance cleanup is unsafe; use the owner-controlled reaggregation job");
    }

    @Transactional
    public void reaggregateLedgerBalancesForPeriod(LocalDate startDate, LocalDate endDate) {
        ledgerBalancePersistencePort.lockAllBalanceAccounts();
        reaggregationControlPort.assertOpen();
        ledgerBalancePersistencePort.deleteBalancesBetween(startDate, endDate);

        List<JournalDetail> postedJournalDetails =
                ledgerBalancePersistencePort.findPostedJournalDetailsBetween(startDate, endDate);

        updateLedgerBalancesBulk(postedJournalDetails);
    }

    @Transactional
    public void updateLedgerBalancesBulk(List<JournalDetail> journalDetails) {
        if (journalDetails == null || journalDetails.isEmpty()) return;

        // Lock the complete account set before the first balance read. Locking line by line
        // would invert the order for journals whose debit/credit account order differs.
        ledgerBalancePersistencePort.lockBalanceAccounts(journalDetails.stream()
                .map(detail -> new BalanceAccount(detail.getAccountCode(), detail.getJournalEntry().getCurrencyCode()))
                .distinct().toList());
        reaggregationControlPort.assertOpen();
        updateLedgerBalancesBulkAfterGuard(journalDetails);
    }

    @Transactional
    public void updateLedgerBalancesBulkForReaggregation(long ownerJobInstanceId,
                                                          LocalDate startDate,
                                                          LocalDate endDate,
                                                          List<JournalDetail> journalDetails) {
        if (journalDetails == null || journalDetails.isEmpty()) return;
        ledgerBalancePersistencePort.lockBalanceAccounts(journalDetails.stream()
                .map(detail -> new BalanceAccount(detail.getAccountCode(), detail.getJournalEntry().getCurrencyCode()))
                .distinct().toList());
        reaggregationControlPort.assertOwner(ownerJobInstanceId, startDate, endDate);
        updateLedgerBalancesBulkAfterGuard(journalDetails);
    }

    private void updateLedgerBalancesBulkAfterGuard(List<JournalDetail> journalDetails) {
        Map<LocalDate, List<JournalDetail>> groupedByDate = journalDetails.stream()
                // Earlier days must be persisted first so later days inherit their closing balance.
                .collect(Collectors.groupingBy(d -> d.getJournalEntry().getAccountingDate(), TreeMap::new, Collectors.toList()));

        for (Map.Entry<LocalDate, List<JournalDetail>> entry : groupedByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<JournalDetail> details = entry.getValue();

            updateDailyBalances(date, details);
        }
    }

    private void updateDailyBalances(LocalDate date, List<JournalDetail> details) {
        Map<GlBalanceKey, BigDecimal> glDebitMap = new LinkedHashMap<>();
        Map<GlBalanceKey, BigDecimal> glCreditMap = new LinkedHashMap<>();
        Set<GlBalanceKey> glKeys = new LinkedHashSet<>();

        Map<SlBalanceKey, BigDecimal> slDebitMap = new LinkedHashMap<>();
        Map<SlBalanceKey, BigDecimal> slCreditMap = new LinkedHashMap<>();
        Set<SlBalanceKey> slKeys = new LinkedHashSet<>();

        for (JournalDetail detail : details) {
            String accountCode = detail.getAccountCode();
            String currencyCode = detail.getJournalEntry().getCurrencyCode();
            BigDecimal amount = detail.getBaseAmount();
            boolean isDebit = JournalSide.DEBIT.equals(detail.getSide());

            GlBalanceKey glKey = new GlBalanceKey(accountCode, currencyCode);
            glKeys.add(glKey);
            if (isDebit) {
                glDebitMap.merge(glKey, amount, BigDecimal::add);
            } else {
                glCreditMap.merge(glKey, amount, BigDecimal::add);
            }

            String partnerCode = detail.getBusinessPartnerCode();
            String deptCode = detail.getDepartmentCode();
            // SQL NULL is a real dimension value, distinct from a literal code "NULL" or "|".
            SlBalanceKey slKey = new SlBalanceKey(accountCode, partnerCode, deptCode, currencyCode);
            slKeys.add(slKey);
            if (isDebit) {
                slDebitMap.merge(slKey, amount, BigDecimal::add);
            } else {
                slCreditMap.merge(slKey, amount, BigDecimal::add);
            }
        }

        List<GlBalance> glBalancesToSave = new ArrayList<>();
        for (GlBalanceKey key : glKeys) {
            BigDecimal debitSum = glDebitMap.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal creditSum = glCreditMap.getOrDefault(key, BigDecimal.ZERO);
            glBalancesToSave.add(createOrUpdateGlBalanceWithSums(
                    key.accountCode(), key.currencyCode(), debitSum, creditSum, date));
        }

        if (!glBalancesToSave.isEmpty()) {
            ledgerBalancePersistencePort.saveGlBalances(glBalancesToSave);
        }

        List<SlBalance> slBalancesToSave = new ArrayList<>();
        for (SlBalanceKey key : slKeys) {
            BigDecimal debitSum = slDebitMap.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal creditSum = slCreditMap.getOrDefault(key, BigDecimal.ZERO);
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
        BalanceReaggregationControlPort.ControlSnapshot before = readableSnapshot();
        List<GlBalance> balances =
                ledgerBalancePersistencePort.findGlBalances(startDate, endDate, accountCode, currencyCode);
        List<GlBalance> result = aggregateGlBalances(balances, startDate);
        verifyReadable(before);
        return result;
    }

    @Transactional(readOnly = true)
    public List<SlBalance> getSlBalances(LocalDate startDate,
                                         LocalDate endDate,
                                         String accountCode,
                                         String businessPartnerCode,
                                         String departmentCode,
                                         String currencyCode) {
        BalanceReaggregationControlPort.ControlSnapshot before = readableSnapshot();
        List<SlBalance> results = ledgerBalancePersistencePort.findSlBalances(
                startDate, endDate, accountCode, businessPartnerCode, departmentCode, currencyCode);
        List<SlBalance> result = aggregateSlBalances(results, startDate);
        verifyReadable(before);
        return result;
    }

    private List<GlBalance> aggregateGlBalances(List<GlBalance> balances, LocalDate balanceDate) {
        Map<String, GlBalance> aggregated = new LinkedHashMap<>();
        Map<String, LocalDate> earliestDates = new LinkedHashMap<>();
        for (GlBalance balance : balances) {
            String key = balance.getAccountCode() + "|" + (balance.getCurrencyCode() != null ? balance.getCurrencyCode() : "");
            GlBalance target = aggregated.computeIfAbsent(key, ignored -> createGlAggregate(balance, balanceDate));
            LocalDate earliestDate = earliestDates.get(key);
            // 일별 기초에는 이전 기말이 이미 이월되어 있다. 응답 날짜와 별도로 최소 날짜의 기초만 선택한다.
            if (earliestDate == null || balance.getBalanceDate().isBefore(earliestDate)) {
                earliestDates.put(key, balance.getBalanceDate());
                target.setBeginningBalance(zeroIfNull(balance.getBeginningBalance()));
            }
            mergeGlMovements(target, balance);
        }
        // 입력 순서에 따른 미완성 기초/흐름 조합을 평가하지 않고, 그룹 집계가 끝난 뒤 기말을 계산한다.
        aggregated.values().forEach(GlBalance::recalculate);
        return new ArrayList<>(aggregated.values());
    }

    private List<SlBalance> aggregateSlBalances(List<SlBalance> balances, LocalDate balanceDate) {
        Map<String, SlBalance> aggregated = new LinkedHashMap<>();
        Map<String, LocalDate> earliestDates = new LinkedHashMap<>();
        for (SlBalance balance : balances) {
            String key = balance.getAccountCode() + "|"
                    + (balance.getBusinessPartnerCode() != null ? balance.getBusinessPartnerCode() : "") + "|"
                    + (balance.getDepartmentCode() != null ? balance.getDepartmentCode() : "") + "|"
                    + (balance.getCurrencyCode() != null ? balance.getCurrencyCode() : "");
            SlBalance target = aggregated.computeIfAbsent(key, ignored -> createSlAggregate(balance, balanceDate));
            LocalDate earliestDate = earliestDates.get(key);
            // 거래처·부서별로도 날짜가 가장 이른 조회 행의 기초만 취하고, 원본 행과 목록은 변경하지 않는다.
            if (earliestDate == null || balance.getBalanceDate().isBefore(earliestDate)) {
                earliestDates.put(key, balance.getBalanceDate());
                target.setBeginningBalance(zeroIfNull(balance.getBeginningBalance()));
            }
            mergeSlMovements(target, balance);
        }
        aggregated.values().forEach(SlBalance::recalculate);
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

    private void mergeGlMovements(GlBalance target, GlBalance source) {
        target.setDebitAmount(target.getDebitAmount().add(zeroIfNull(source.getDebitAmount())));
        target.setCreditAmount(target.getCreditAmount().add(zeroIfNull(source.getCreditAmount())));
    }

    private void mergeSlMovements(SlBalance target, SlBalance source) {
        target.setDebitAmount(target.getDebitAmount().add(zeroIfNull(source.getDebitAmount())));
        target.setCreditAmount(target.getCreditAmount().add(zeroIfNull(source.getCreditAmount())));
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
        BalanceReaggregationControlPort.ControlSnapshot before = readableSnapshot();
        LedgerAggregateSummary result = ledgerBalancePersistencePort.calculateGlBalanceAggregate(
                startDate, endDate, accountCode, currencyCode, amountBasis);
        verifyReadable(before);
        return result;
    }

    private BalanceReaggregationControlPort.ControlSnapshot readableSnapshot() {
        BalanceReaggregationControlPort.ControlSnapshot snapshot = reaggregationControlPort.snapshot();
        if (!snapshot.isOpen()) {
            throw new IllegalStateException("Ledger balance reaggregation is active; reads are unavailable");
        }
        return snapshot;
    }

    private void verifyReadable(BalanceReaggregationControlPort.ControlSnapshot before) {
        BalanceReaggregationControlPort.ControlSnapshot after = reaggregationControlPort.snapshot();
        if (!after.isOpen() || after.epoch() != before.epoch()) {
            throw new IllegalStateException("Ledger balance reaggregation changed while the balance read was materialized");
        }
    }
}
