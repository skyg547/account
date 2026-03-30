package com.ho.account.ledger.service;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Department;
import com.ho.account.basic.domain.Currency; // Added missing import for Currency
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.repository.JournalDetailRepository;
import com.ho.account.ledger.domain.GlBalance;
import com.ho.account.ledger.domain.SlBalance;
import com.ho.account.ledger.repository.GlBalanceRepository;
import com.ho.account.ledger.repository.SlBalanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors; // Added missing import for Collectors

/**
 * GL(총계정원장) 및 SL(보조원장) 잔액을 관리하는 서비스
 * 전표가 POSTED 상태가 될 때 GL/SL 잔액을 업데이트하는 로직을 포함합니다.
 */
@Service
public class LedgerService {

    private final GlBalanceRepository glBalanceRepository;
    private final SlBalanceRepository slBalanceRepository;
    private final JournalDetailRepository journalDetailRepository; // Added

    public LedgerService(GlBalanceRepository glBalanceRepository, SlBalanceRepository slBalanceRepository, JournalDetailRepository journalDetailRepository) {
        this.glBalanceRepository = glBalanceRepository;
        this.slBalanceRepository = slBalanceRepository;
        this.journalDetailRepository = journalDetailRepository; // Added
    }

    /**
     * 저널 상세 정보를 바탕으로 GL 및 SL 잔액을 업데이트합니다.
     * @param journalDetail 업데이트할 저널 상세 정보
     * @param accountingDate 회계 일자
     */
    @Transactional
    public void updateLedgerBalances(JournalDetail journalDetail, LocalDate accountingDate) {
        AccountSubject accountSubject = journalDetail.getAccountSubject();
        BusinessPartner businessPartner = journalDetail.getBusinessPartner();
        Department department = journalDetail.getDepartment();
        com.ho.account.basic.domain.Currency currency = journalDetail.getJournalEntry().getCurrency();
        BigDecimal amount = journalDetail.getBaseAmount();
        boolean isDebit = "DEBIT".equals(journalDetail.getDrcrType());

        updateGlBalance(accountSubject, currency, amount, isDebit, accountingDate);
        updateSlBalance(accountSubject, businessPartner, department, currency, amount, isDebit, accountingDate);
    }

    private void updateGlBalance(AccountSubject accountSubject, Currency currency, BigDecimal amount, boolean isDebit, LocalDate accountingDate) {
        YearMonth period = YearMonth.from(accountingDate);

        Optional<GlBalance> optionalGlBalance = glBalanceRepository.findByAccountSubjectAndCurrencyAndBalanceDateAndPeriod(
                accountSubject, currency, accountingDate, period);

        GlBalance glBalance = optionalGlBalance.orElseGet(() -> {
            GlBalance newGlBalance = new GlBalance();
            newGlBalance.setAccountSubject(accountSubject);
            newGlBalance.setCurrency(currency);
            newGlBalance.setBalanceDate(accountingDate);
            newGlBalance.setPeriod(period);
            return newGlBalance;
        });

        if (isDebit) {
            glBalance.setDebitAmount(glBalance.getDebitAmount().add(amount));
        } else {
            glBalance.setCreditAmount(glBalance.getCreditAmount().add(amount));
        }
        glBalance.setEndingBalance(glBalance.getBeginningBalance().add(glBalance.getDebitAmount()).subtract(glBalance.getCreditAmount()));

        glBalanceRepository.save(glBalance);
    }

    private void updateSlBalance(AccountSubject accountSubject, BusinessPartner businessPartner, Department department, Currency currency, BigDecimal amount, boolean isDebit, LocalDate accountingDate) {
        YearMonth period = YearMonth.from(accountingDate);

        Optional<SlBalance> optionalSlBalance = slBalanceRepository.findByAccountSubjectAndBusinessPartnerAndDepartmentAndCurrencyAndBalanceDateAndPeriod(
                accountSubject, businessPartner, department, currency, accountingDate, period);

        SlBalance slBalance = optionalSlBalance.orElseGet(() -> {
            SlBalance newSlBalance = new SlBalance();
            newSlBalance.setAccountSubject(accountSubject);
            newSlBalance.setBusinessPartner(businessPartner);
            newSlBalance.setDepartment(department);
            newSlBalance.setCurrency(currency);
            newSlBalance.setBalanceDate(accountingDate);
            newSlBalance.setPeriod(period);
            return newSlBalance;
        });

        if (isDebit) {
            slBalance.setDebitAmount(slBalance.getDebitAmount().add(amount));
        } else {
            slBalance.setCreditAmount(slBalance.getCreditAmount().add(amount));
        }
        slBalance.setEndingBalance(slBalance.getBeginningBalance().add(slBalance.getDebitAmount()).subtract(slBalance.getCreditAmount()));

        slBalanceRepository.save(slBalance);
    }

    /**
     * 특정 기간(날짜 범위)의 GL 및 SL 잔액을 재집계합니다 (마감 배치 정책).
     * 기존 잔액을 삭제하고, 해당 기간 내 전기(POSTED)된 모든 전표 상세 내역을 바탕으로 잔액을 재계산합니다.
     * @param startDate 재집계 시작일
     * @param endDate 재집계 종료일
     */
    @Transactional
    public void reaggregateLedgerBalancesForPeriod(LocalDate startDate, LocalDate endDate) {
        // 1. 해당 기간의 기존 GL 및 SL 잔액 삭제
        glBalanceRepository.deleteAllInBatch(glBalanceRepository.findByBalanceDateBetween(startDate, endDate));
        slBalanceRepository.deleteAllInBatch(slBalanceRepository.findByBalanceDateBetween(startDate, endDate));

        // 2. 해당 기간 내 전기(POSTED)된 모든 전표 상세 내역 조회
        List<JournalDetail> postedJournalDetails = journalDetailRepository.findPostedJournalDetailsByAccountingDateBetween(startDate, endDate);

        // 3. 각 전표 상세 내역을 바탕으로 GL 및 SL 잔액 재계산
        for (JournalDetail detail : postedJournalDetails) {
            updateLedgerBalances(detail, detail.getJournalEntry().getAccountingDate());
        }
    }

    /**
     * 특정 기간 동안의 GL 잔액을 조회합니다.
     * @param startDate 조회 시작일
     * @param endDate 조회 종료일
     * @param accountSubject 계정과목 (필터링 조건, null 허용)
     * @param currency 통화 (필터링 조건, null 허용)
     * @return GL 잔액 목록
     */
    @Transactional(readOnly = true)
    public List<GlBalance> getGlBalances(LocalDate startDate, LocalDate endDate, AccountSubject accountSubject, com.ho.account.basic.domain.Currency currency) {
        if (accountSubject != null && currency != null) {
            return glBalanceRepository.findByBalanceDateBetweenAndAccountSubjectAndCurrency(startDate, endDate, accountSubject, currency);
        } else if (accountSubject != null) {
            // Need a new query method for accountSubject only, or filter in memory
            // For now, let's assume direct queries for specific combinations.
            // A more robust solution would use Specification or Querydsl.
            return glBalanceRepository.findByBalanceDateBetween(startDate, endDate).stream()
                    .filter(b -> b.getAccountSubject().equals(accountSubject))
                    .collect(Collectors.toList());
        } else if (currency != null) {
            // Need a new query method for currency only
            return glBalanceRepository.findByBalanceDateBetween(startDate, endDate).stream()
                    .filter(b -> b.getCurrency().equals(currency))
                    .collect(Collectors.toList());
        } else {
            return glBalanceRepository.findByBalanceDateBetween(startDate, endDate);
        }
    }

    /**
     * 특정 기간 동안의 SL 잔액을 조회합니다.
     * @param startDate 조회 시작일
     * @param endDate 조회 종료일
     * @param accountSubject 계정과목 (필터링 조건, null 허용)
     * @param businessPartner 거래처 (필터링 조건, null 허용)
     * @param department 부서 (필터링 조건, null 허용)
     * @param currency 통화 (필터링 조건, null 허용)
     * @return SL 잔액 목록
     */
    @Transactional(readOnly = true)
    public List<SlBalance> getSlBalances(LocalDate startDate, LocalDate endDate,
                                         AccountSubject accountSubject, BusinessPartner businessPartner,
                                         Department department, com.ho.account.basic.domain.Currency currency) {
        // This method needs more robust filtering logic using Specifications or Querydsl for optimal performance
        // For simplicity, using in-memory filtering for optional parameters where direct JPA derivation is complex
        List<SlBalance> results = slBalanceRepository.findByBalanceDateBetween(startDate, endDate);

        if (accountSubject != null) {
            results = results.stream().filter(b -> accountSubject.equals(b.getAccountSubject())).collect(Collectors.toList());
        }
        if (businessPartner != null) {
            results = results.stream().filter(b -> businessPartner.equals(b.getBusinessPartner())).collect(Collectors.toList());
        }
        if (department != null) {
            results = results.stream().filter(b -> department.equals(b.getDepartment())).collect(Collectors.toList());
        }
        if (currency != null) {
            results = results.stream().filter(b -> currency.equals(b.getCurrency())).collect(Collectors.toList());
        }

        return results;
    }
}
