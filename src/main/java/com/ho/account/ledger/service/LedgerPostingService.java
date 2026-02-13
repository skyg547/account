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

/**
 * 원장 전기 서비스(Ledger Posting Service)
 * 확정된 전표를 총계정원장(GL)에 반영하며, 개별 거래 내역(GlEntry) 생성 및 계정 잔액(GlBalance)을 업데이트함.
 */
@Service
public class LedgerPostingService {

    @Autowired
    private GlBalanceRepository glBalanceRepository;

    @Autowired
    private GlEntryRepository glEntryRepository;

    @Autowired
    private FiscalPeriodRepository fiscalPeriodRepository;

    /**
     * 전표를 총계정원장에 전기(Post)함.
     * 
     * @param journalEntry 전기할 분개 전표
     */
    @Transactional
    public void postToLedger(JournalEntry journalEntry) {
        LocalDate postingDate = journalEntry.getAccountingDate();
        FiscalPeriod period = fiscalPeriodRepository.findByDate(postingDate)
                .orElseThrow(() -> new RuntimeException("Fiscal period not found for date: " + postingDate));

        for (JournalDetail detail : journalEntry.getDetails()) {
            // 1. GL Entry 생성
            GlEntry glEntry = new GlEntry();
            glEntry.setJournalDetail(detail);
            glEntry.setAccount(detail.getAccountSubject());
            glEntry.setFiscalYear(period.getFiscalYear());
            glEntry.setFiscalPeriod(period.getFiscalPeriod());
            glEntry.setPostingDate(postingDate);
            glEntry.setCurrency(journalEntry.getCurrency());
            glEntry.setExchangeRate(journalEntry.getExchangeRate());

            if ("DEBIT".equals(detail.getDrcrType())) {
                glEntry.setDrAmount(detail.getAmount());
                glEntry.setBaseDrAmount(detail.getBaseAmount());
                glEntry.setCrAmount(BigDecimal.ZERO);
                glEntry.setBaseCrAmount(BigDecimal.ZERO);
            } else {
                glEntry.setCrAmount(detail.getAmount());
                glEntry.setBaseCrAmount(detail.getBaseAmount());
                glEntry.setDrAmount(BigDecimal.ZERO);
                glEntry.setBaseDrAmount(BigDecimal.ZERO);
            }

            glEntry.setLineageSourceType(journalEntry.getLineageSourceType());
            glEntry.setLineageSourceId(journalEntry.getLineageSourceId());
            glEntry.setAuditUser(journalEntry.getAuditUser());
            glEntryRepository.save(glEntry);

            // 2. GL Balance 업데이트
            GlBalance balance = glBalanceRepository.findByAccountAndFiscalYearAndFiscalPeriodAndDepartmentAndCurrency(
                    detail.getAccountSubject(), period.getFiscalYear(), period.getFiscalPeriod(),
                    detail.getDepartment(), journalEntry.getCurrency())
                    .orElseGet(() -> createNewBalance(detail, period, journalEntry));

            if ("DEBIT".equals(detail.getDrcrType())) {
                balance.setCurrentPeriodDr(balance.getCurrentPeriodDr().add(detail.getAmount()));
            } else {
                balance.setCurrentPeriodCr(balance.getCurrentPeriodCr().add(detail.getAmount()));
            }
            balance.setAuditUser(journalEntry.getAuditUser());
            glBalanceRepository.save(balance);
        }
    }

    private GlBalance createNewBalance(JournalDetail detail, FiscalPeriod period, JournalEntry journalEntry) {
        GlBalance balance = new GlBalance();
        balance.setAccount(detail.getAccountSubject());
        balance.setFiscalYear(period.getFiscalYear());
        balance.setFiscalPeriod(period.getFiscalPeriod());
        balance.setDepartment(detail.getDepartment());
        balance.setCurrency(journalEntry.getCurrency());

        // 전기 이월 잔액 가져오기 로직 (간소화됨)
        // TODO: 이전 기간 탐색 및 기초잔액 설정
        balance.setBeginBalanceDr(BigDecimal.ZERO);
        balance.setBeginBalanceCr(BigDecimal.ZERO);

        return balance;
    }
}
