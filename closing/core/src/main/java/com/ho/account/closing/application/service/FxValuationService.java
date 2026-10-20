package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.out.ClosingJournalEntryCommand;
import com.ho.account.closing.application.port.out.ClosingJournalEntryPort;
import com.ho.account.closing.application.port.out.ClosingJournalEntryResult;
import com.ho.account.closing.application.port.out.ClosingJournalLineCommand;
import com.ho.account.closing.application.port.out.ClosingJournalSide;
import com.ho.account.closing.application.port.out.FxExchangeRateLookupPort;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
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
@RequiredArgsConstructor
public class FxValuationService {

    private static final String SYSTEM_ACTOR = "SYSTEM";
    private static final String BATCH_ACTOR = "BATCH";

    private final FxExchangeRateLookupPort fxExchangeRateLookupPort;
    private final ClosingJournalEntryPort closingJournalEntryPort;
    private final ClosingAccountingProperties accountingProperties;
    private final FxValuationEligibilityResolver eligibilityResolver;

    public void processFxValuationForAccount(FxValuationBalance balance, LocalDate valuationDate, Long valuationBatchId) {
        prepareFxValuationForAccount(balance, valuationDate, valuationBatchId)
                .ifPresent(this::postPreparedFxValuation);
    }

    /**
     * Validates one posted-ledger balance and prepares its deterministic journal without writing it.
     * This separation lets API and Batch validate a complete unit of work before the first remote
     * journal effect occurs.
     */
    public Optional<ClosingJournalEntryCommand> prepareFxValuationForAccount(
            FxValuationBalance balance,
            LocalDate valuationDate,
            Long valuationBatchId) {
        Objects.requireNonNull(balance, "balance must not be null");
        Objects.requireNonNull(valuationDate, "valuationDate must not be null");
        if (valuationBatchId == null || valuationBatchId <= 0) {
            throw new IllegalArgumentException("valuationBatchId must be positive");
        }
        String reportingCurrencyCode = accountingProperties.requireFxValuationReportingCurrencyCode();
        if (balance.currencyCode().equalsIgnoreCase(reportingCurrencyCode)) {
            return Optional.empty();
        }

        BigDecimal foreignAmount = balance.foreignEndingBalance();
        BigDecimal bookReportingAmount = balance.bookReportingAmount();
        if (bookReportingAmount == null) {
            throw new IllegalStateException("Base ending balance is missing for account "
                    + balance.accountCode() + ", currency " + balance.currencyCode());
        }
        // Direct callers must not bypass the dated gate, even for zero balances or unchanged rates.
        if (!eligibilityResolver.isEligible(balance.accountCode(), valuationDate)) {
            return Optional.empty();
        }
        if (foreignAmount.signum() == 0 && bookReportingAmount.signum() == 0) {
            return Optional.empty();
        }
        if (foreignAmount.signum() == 0 || bookReportingAmount.signum() == 0) {
            throw new IllegalStateException("FX transaction and reporting balances are inconsistent for account "
                    + balance.accountCode() + ", currency " + balance.currencyCode());
        }

        BigDecimal rate = fxExchangeRateLookupPort.findRate(
                balance.currencyCode(),
                reportingCurrencyCode,
                valuationDate)
                .orElseThrow(() -> new IllegalStateException(
                        "FX rate not found for " + balance.currencyCode() + " to "
                                + reportingCurrencyCode + " on " + valuationDate));
        if (rate.signum() <= 0) {
            throw new IllegalStateException("FX rate must be positive for " + balance.currencyCode()
                    + " to " + reportingCurrencyCode + " on " + valuationDate);
        }

        BigDecimal revaluedReportingAmount = foreignAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal difference = revaluedReportingAmount.subtract(bookReportingAmount);
        if (difference.signum() == 0) {
            return Optional.empty();
        }

        return Optional.of(createValuationJournalEntry(
                balance.accountCode(),
                balance.currencyCode(),
                difference,
                valuationDate,
                valuationBatchId,
                reportingCurrencyCode));
    }

    /** Creates the prepared draft and applies the explicitly configured auto-post policy. */
    public ClosingJournalEntryResult postPreparedFxValuation(ClosingJournalEntryCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ClosingJournalEntryResult result = closingJournalEntryPort.createDraftAdjustment(command);
        if (accountingProperties.isAutoPostAdjustments() && !"POSTED".equals(result.status())) {
            closingJournalEntryPort.approveAndPost(result.journalEntryId(), SYSTEM_ACTOR);
        }
        return result;
    }

    private ClosingJournalEntryCommand createValuationJournalEntry(String accountCode,
                                                                    String sourceCurrencyCode,
                                                                    BigDecimal difference,
                                                                    LocalDate valuationDate,
                                                                    Long batchId,
                                                                    String reportingCurrencyCode) {
        BigDecimal absDiff = difference.abs();

        ClosingJournalSide accountSide = difference.signum() > 0
                ? ClosingJournalSide.DEBIT
                : ClosingJournalSide.CREDIT;
        ClosingJournalSide pnlSide = accountSide == ClosingJournalSide.DEBIT
                ? ClosingJournalSide.CREDIT
                : ClosingJournalSide.DEBIT;
        String pnlAccountCode = pnlSide == ClosingJournalSide.CREDIT
                ? accountingProperties.getFxTranslationGainAccountCode()
                : accountingProperties.getFxTranslationLossAccountCode();

        return new ClosingJournalEntryCommand(
                valuationDate,
                valuationDate,
                "Month-end FX Valuation",
                "CLOSING_ADJUSTMENT",
                BATCH_ACTOR,
                SYSTEM_ACTOR,
                "FX_VALUATION",
                batchId + "|" + accountCode + "|" + sourceCurrencyCode,
                reportingCurrencyCode,
                ClosingSlipNoFactory.fxValuation(
                        valuationDate,
                        accountCode + "|" + sourceCurrencyCode,
                        batchId),
                List.of(
                        new ClosingJournalLineCommand(
                                accountSide,
                                accountCode,
                                absDiff,
                                absDiff,
                                "FX Revaluation adjustment"),
                        new ClosingJournalLineCommand(
                                pnlSide,
                                pnlAccountCode,
                                absDiff,
                                absDiff,
                                "FX Translation Gain/Loss")));
    }
}
