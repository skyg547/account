package com.ho.account.journalledger.application.service.ledger;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Currency;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
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

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final GlBalanceRepository glBalanceRepository;
    private final SlBalanceRepository slBalanceRepository;
    private final JournalDetailRepository journalDetailRepository;

    @Transactional
    public void updateLedgerBalances(JournalDetail journalDetail, LocalDate accountingDate) {
        AccountSubject accountSubject = journalDetail.getAccountSubject();
        BusinessPartner businessPartner = journalDetail.getBusinessPartner();
        Department department = journalDetail.getDepartment();
        Currency currency = journalDetail.getJournalEntry().getCurrency();
        BigDecimal amount = journalDetail.getBaseAmount();
        boolean isDebit = "DEBIT".equals(journalDetail.getDrcrType());

        updateGlBalance(accountSubject, currency, amount, isDebit, accountingDate);
        updateSlBalance(accountSubject, businessPartner, department, currency, amount, isDebit, accountingDate);
    }

    private void updateGlBalance(AccountSubject accountSubject,
                                 Currency currency,
                                 BigDecimal amount,
                                 boolean isDebit,
                                 LocalDate accountingDate) {
        YearMonth period = YearMonth.from(accountingDate);

        Optional<GlBalance> optionalGlBalance = glBalanceRepository.findByAccountSubjectAndCurrencyAndBalanceDateAndPeriod(
                accountSubject, currency, accountingDate, period);

        GlBalance glBalance = optionalGlBalance.orElseGet(() -> {
            GlBalance newGlBalance = new GlBalance();
            newGlBalance.setAccountSubject(accountSubject);
            newGlBalance.setCurrency(currency);
            newGlBalance.setBalanceDate(accountingDate);
            newGlBalance.setPeriod(period);

            // ?붿븸 ?댁썡 (Carry-forward) 濡쒖쭅
            glBalanceRepository.findFirstByAccountSubjectAndCurrencyAndBalanceDateBeforeOrderByBalanceDateDesc(
                    accountSubject, currency, accountingDate)
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

    private void updateSlBalance(AccountSubject accountSubject,
                                 BusinessPartner businessPartner,
                                 Department department,
                                 Currency currency,
                                 BigDecimal amount,
                                 boolean isDebit,
                                 LocalDate accountingDate) {
        YearMonth period = YearMonth.from(accountingDate);

        Optional<SlBalance> optionalSlBalance =
                slBalanceRepository.findByAccountSubjectAndBusinessPartnerAndDepartmentAndCurrencyAndBalanceDateAndPeriod(
                        accountSubject, businessPartner, department, currency, accountingDate, period);

        SlBalance slBalance = optionalSlBalance.orElseGet(() -> {
            SlBalance newSlBalance = new SlBalance();
            newSlBalance.setAccountSubject(accountSubject);
            newSlBalance.setBusinessPartner(businessPartner);
            newSlBalance.setDepartment(department);
            newSlBalance.setCurrency(currency);
            newSlBalance.setBalanceDate(accountingDate);
            newSlBalance.setPeriod(period);

            // ?붿븸 ?댁썡 (Carry-forward) 濡쒖쭅
            slBalanceRepository.findFirstByAccountSubjectAndBusinessPartnerAndDepartmentAndCurrencyAndBalanceDateBeforeOrderByBalanceDateDesc(
                    accountSubject, businessPartner, department, currency, accountingDate)
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

        for (JournalDetail detail : postedJournalDetails) {
            updateLedgerBalances(detail, detail.getJournalEntry().getAccountingDate());
        }
    }

    @Transactional(readOnly = true)
    public List<GlBalance> getGlBalances(LocalDate startDate,
                                         LocalDate endDate,
                                         AccountSubject accountSubject,
                                         Currency currency) {
        List<GlBalance> balances;
        if (accountSubject != null && currency != null) {
            balances = glBalanceRepository.findByBalanceDateBetweenAndAccountSubjectAndCurrency(
                    startDate, endDate, accountSubject, currency);
        } else if (accountSubject != null) {
            balances = glBalanceRepository.findByBalanceDateBetween(startDate, endDate).stream()
                    .filter(balance -> balance.getAccountSubject().equals(accountSubject))
                    .collect(Collectors.toList());
        } else if (currency != null) {
            balances = glBalanceRepository.findByBalanceDateBetween(startDate, endDate).stream()
                    .filter(balance -> balance.getCurrency().equals(currency))
                    .collect(Collectors.toList());
        } else {
            balances = glBalanceRepository.findByBalanceDateBetween(startDate, endDate);
        }
        return aggregateGlBalances(balances, startDate);
    }

    @Transactional(readOnly = true)
    public List<SlBalance> getSlBalances(LocalDate startDate,
                                         LocalDate endDate,
                                         AccountSubject accountSubject,
                                         BusinessPartner businessPartner,
                                         Department department,
                                         Currency currency) {
        List<SlBalance> results = slBalanceRepository.findByBalanceDateBetween(startDate, endDate);

        if (accountSubject != null) {
            results = results.stream()
                    .filter(balance -> balance.getAccountSubject() != null
                            && accountSubject.getCode().equals(balance.getAccountSubject().getCode()))
                    .collect(Collectors.toList());
        }
        if (businessPartner != null) {
            results = results.stream()
                    .filter(balance -> balance.getBusinessPartner() != null
                            && businessPartner.getBusinessPartnerCode()
                                    .equals(balance.getBusinessPartner().getBusinessPartnerCode()))
                    .collect(Collectors.toList());
        }
        if (department != null) {
            results = results.stream()
                    .filter(balance -> balance.getDepartment() != null
                            && department.getCode().equals(balance.getDepartment().getCode()))
                    .collect(Collectors.toList());
        }
        if (currency != null) {
            results = results.stream()
                    .filter(balance -> balance.getCurrency() != null
                            && currency.getCode().equals(balance.getCurrency().getCode()))
                    .collect(Collectors.toList());
        }

        return aggregateSlBalances(results, startDate);
    }

    private List<GlBalance> aggregateGlBalances(List<GlBalance> balances, LocalDate balanceDate) {
        Map<String, GlBalance> aggregated = new LinkedHashMap<>();
        for (GlBalance balance : balances) {
            String key = balance.getAccountSubject().getCode() + "|"
                    + (balance.getCurrency() != null ? balance.getCurrency().getCode() : "");
            GlBalance target = aggregated.computeIfAbsent(key, ignored -> createGlAggregate(balance, balanceDate));
            mergeIntoGlBalance(target, balance);
        }
        return new ArrayList<>(aggregated.values());
    }

    private List<SlBalance> aggregateSlBalances(List<SlBalance> balances, LocalDate balanceDate) {
        Map<String, SlBalance> aggregated = new LinkedHashMap<>();
        for (SlBalance balance : balances) {
            String key = balance.getAccountSubject().getCode() + "|"
                    + (balance.getBusinessPartner() != null ? balance.getBusinessPartner().getBusinessPartnerCode() : "") + "|"
                    + (balance.getDepartment() != null ? balance.getDepartment().getCode() : "") + "|"
                    + (balance.getCurrency() != null ? balance.getCurrency().getCode() : "");
            SlBalance target = aggregated.computeIfAbsent(key, ignored -> createSlAggregate(balance, balanceDate));
            mergeIntoSlBalance(target, balance);
        }
        return new ArrayList<>(aggregated.values());
    }

    private GlBalance createGlAggregate(GlBalance source, LocalDate balanceDate) {
        GlBalance aggregated = new GlBalance();
        aggregated.setAccountSubject(source.getAccountSubject());
        aggregated.setCurrency(source.getCurrency());
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
        aggregated.setAccountSubject(source.getAccountSubject());
        aggregated.setBusinessPartner(source.getBusinessPartner());
        aggregated.setDepartment(source.getDepartment());
        aggregated.setCurrency(source.getCurrency());
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
