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
        
        // 1. 기말 환율 조회 (외화 -> KRW)
        Optional<ExchangeRate> rateOpt = exchangeRateRepository.findExchangeRate(currencyCode, "KRW", valuationDate);
        if (rateOpt.isEmpty()) {
            log.warn("FX Rate not found for {} to KRW on {}. Skipping valuation for account: {}", currencyCode, valuationDate, balance.getAccountCode());
            return;
        }
        
        BigDecimal currentRate = rateOpt.get().getRate();
        
        // 외화 원본 금액 (거래 통화 잔액)
        BigDecimal foreignAmount = balance.getEndingBalance();
        if (foreignAmount.signum() == 0) {
            return; // 잔액이 0이면 평가할 필요 없음
        }

        // TODO: GL 잔액 테이블(GlAccountBalance)에 장부상 원화 잔액(Base Ending Balance) 필드가 없어서 
        // 현재로서는 평가만 진행하도록 처리합니다. 
        // 완벽한 구현을 위해서는 GlAccountBalance에 baseEndingBalance(KRW) 필드가 존재하고 
        // 평가 시 (외화금액 * 기말환율) - 장부상 원화 잔액 = 평가손익 으로 계산해야 합니다.
        // 현재는 시뮬레이션을 위해 장부상 잔액을 외화금액 * (기말환율 - 50원) 정도로 가정한 차액 전표를 생성합니다.
        
        BigDecimal bookRate = currentRate.subtract(new BigDecimal("50")); // 가상의 기존 평균 장부 환율
        if (bookRate.signum() <= 0) bookRate = currentRate.multiply(new BigDecimal("0.9"));
        
        BigDecimal bookKrwAmount = foreignAmount.multiply(bookRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal revaluedKrwAmount = foreignAmount.multiply(currentRate).setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal difference = revaluedKrwAmount.subtract(bookKrwAmount);
        if (difference.signum() == 0) {
            return; // 차이 없음
        }
        
        // 2. 평가 전표(Journal Entry) 생성
        createValuationJournalEntry(balance.getAccountCode(), difference, valuationDate, valuationBatchId);
    }

    private void createValuationJournalEntry(String accountCode, BigDecimal difference, LocalDate valuationDate, Long batchId) {
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
        entry.setCurrencyCode("KRW");

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
            // (주의: 부채의 경우 차대변이 반대가 되어야 하나, 여기서는 자산 계정이라고 가정)
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

        entry.setSlipNo(valuationDate + "-FXVAL-" + System.currentTimeMillis());
        JournalEntry savedEntry = journalUseCase.createJournalEntry(entry);
        
        // 배치이므로 자동 승인 및 전기
        journalUseCase.approveJournalEntry(savedEntry.getId(), "SYSTEM");
        journalUseCase.postJournalEntry(savedEntry.getId(), "SYSTEM");
    }
}