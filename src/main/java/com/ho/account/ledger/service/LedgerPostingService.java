package com.ho.account.ledger.service;

import com.ho.account.basic.domain.FiscalPeriod;
import com.ho.account.basic.repository.FiscalPeriodRepository;
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.ledger.domain.GlBalance;
import com.ho.account.ledger.domain.GlEntry;
import com.ho.account.ledger.repository.GlBalanceRepository;
import com.ho.account.ledger.repository.GlEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class LedgerPostingService {

    @Autowired
    private GlBalanceRepository glBalanceRepository;

    @Autowired
    private GlEntryRepository glEntryRepository;

    @Autowired
    private FiscalPeriodRepository fiscalPeriodRepository;

    @Transactional
    public void postToLedger(JournalEntry entry) {
        LocalDate postingDate = entry.getAccountingDate();
        FiscalPeriod period = fiscalPeriodRepository.findByDate(postingDate)
                .orElseThrow(() -> new RuntimeException("Fiscal period not found for date: " + postingDate));

        for (JournalDetail detail : entry.getDetails()) {
            // 1. GL Entry 생성
            GlEntry glEntry = new GlEntry();
            glEntry.setJournalDetail(detail);
            glEntry.setAccount(detail.getAccountSubject());
            glEntry.setFiscalYear(period.getFiscalYear());
            glEntry.setFiscalPeriod(period.getFiscalPeriod());
            glEntry.setPostingDate(postingDate);
            glEntry.setCurrency(entry.getCurrency());
            glEntry.setExchangeRate(entry.getExchangeRate());

            if ("DEBIT".equals(detail.getDrcrType())) {
                glEntry.setDrAmount(detail.getAmount());
                glEntry.setBaseDrAmount(detail.getAmount()); // TODO: Multi-currency logic
            } else {
                glEntry.setCrAmount(detail.getAmount());
                glEntry.setBaseCrAmount(detail.getAmount());
            }

            glEntry.setLineageSourceType(entry.getLineageSourceType());
            glEntry.setLineageSourceId(entry.getLineageSourceId());
            glEntry.setAuditUser(entry.getAuditUser());
            glEntryRepository.save(glEntry);

            // 2. GL Balance 업데이트
            GlBalance balance = glBalanceRepository.findByAccountAndFiscalYearAndFiscalPeriodAndDepartmentAndCurrency(
                    detail.getAccountSubject(), period.getFiscalYear(), period.getFiscalPeriod(),
                    detail.getDepartment(), entry.getCurrency())
                    .orElseGet(() -> createNewBalance(detail, period, entry));

            if ("DEBIT".equals(detail.getDrcrType())) {
                balance.setCurrentPeriodDr(balance.getCurrentPeriodDr().add(detail.getAmount()));
            } else {
                balance.setCurrentPeriodCr(balance.getCurrentPeriodCr().add(detail.getAmount()));
            }
            balance.setAuditUser(entry.getAuditUser());
            glBalanceRepository.save(balance);
        }
    }

    private GlBalance createNewBalance(JournalDetail detail, FiscalPeriod period, JournalEntry entry) {
        GlBalance balance = new GlBalance();
        balance.setAccount(detail.getAccountSubject());
        balance.setFiscalYear(period.getFiscalYear());
        balance.setFiscalPeriod(period.getFiscalPeriod());
        balance.setDepartment(detail.getDepartment());
        balance.setCurrency(entry.getCurrency());

        // 전기 이월 잔액 가져오기 로직 (간소화됨)
        // TODO: 이전 기간 탐색 및 기초잔액 설정
        balance.setBeginBalanceDr(BigDecimal.ZERO);
        balance.setBeginBalanceCr(BigDecimal.ZERO);

        return balance;
    }
}
