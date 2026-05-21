package com.ho.account.closing.batch.service;

import com.ho.account.closing.application.service.ClosingAccountingProperties;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

/**
 * [결산 배치 - 기대신용손실(ECL) 기반 대손충당금 서비스]
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 매월 결산 시점에 회사가 떼일 것으로 예상되는 돈(대손충당금)을 계산하고 회계처리하는 역할을 합니다.
 * 
 * 예를 들어:
 * 1. 은행이 고객들에게 빌려준 대출금(대출채권) 잔액이 총 100억 원이라고 가정합시다.
 * 2. 과거 통계를 보니, 이 중 1%인 1억 원은 결국 못 받을 것(부도)으로 예상됩니다. (ECL: Expected Credit Loss)
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

    // 대출채권 계정 (평가 대상 원본) - 하드코딩된 예시이며, 실제로는 마스터 메타데이터에서 관리해야 함
    private static final String LOAN_RECEIVABLE_ACCOUNT = "12000"; 
    // 기대신용손실률 (1%) - IFRS 9 모델러가 산출한 Stage별 확률이라고 가정
    private static final BigDecimal ECL_RATE = new BigDecimal("0.01");

    @Transactional
    public void processEclProvision(LocalDate closingDate, Long provisionBatchId) {
        log.info("Starting ECL Provision calculation for closing date: {}", closingDate);

        // 1. 회계 정책 속성(Properties)에서 충당금 분개 룰을 가져옵니다.
        // application.yml 등에 account.closing.accounting.provision-rules.ECL = ... 형태로 정의됨
        ClosingAccountingProperties.AutomatedJournalRule eclRule = 
            accountingProperties.requireProvisionRule(ProvisionBatch.ProvisionType.ECL);

        String badDebtExpenseAccount = eclRule.getDebitAccountCode(); // 예: 83000 (대손상각비)
        String allowanceForDoubtfulAccounts = eclRule.getCreditAccountCode(); // 예: 12001 (대손충당금)

        // 2. 기말 대출채권 잔액 조회 (간소화를 위해 특정 계정 하나만 조회)
        Optional<GlAccountBalance> loanBalanceOpt = glAccountBalanceRepository
                .findByAccountCodeAndCurrencyCodeAndBalanceDateAndBalanceType(
                        LOAN_RECEIVABLE_ACCOUNT, "KRW", closingDate, GlBalanceType.DEBIT);

        if (loanBalanceOpt.isEmpty()) {
            log.warn("No Loan Receivable balance found for {} to calculate ECL.", closingDate);
            return;
        }

        BigDecimal loanPrincipal = loanBalanceOpt.get().getEndingBalance();
        if (loanPrincipal.signum() <= 0) {
            log.info("Loan Receivable balance is zero or negative. No provision required.");
            return;
        }

        // 3. 기대신용손실(목표 충당금) 산출 = 대출채권 잔액 * 1%
        BigDecimal targetAllowance = loanPrincipal.multiply(ECL_RATE).setScale(2, RoundingMode.HALF_UP);

        // 4. 기존에 쌓여있는 대손충당금 잔액 조회
        BigDecimal existingAllowance = BigDecimal.ZERO;
        Optional<GlAccountBalance> existingAllowanceOpt = glAccountBalanceRepository
                .findByAccountCodeAndCurrencyCodeAndBalanceDateAndBalanceType(
                        allowanceForDoubtfulAccounts, "KRW", closingDate, GlBalanceType.CREDIT);
        
        if (existingAllowanceOpt.isPresent()) {
            existingAllowance = existingAllowanceOpt.get().getEndingBalance();
        }

        // 5. 추가로 적립해야 할 금액 계산 (보충법)
        // 목표액보다 기존 잔액이 적으면 추가 적립(비용 발생), 많으면 환입(수익 발생) 처리.
        BigDecimal difference = targetAllowance.subtract(existingAllowance);

        if (difference.signum() == 0) {
            log.info("ECL provision target equals existing allowance. No journal required.");
            return;
        }

        // 6. 충당금 전표(Journal Entry) 생성
        createProvisionJournalEntry(difference, closingDate, provisionBatchId, badDebtExpenseAccount, allowanceForDoubtfulAccounts);
    }

    private void createProvisionJournalEntry(BigDecimal amount, LocalDate closingDate, Long batchId, 
                                             String expenseAccount, String allowanceAccount) {
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
        entry.setLineageSourceId(batchId.toString());
        entry.setCurrencyCode("KRW");

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
            // 환입 (수익 인식)
            // 차변: 대손충당금
            debitDetail.setSide(JournalSide.DEBIT);
            debitDetail.setAccountCode(allowanceAccount);
            debitDetail.setDetailDescription("Allowance for Doubtful Accounts (ECL Reversal)");
            
            // 대변: 대손충당금환입 (수익) -> 예제를 위해 임시로 expenseAccount를 반대로 씀. (실무는 별도 수익 계정)
            creditDetail.setSide(JournalSide.CREDIT);
            creditDetail.setAccountCode(expenseAccount); 
            creditDetail.setDetailDescription("Bad Debt Expense Reversal (Income)");
        }

        entry.addDetail(debitDetail);
        entry.addDetail(creditDetail);

        entry.setSlipNo(closingDate + "-ECL-" + System.currentTimeMillis());
        JournalEntry savedEntry = journalUseCase.createJournalEntry(entry);
        
        // 배치이므로 자동 승인 및 전기
        journalUseCase.approveJournalEntry(savedEntry.getId(), "SYSTEM");
        journalUseCase.postJournalEntry(savedEntry.getId(), "SYSTEM");
        
        log.info("Successfully posted ECL provision journal entry. SlipNo: {}, Amount: {}", savedEntry.getSlipNo(), absAmount);
    }
}