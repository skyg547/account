package com.ho.account.closing.batch.service;

import com.ho.account.closing.application.service.ClosingAccountingProperties;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
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
    private final MasterDataQueryPort masterDataQueryPort;

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
        
        AccountSubjectRef accountSubject = masterDataQueryPort.findAccountSubjectAt(balance.getAccountCode(), valuationDate)
                .orElse(null);
        if (accountSubject == null) {
            log.warn("Account subject is missing. Skipping FX valuation for account: {}, date: {}",
                    balance.getAccountCode(), valuationDate);
            return;
        }

        // 2. 평가 전표(Journal Entry) 생성
        createValuationJournalEntry(accountSubject, difference, valuationDate, valuationBatchId, reportingCurrencyCode);
    }

    private void createValuationJournalEntry(AccountSubjectRef accountSubject, BigDecimal difference, LocalDate valuationDate, Long batchId,
                                             String reportingCurrencyCode) {
        boolean debitNormalBalance = accountSubject.debitNormalBalance();
        boolean isGain = debitNormalBalance ? difference.signum() > 0 : difference.signum() < 0;
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
        accountDetail.setAccountCode(accountSubject.code());
        accountDetail.setAmount(absDiff);
        accountDetail.setBaseAmount(absDiff);
        accountDetail.setDetailDescription("FX Revaluation adjustment");

        JournalDetail pnlDetail = new JournalDetail();
        pnlDetail.setAmount(absDiff);
        pnlDetail.setBaseAmount(absDiff);
        pnlDetail.setDetailDescription("FX Translation Gain/Loss");

        if (difference.signum() > 0) {
            // 장부가를 늘리는 평가입니다. 차변 정상 계정(자산 등)은 차변, 대변 정상 계정(부채 등)은 대변으로 늘립니다.
            accountDetail.setSide(debitNormalBalance ? JournalSide.DEBIT : JournalSide.CREDIT);
        } else {
            // 장부가를 줄이는 평가입니다. 정상잔액 방향의 반대편에 계정 라인을 기록합니다.
            accountDetail.setSide(debitNormalBalance ? JournalSide.CREDIT : JournalSide.DEBIT);
        }

        if (isGain) {
            pnlDetail.setSide(JournalSide.CREDIT);
            pnlDetail.setAccountCode(accountingProperties.getFxTranslationGainAccountCode());
        } else {
            pnlDetail.setSide(JournalSide.DEBIT);
            pnlDetail.setAccountCode(accountingProperties.getFxTranslationLossAccountCode());
        }

        entry.addDetail(accountDetail);
        entry.addDetail(pnlDetail);

        entry.setSlipNo(ClosingSlipNoFactory.fxValuation(valuationDate, accountSubject.code(), batchId));
        JournalEntry savedEntry = journalUseCase.createJournalEntry(entry);
        
        // 결산 배치는 전표 초안 생성까지만 수행하는 것이 기본입니다. 별도 운영 통제가 자동 전기를
        // 허용한 환경에서만 승인/전기하여, 오류 시 검토와 역분개 절차를 거칠 수 있게 합니다.
        if (accountingProperties.isAutoPostAdjustments()) {
            journalUseCase.approveJournalEntry(savedEntry.getId(), "SYSTEM");
            journalUseCase.postJournalEntry(savedEntry.getId(), "SYSTEM");
        }
    }
}
