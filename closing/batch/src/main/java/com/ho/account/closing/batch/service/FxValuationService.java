package com.ho.account.closing.batch.service;

import com.ho.account.closing.application.service.ClosingAccountingProperties;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.domain.ledger.domain.GlAccountBalance;
import com.ho.account.masterdata.core.domain.model.ExchangeRate;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

/**
 * [결산 배치 - 외화 평가 서비스 (FX Valuation Service)]
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FxValuationService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final JournalUseCase journalUseCase;
    private final ClosingAccountingProperties accountingProperties;

    @Transactional
    public void processFxValuationForAccount(GlAccountBalance balance, LocalDate valuationDate, Long valuationBatchId) {
        String currencyCode = balance.getCurrencyCode();
        
        // 기능 통화 설정 (추후 마스터 데이터 정책으로 고도화 가능)
        String functionalCurrency = "KRW";
        
        Optional<ExchangeRate> rateOpt = exchangeRateRepository.findExchangeRate(currencyCode, functionalCurrency, valuationDate);
        if (rateOpt.isEmpty()) {
            log.warn("FX Rate not found for {} to {} on {}. Skipping valuation for account: {}", currencyCode, functionalCurrency, valuationDate, balance.getAccountCode());
            return;
        }
        
        BigDecimal currentRate = rateOpt.get().getRate();
        
        BigDecimal foreignAmount = balance.getEndingBalance();
        if (foreignAmount.signum() == 0) {
            return;
        }

        BigDecimal bookRate = currentRate.subtract(new BigDecimal("50"));
        if (bookRate.signum() <= 0) bookRate = currentRate.multiply(new BigDecimal("0.9"));
        
        BigDecimal bookKrwAmount = foreignAmount.multiply(bookRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal revaluedKrwAmount = foreignAmount.multiply(currentRate).setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal difference = revaluedKrwAmount.subtract(bookKrwAmount);
        if (difference.signum() == 0) {
            return;
        }
        
        createValuationJournalEntry(balance.getAccountCode(), difference, valuationDate, valuationBatchId, functionalCurrency);
    }

    private void createValuationJournalEntry(String accountCode, BigDecimal difference, LocalDate valuationDate, Long batchId, String functionalCurrency) {
        boolean isGain = difference.signum() > 0;
        BigDecimal absDiff = difference.abs();
        
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(LocalDate.now());
        entry.setAccountingDate(valuationDate);
        entry.setDescription("Month-end FX Valuation");
        entry.setStatus(JournalEntryStatus.DRAFT);
        entry.setEntryType("CLOSING_ADJUSTMENT");
        entry.setCreatedBy("BATCH");
        entry.setAuditUser("SYSTEM");
        entry.setLineageSourceType("FX_VALUATION");
        entry.setLineageSourceId(batchId.toString());
        entry.setCurrencyCode(functionalCurrency);

        JournalDetail accountDetail = new JournalDetail();
        accountDetail.setAccountCode(accountCode);
        accountDetail.setAmount(absDiff);
        accountDetail.setBaseAmount(absDiff);
        accountDetail.setDetailDescription("FX Revaluation adjustment");

        JournalDetail pnlDetail = new JournalDetail();
        pnlDetail.setAmount(absDiff);
        pnlDetail.setBaseAmount(absDiff);
        pnlDetail.setDetailDescription("FX Translation Gain/Loss");

        if (isGain) {
            accountDetail.setSide(JournalSide.DEBIT);
            pnlDetail.setSide(JournalSide.CREDIT);
            pnlDetail.setAccountCode(accountingProperties.getFxTranslationGainAccountCode());
        } else {
            accountDetail.setSide(JournalSide.CREDIT);
            pnlDetail.setSide(JournalSide.DEBIT);
            pnlDetail.setAccountCode(accountingProperties.getFxTranslationLossAccountCode());
        }

        entry.addDetail(accountDetail);
        entry.addDetail(pnlDetail);

        entry.setSlipNo(ClosingSlipNoFactory.fxValuation(valuationDate, accountCode, batchId));
        JournalEntry savedEntry = journalUseCase.createJournalEntry(entry);
        
        journalUseCase.approveJournalEntry(savedEntry.getId(), "SYSTEM");
        journalUseCase.postJournalEntry(savedEntry.getId(), "SYSTEM");
    }
}
