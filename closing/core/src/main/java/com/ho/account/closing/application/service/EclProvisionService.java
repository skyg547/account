package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.out.AllowanceBalanceLookupPort;
import com.ho.account.closing.application.port.out.ClosingJournalEntryCommand;
import com.ho.account.closing.application.port.out.ClosingJournalEntryPort;
import com.ho.account.closing.application.port.out.ClosingJournalEntryResult;
import com.ho.account.closing.application.port.out.ClosingJournalLineCommand;
import com.ho.account.closing.application.port.out.ClosingJournalSide;
import com.ho.account.closing.application.port.out.EclAllowanceResultPort;
import com.ho.account.closing.domain.EclAllowanceSummary;
import com.ho.account.closing.domain.ProvisionBatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * [결산 Core - IFRS9 기대신용손실(ECL) 기반 대손충당금 서비스]
 *
 * <p>초보자 설명: ECL 엔진이 목표 대손충당금을 확정하면 closing은 기존 GL 충당금 잔액과 비교합니다.
 * 목표가 1억원이고 이미 8천만원이 쌓여 있다면 차이 2천만원만 추가 비용/충당금 전표로 남깁니다.
 * 반대로 목표가 기존 잔액보다 작으면 대손충당금을 줄이고 환입 수익을 인식합니다.</p>
 *
 * <p>헥사고날 기준: ECL 산출값 조회, 기존 GL 잔액 조회, 전표 생성은 포트로 분리합니다.
 * Stage/PD/LGD/EAD 재계산은 이 서비스의 책임이 아니며, 확정된 allowance_summary만 소비합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EclProvisionService {

    private static final String SYSTEM_ACTOR = "SYSTEM";
    private static final String BATCH_ACTOR = "BATCH";

    private final AllowanceBalanceLookupPort allowanceBalanceLookupPort;
    private final ClosingJournalEntryPort closingJournalEntryPort;
    private final ClosingAccountingProperties accountingProperties;
    private final EclAllowanceResultPort eclAllowanceResultPort;

    @Transactional
    public void processEclProvision(LocalDate closingDate, Long provisionBatchId) {
        log.info("Starting ECL Provision calculation for closing date: {}", closingDate);

        List<EclAllowanceSummary> summaries = eclAllowanceResultPort.loadSummaries(closingDate);
        if (summaries.isEmpty()) {
            log.warn("No finalized ECL allowance summary found for {}. No provision journal will be created.", closingDate);
            return;
        }

        ClosingAccountingProperties.AutomatedJournalRule eclRule =
                accountingProperties.requireProvisionRule(ProvisionBatch.ProvisionType.ECL);

        for (EclAllowanceSummary summary : summaries) {
            processSummary(summary, closingDate, provisionBatchId, eclRule);
        }
    }

    private void processSummary(EclAllowanceSummary summary,
                                LocalDate closingDate,
                                Long provisionBatchId,
                                ClosingAccountingProperties.AutomatedJournalRule eclRule) {
        String badDebtExpenseAccount = resolveRequiredAccount(
                summary.badDebtExpenseAccountCode(),
                eclRule.getDebitAccountCode(),
                "bad debt expense account");
        String allowanceForDoubtfulAccounts = resolveRequiredAccount(
                summary.allowanceAccountCode(),
                eclRule.getCreditAccountCode(),
                "allowance account");
        String currencyCode = requireText(summary.currencyCode(), "currencyCode");

        BigDecimal existingAllowance = allowanceBalanceLookupPort.findCreditEndingBalance(
                allowanceForDoubtfulAccounts,
                currencyCode,
                closingDate);
        BigDecimal difference = summary.targetAllowanceAmount().subtract(existingAllowance);

        if (difference.signum() == 0) {
            log.info("ECL provision target equals existing allowance. No journal required. summary={}",
                    summary.lineageSourceId(provisionBatchId));
            return;
        }

        createProvisionJournalEntry(
                difference,
                closingDate,
                provisionBatchId,
                badDebtExpenseAccount,
                allowanceForDoubtfulAccounts,
                currencyCode,
                summary);
    }

    private void createProvisionJournalEntry(BigDecimal amount,
                                             LocalDate closingDate,
                                             Long batchId,
                                             String expenseAccount,
                                             String allowanceAccount,
                                             String currencyCode,
                                             EclAllowanceSummary summary) {
        boolean isAdditionalProvision = amount.signum() > 0;
        BigDecimal absAmount = amount.abs();

        List<ClosingJournalLineCommand> lines = isAdditionalProvision
                ? additionalProvisionLines(absAmount, expenseAccount, allowanceAccount)
                : reversalLines(absAmount, allowanceAccount, summary);

        ClosingJournalEntryCommand command = new ClosingJournalEntryCommand(
                LocalDate.now(),
                closingDate,
                "Month-end ECL Provision (Impairment)",
                "CLOSING_ADJUSTMENT",
                BATCH_ACTOR,
                SYSTEM_ACTOR,
                "ECL_PROVISION",
                summary.lineageSourceId(batchId),
                currencyCode,
                ClosingSlipNoFactory.eclProvision(
                        closingDate,
                        summary.slipDiscriminator(allowanceAccount),
                        batchId),
                lines);

        ClosingJournalEntryResult result = closingJournalEntryPort.createDraftAdjustment(command);
        if (accountingProperties.isAutoPostAdjustments()) {
            closingJournalEntryPort.approveAndPost(result.journalEntryId(), SYSTEM_ACTOR);
        }

        log.info("Successfully created ECL provision journal entry. SlipNo: {}, Amount: {}", result.slipNo(), absAmount);
    }

    private List<ClosingJournalLineCommand> additionalProvisionLines(BigDecimal amount,
                                                                     String expenseAccount,
                                                                     String allowanceAccount) {
        return List.of(
                new ClosingJournalLineCommand(
                        ClosingJournalSide.DEBIT,
                        expenseAccount,
                        amount,
                        amount,
                        "Bad Debt Expense (ECL Addition)"),
                new ClosingJournalLineCommand(
                        ClosingJournalSide.CREDIT,
                        allowanceAccount,
                        amount,
                        amount,
                        "Allowance for Doubtful Accounts (ECL Addition)"));
    }

    private List<ClosingJournalLineCommand> reversalLines(BigDecimal amount,
                                                          String allowanceAccount,
                                                          EclAllowanceSummary summary) {
        String reversalIncomeAccount = requireText(
                summary.reversalIncomeAccountCode(),
                "reversalIncomeAccountCode");
        return List.of(
                new ClosingJournalLineCommand(
                        ClosingJournalSide.DEBIT,
                        allowanceAccount,
                        amount,
                        amount,
                        "Allowance for Doubtful Accounts (ECL Reversal)"),
                new ClosingJournalLineCommand(
                        ClosingJournalSide.CREDIT,
                        reversalIncomeAccount,
                        amount,
                        amount,
                        "Allowance Reversal Income (ECL Reversal)"));
    }

    private String resolveRequiredAccount(String primary, String fallback, String fieldName) {
        if (hasText(primary)) {
            return primary.trim();
        }
        return requireText(fallback, fieldName);
    }

    private String requireText(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalStateException("Missing ECL " + fieldName);
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}