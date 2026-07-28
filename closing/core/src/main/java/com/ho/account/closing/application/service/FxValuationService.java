package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.out.ClosingJournalEntryCommand;
import com.ho.account.closing.application.port.out.ClosingJournalEntryPort;
import com.ho.account.closing.application.port.out.ClosingJournalEntryResult;
import com.ho.account.closing.application.port.out.ClosingJournalLineCommand;
import com.ho.account.closing.application.port.out.ClosingJournalSide;
import com.ho.account.closing.application.port.out.FxExchangeRateLookupPort;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * [결산 Core - 외화 평가 서비스 (FX Valuation Service)]
 *
 * <p>초보자 설명: 이 서비스는 월말 결산 시점에 외화 잔액의 현재 보고통화 가치를 다시 계산합니다.
 * 예를 들어 100달러를 장부에 10만원으로 들고 있는데 기말 환율로 13만원이 되었다면,
 * 차이 3만원을 외화환산이익 또는 외화환산손실 전표로 남깁니다.</p>
 *
 * <p>헥사고날 기준: 환율 조회, 전표 생성, 계정과목 조회는 모두 포트로 호출합니다.
 * Spring Batch는 이 서비스를 호출하는 어댑터일 뿐이고, 차대변 판단과 금액 산출 순서는 core가 소유합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FxValuationService {

    private static final String SYSTEM_ACTOR = "SYSTEM";
    private static final String BATCH_ACTOR = "BATCH";

    private final FxExchangeRateLookupPort fxExchangeRateLookupPort;
    private final ClosingJournalEntryPort closingJournalEntryPort;
    private final ClosingAccountingProperties accountingProperties;
    private final MasterDataQueryPort masterDataQueryPort;

    @Transactional
    public void processFxValuationForAccount(FxValuationBalance balance, LocalDate valuationDate, Long valuationBatchId) {
        String reportingCurrencyCode = accountingProperties.requireFxValuationReportingCurrencyCode();
        if (balance.currencyCode().equalsIgnoreCase(reportingCurrencyCode)) {
            return;
        }

        Optional<BigDecimal> rateOpt = fxExchangeRateLookupPort.findRate(
                balance.currencyCode(),
                reportingCurrencyCode,
                valuationDate);
        if (rateOpt.isEmpty()) {
            log.warn("FX Rate not found for {} to {} on {}. Skipping valuation for account: {}",
                    balance.currencyCode(), reportingCurrencyCode, valuationDate, balance.accountCode());
            return;
        }

        BigDecimal foreignAmount = balance.foreignEndingBalance();
        if (foreignAmount.signum() == 0) {
            return;
        }

        BigDecimal bookReportingAmount = balance.bookReportingAmount();
        if (bookReportingAmount == null) {
            log.warn("Base ending balance is missing. Skipping FX valuation for account: {}, currency: {}, date: {}",
                    balance.accountCode(), balance.currencyCode(), valuationDate);
            return;
        }

        BigDecimal revaluedReportingAmount = foreignAmount.multiply(rateOpt.get()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal difference = revaluedReportingAmount.subtract(bookReportingAmount);
        if (difference.signum() == 0) {
            return;
        }

        AccountSubjectRef accountSubject = masterDataQueryPort.findAccountSubjectAt(balance.accountCode(), valuationDate)
                .orElse(null);
        if (accountSubject == null) {
            log.warn("Account subject is missing. Skipping FX valuation for account: {}, date: {}",
                    balance.accountCode(), valuationDate);
            return;
        }

        createValuationJournalEntry(accountSubject, difference, valuationDate, valuationBatchId, reportingCurrencyCode);
    }

    private void createValuationJournalEntry(AccountSubjectRef accountSubject,
                                             BigDecimal difference,
                                             LocalDate valuationDate,
                                             Long batchId,
                                             String reportingCurrencyCode) {
        boolean debitNormalBalance = accountSubject.debitNormalBalance();
        boolean isGain = debitNormalBalance ? difference.signum() > 0 : difference.signum() < 0;
        BigDecimal absDiff = difference.abs();

        ClosingJournalSide accountSide = difference.signum() > 0
                ? normalBalanceSide(debitNormalBalance)
                : oppositeNormalBalanceSide(debitNormalBalance);
        ClosingJournalSide pnlSide = isGain ? ClosingJournalSide.CREDIT : ClosingJournalSide.DEBIT;
        String pnlAccountCode = isGain
                ? accountingProperties.getFxTranslationGainAccountCode()
                : accountingProperties.getFxTranslationLossAccountCode();

        ClosingJournalEntryCommand command = new ClosingJournalEntryCommand(
                LocalDate.now(),
                valuationDate,
                "Month-end FX Valuation",
                "CLOSING_ADJUSTMENT",
                BATCH_ACTOR,
                SYSTEM_ACTOR,
                "FX_VALUATION",
                batchId.toString(),
                reportingCurrencyCode,
                ClosingSlipNoFactory.fxValuation(valuationDate, accountSubject.code(), batchId),
                List.of(
                        new ClosingJournalLineCommand(
                                accountSide,
                                accountSubject.code(),
                                absDiff,
                                absDiff,
                                "FX Revaluation adjustment"),
                        new ClosingJournalLineCommand(
                                pnlSide,
                                pnlAccountCode,
                                absDiff,
                                absDiff,
                                "FX Translation Gain/Loss")));

        ClosingJournalEntryResult result = closingJournalEntryPort.createDraftAdjustment(command);
        if (accountingProperties.isAutoPostAdjustments()) {
            closingJournalEntryPort.approveAndPost(result.journalEntryId(), SYSTEM_ACTOR);
        }
    }

    private ClosingJournalSide normalBalanceSide(boolean debitNormalBalance) {
        return debitNormalBalance ? ClosingJournalSide.DEBIT : ClosingJournalSide.CREDIT;
    }

    private ClosingJournalSide oppositeNormalBalanceSide(boolean debitNormalBalance) {
        return debitNormalBalance ? ClosingJournalSide.CREDIT : ClosingJournalSide.DEBIT;
    }
}