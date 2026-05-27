package com.ho.account.closing.batch.service;

import com.ho.account.closing.application.port.out.EclAllowanceResultPort;
import com.ho.account.closing.application.service.ClosingAccountingProperties;
import com.ho.account.closing.domain.EclAllowanceSummary;
import com.ho.account.closing.domain.ProvisionBatch;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.domain.ledger.domain.GlAccountBalance;
import com.ho.account.journalledger.domain.ledger.domain.GlBalanceType;
import com.ho.account.journalledger.domain.ledger.repository.GlAccountBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * [결산 배치 - IFRS9 기대신용손실(ECL) 기반 대손충당금 서비스]
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 매월 결산 시점에 회사가 떼일 것으로 예상되는 돈(대손충당금)을 계산하고 회계처리하는 역할을 합니다.
 * 
 * 예를 들어:
 * 1. 은행이 고객들에게 빌려준 대출금(대출채권) 잔액이 총 100억 원이라고 가정합시다.
 * 2. ECL 엔진이 Stage/PD/LGD/EAD와 미래전망 시나리오를 반영해 목표 충당금을 산출합니다.
 * 3. 그런데 이미 기존에 8천만 원을 떼일 것에 대비해 적립(충당금)해 두었습니다.
 * 4. 그렇다면 이번 달에는 추가로 2천만 원만 더 쌓으면 됩니다. (1억 - 8천만)
 * 5. 이 서비스는 그 2천만 원에 대해 "비용(대손상각비) 2천 / 부채(대손충당금) 2천" 이라는 전표를 자동으로 끊어줍니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EclProvisionService {

    private final GlAccountBalanceRepository glAccountBalanceRepository;
    private final JournalUseCase journalUseCase;
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

        // ECL 산출 엔진이 계산한 목표 충당금과 GL의 기존 충당금 잔액 차이만 회계처리한다.
        BigDecimal existingAllowance = BigDecimal.ZERO;
        Optional<GlAccountBalance> existingAllowanceOpt = glAccountBalanceRepository
                .findByAccountCodeAndCurrencyCodeAndBalanceDateAndBalanceType(
                        allowanceForDoubtfulAccounts, currencyCode, closingDate, GlBalanceType.CREDIT);
        
        if (existingAllowanceOpt.isPresent()) {
            existingAllowance = existingAllowanceOpt.get().getEndingBalance();
        }

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

    private void createProvisionJournalEntry(BigDecimal amount, LocalDate closingDate, Long batchId, 
                                             String expenseAccount, String allowanceAccount,
                                             String currencyCode, EclAllowanceSummary summary) {
        boolean isAdditionalProvision = amount.signum() > 0;
        BigDecimal absAmount = amount.abs();

        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(LocalDate.now());
        entry.setAccountingDate(closingDate);
        entry.setDescription("Month-end ECL Provision (Impairment)");
        entry.setStatus(JournalEntryStatus.DRAFT);
        entry.setEntryType("CLOSING_ADJUSTMENT");
        entry.setCreatedBy("BATCH");
        entry.setAuditUser("SYSTEM");
        entry.setLineageSourceType("ECL_PROVISION");
        entry.setLineageSourceId(summary.lineageSourceId(batchId));
        entry.setCurrencyCode(currencyCode);

        JournalDetail debitDetail = new JournalDetail();
        debitDetail.setAmount(absAmount);
        debitDetail.setBaseAmount(absAmount);
        
        JournalDetail creditDetail = new JournalDetail();
        creditDetail.setAmount(absAmount);
        creditDetail.setBaseAmount(absAmount);

        if (isAdditionalProvision) {
            // 보충적립 (비용 인식)
            // 차변: 대손상각비 (비용)
            debitDetail.setSide(JournalSide.DEBIT);
            debitDetail.setAccountCode(expenseAccount);
            debitDetail.setDetailDescription("Bad Debt Expense (ECL Addition)");
            
            // 대변: 대손충당금 (자산 차감)
            creditDetail.setSide(JournalSide.CREDIT);
            creditDetail.setAccountCode(allowanceAccount);
            creditDetail.setDetailDescription("Allowance for Doubtful Accounts (ECL Addition)");
        } else {
            String reversalIncomeAccount = requireText(
                    summary.reversalIncomeAccountCode(),
                    "reversalIncomeAccountCode");

            // 환입 (수익 인식)
            // 차변: 대손충당금
            debitDetail.setSide(JournalSide.DEBIT);
            debitDetail.setAccountCode(allowanceAccount);
            debitDetail.setDetailDescription("Allowance for Doubtful Accounts (ECL Reversal)");
            
            // 대변: 대손충당금환입 (수익)
            creditDetail.setSide(JournalSide.CREDIT);
            creditDetail.setAccountCode(reversalIncomeAccount);
            creditDetail.setDetailDescription("Allowance Reversal Income (ECL Reversal)");
        }

        entry.addDetail(debitDetail);
        entry.addDetail(creditDetail);

        entry.setSlipNo(ClosingSlipNoFactory.eclProvision(
                closingDate,
                summary.slipDiscriminator(allowanceAccount),
                batchId));
        JournalEntry savedEntry = journalUseCase.createJournalEntry(entry);
        
        // 배치이므로 자동 승인 및 전기
        // @todo Closing control: auto approve/post should pass through a closing adjustment approval policy or reversal workflow.
        journalUseCase.approveJournalEntry(savedEntry.getId(), "SYSTEM");
        journalUseCase.postJournalEntry(savedEntry.getId(), "SYSTEM");
        
        log.info("Successfully posted ECL provision journal entry. SlipNo: {}, Amount: {}", savedEntry.getSlipNo(), absAmount);
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
