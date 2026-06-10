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
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 매월 말 결산 시점에 '우리가 가진 외화(달러 등)의 현재 원화 가치가 얼마인지'를 평가합니다.
 * 
 * 예를 들어:
 * 1. 은행에 100 달러가 있고, 당시에 1달러=1000원(총 10만원)에 샀다고 가정합니다.
 * 2. 기말에 환율이 1달러=1300원으로 올랐습니다.
 * 3. 그럼 100달러는 이제 13만원의 가치를 지닙니다!
 * 4. 이 서비스는 기존 장부금액(10만원)과 평가금액(13만원)의 차이인 3만원을
 *    "외화환산이익(외화평가이익)"으로 회계 전표에 자동으로 끊어줍니다.
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
        String reportingCurrencyCode = accountingProperties.requireFxValuationReportingCurrencyCode();
        if (currencyCode.equalsIgnoreCase(reportingCurrencyCode)) {
            return;
        }
        
        // 1. 기말 환율 조회 (외화 -> 보고통화)
        Optional<ExchangeRate> rateOpt = exchangeRateRepository.findExchangeRate(currencyCode, reportingCurrencyCode, valuationDate);
        if (rateOpt.isEmpty()) {
            log.warn("FX Rate not found for {} to {} on {}. Skipping valuation for account: {}",
                    currencyCode, reportingCurrencyCode, valuationDate, balance.getAccountCode());
            return;
        }
        
        BigDecimal currentRate = rateOpt.get().getRate();
        
        // 외화 원본 금액 (거래 통화 잔액)
        BigDecimal foreignAmount = balance.getEndingBalance();
        if (foreignAmount.signum() == 0) {
            return; // 잔액이 0이면 평가할 필요 없음
        }

        BigDecimal bookReportingAmount = balance.getBaseEndingBalance();
        if (bookReportingAmount == null) {
            log.warn("Base ending balance is missing. Skipping FX valuation for account: {}, currency: {}, date: {}",
                    balance.getAccountCode(), currencyCode, valuationDate);
            return;
        }
        
        BigDecimal revaluedReportingAmount = foreignAmount.multiply(currentRate).setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal difference = revaluedReportingAmount.subtract(bookReportingAmount);
        if (difference.signum() == 0) {
            return; // 차이 없음
        }
        
        // 2. 평가 전표(Journal Entry) 생성
        createValuationJournalEntry(balance.getAccountCode(), difference, valuationDate, valuationBatchId, reportingCurrencyCode);
    }

    private void createValuationJournalEntry(String accountCode, BigDecimal difference, LocalDate valuationDate, Long batchId,
                                             String reportingCurrencyCode) {
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
        entry.setCurrencyCode(reportingCurrencyCode);

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
            // 이익: 차변(자산/부채 계정) / 대변(외화환산이익)
            // 현재는 자산 계정을 기준으로 처리합니다.
            // @todo 계정 성격(자산/부채)을 master-data 계정 속성으로 조회해 부채 계정의 차대변 반전까지 반영해야 합니다.
            accountDetail.setSide(JournalSide.DEBIT);
            
            pnlDetail.setSide(JournalSide.CREDIT);
            pnlDetail.setAccountCode(accountingProperties.getFxTranslationGainAccountCode());
        } else {
            // 손실: 차변(외화환산손실) / 대변(자산/부채 계정)
            accountDetail.setSide(JournalSide.CREDIT);
            
            pnlDetail.setSide(JournalSide.DEBIT);
            pnlDetail.setAccountCode(accountingProperties.getFxTranslationLossAccountCode());
        }

        entry.addDetail(accountDetail);
        entry.addDetail(pnlDetail);

        entry.setSlipNo(ClosingSlipNoFactory.fxValuation(valuationDate, accountCode, batchId));
        JournalEntry savedEntry = journalUseCase.createJournalEntry(entry);
        
        // 결산 배치는 전표 초안 생성까지만 수행하는 것이 기본입니다. 별도 운영 통제가 자동 전기를
        // 허용한 환경에서만 승인/전기하여, 오류 시 검토와 역분개 절차를 거칠 수 있게 합니다.
        if (accountingProperties.isAutoPostAdjustments()) {
            journalUseCase.approveJournalEntry(savedEntry.getId(), "SYSTEM");
            journalUseCase.postJournalEntry(savedEntry.getId(), "SYSTEM");
        }
    }
}
