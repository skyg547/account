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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

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
        Objects.requireNonNull(closingDate, "closingDate must not be null");
        if (provisionBatchId == null || provisionBatchId <= 0) {
            throw new IllegalArgumentException("provisionBatchId must be positive");
        }
        log.info("Starting ECL Provision calculation for closing date: {}", closingDate);

        List<EclAllowanceSummary> summaries = eclAllowanceResultPort.loadSummaries(closingDate);
        if (summaries.isEmpty()) {
            throw new IllegalStateException(
                    "No finalized ECL allowance summary found for " + closingDate
                            + "; a zero-portfolio completion marker is required before treating this as no-op");
        }

        ClosingAccountingProperties.AutomatedJournalRule eclRule =
                accountingProperties.requireProvisionRule(ProvisionBatch.ProvisionType.ECL);

        requireSingleSnapshotIdentity(summaries);
        String legalEntityCode = requireSingleLegalEntity(summaries);
        Map<ProvisionKey, ProvisionGroup> groups =
                aggregateByLedgerBalanceKey(summaries, closingDate, legalEntityCode, eclRule);
        groups.values().stream()
                .sorted(Comparator.comparing(group -> group.key().stableValue()))
                .forEach(group -> processGroup(group, closingDate, provisionBatchId));
    }

    private void processGroup(
            ProvisionGroup group,
            LocalDate closingDate,
            Long provisionBatchId) {
        BigDecimal existingAllowance = allowanceBalanceLookupPort.findCreditEndingBalance(
                group.key().allowanceAccountCode(),
                group.key().currencyCode(),
                closingDate);
        BigDecimal difference = group.targetAllowanceAmount().subtract(existingAllowance);

        if (difference.signum() == 0) {
            log.info("ECL provision target equals existing allowance. No journal required. group={}",
                    group.key().stableValue());
            return;
        }

        createProvisionJournalEntry(
                difference,
                closingDate,
                provisionBatchId,
                group);
    }

    private void createProvisionJournalEntry(BigDecimal amount,
                                             LocalDate closingDate,
                                             Long batchId,
                                             ProvisionGroup group) {
        boolean isAdditionalProvision = amount.signum() > 0;
        BigDecimal absAmount = amount.abs();

        List<ClosingJournalLineCommand> lines = isAdditionalProvision
                ? additionalProvisionLines(
                        absAmount,
                        group.badDebtExpenseAccountCode(),
                        group.key().allowanceAccountCode())
                : reversalLines(
                        absAmount,
                        group.key().allowanceAccountCode(),
                        requireText(group.reversalIncomeAccountCode(), "reversalIncomeAccountCode"));

        ClosingJournalEntryCommand command = new ClosingJournalEntryCommand(
                closingDate,
                closingDate,
                "Month-end ECL Provision (Impairment)",
                "CLOSING_ADJUSTMENT",
                BATCH_ACTOR,
                SYSTEM_ACTOR,
                "ECL_PROVISION",
                group.lineageSourceId(batchId),
                group.key().currencyCode(),
                ClosingSlipNoFactory.eclProvision(
                        closingDate,
                        group.key().stableValue(),
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
                                                          String reversalIncomeAccount) {
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

    private String requireSingleLegalEntity(List<EclAllowanceSummary> summaries) {
        Set<String> legalEntities = new TreeSet<>();
        for (EclAllowanceSummary summary : summaries) {
            legalEntities.add(requireText(summary.legalEntityCode(), "legalEntityCode"));
        }
        if (legalEntities.size() != 1) {
            throw new IllegalStateException(
                    "Closing GL has no legal-entity dimension; exactly one legal entity is required per ECL run: "
                            + legalEntities);
        }
        return legalEntities.iterator().next();
    }

    private void requireSingleSnapshotIdentity(List<EclAllowanceSummary> summaries) {
        Set<String> runIds = new TreeSet<>();
        Set<String> modelVersions = new TreeSet<>();
        for (EclAllowanceSummary summary : summaries) {
            runIds.add(summary.runId());
            modelVersions.add(summary.modelVersion());
        }
        if (runIds.size() != 1 || modelVersions.size() != 1) {
            throw new IllegalStateException(
                    "ECL closing input must come from one finalized run/model snapshot: runs="
                            + runIds + ", models=" + modelVersions);
        }
    }

    private Map<ProvisionKey, ProvisionGroup> aggregateByLedgerBalanceKey(
            List<EclAllowanceSummary> summaries,
            LocalDate closingDate,
            String legalEntityCode,
            ClosingAccountingProperties.AutomatedJournalRule eclRule) {
        Map<ProvisionKey, ProvisionGroup> groups = new LinkedHashMap<>();
        for (EclAllowanceSummary summary : summaries) {
            if (!closingDate.equals(summary.baseDate())) {
                throw new IllegalStateException(
                        "ECL summary baseDate " + summary.baseDate() + " does not match closingDate " + closingDate);
            }

            String allowanceAccount = resolveRequiredAccount(
                    summary.allowanceAccountCode(),
                    eclRule.getCreditAccountCode(),
                    "allowance account");
            String expenseAccount = resolveRequiredAccount(
                    summary.badDebtExpenseAccountCode(),
                    eclRule.getDebitAccountCode(),
                    "bad debt expense account");
            String currencyCode = requireText(summary.currencyCode(), "currencyCode");
            String reversalAccount = hasText(summary.reversalIncomeAccountCode())
                    ? summary.reversalIncomeAccountCode().trim()
                    : null;
            ProvisionKey key = new ProvisionKey(legalEntityCode, currencyCode, allowanceAccount);
            groups.compute(key, (ignored, current) -> current == null
                    ? ProvisionGroup.first(key, summary, expenseAccount, reversalAccount)
                    : current.add(summary, expenseAccount, reversalAccount));
        }
        return groups;
    }

    private record ProvisionKey(
            String legalEntityCode,
            String currencyCode,
            String allowanceAccountCode) {

        private String stableValue() {
            return legalEntityCode + "|" + currencyCode + "|" + allowanceAccountCode;
        }
    }

    private record ProvisionGroup(
            ProvisionKey key,
            BigDecimal targetAllowanceAmount,
            String badDebtExpenseAccountCode,
            String reversalIncomeAccountCode) {

        private static ProvisionGroup first(
                ProvisionKey key,
                EclAllowanceSummary summary,
                String expenseAccount,
                String reversalAccount) {
            return new ProvisionGroup(
                    key,
                    summary.targetAllowanceAmount(),
                    expenseAccount,
                    reversalAccount);
        }

        private ProvisionGroup add(
                EclAllowanceSummary summary,
                String expenseAccount,
                String reversalAccount) {
            requireSameMapping(
                    badDebtExpenseAccountCode,
                    expenseAccount,
                    "bad debt expense",
                    key);
            String resolvedReversal = mergeOptionalMapping(
                    reversalIncomeAccountCode,
                    reversalAccount,
                    "reversal income",
                    key);
            return new ProvisionGroup(
                    key,
                    targetAllowanceAmount.add(summary.targetAllowanceAmount()),
                    badDebtExpenseAccountCode,
                    resolvedReversal);
        }

        private String lineageSourceId(Long batchId) {
            Objects.requireNonNull(batchId, "batchId must not be null");
            return batchId + "|" + key.allowanceAccountCode() + "|" + key.currencyCode();
        }

        private static void requireSameMapping(
                String current,
                String candidate,
                String mappingName,
                ProvisionKey key) {
            if (!Objects.equals(current, candidate)) {
                throw new IllegalStateException(
                        "Mixed " + mappingName + " account mapping for ECL group " + key.stableValue());
            }
        }

        private static String mergeOptionalMapping(
                String current,
                String candidate,
                String mappingName,
                ProvisionKey key) {
            if (current == null) {
                return candidate;
            }
            if (candidate == null) {
                return current;
            }
            requireSameMapping(current, candidate, mappingName, key);
            return current;
        }
    }
}
