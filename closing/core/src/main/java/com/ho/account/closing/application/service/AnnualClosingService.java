package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.in.AnnualClosingUseCase;
import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.contracts.journal.JournalSide;
import com.ho.account.contracts.journal.JournalSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 연차 결산(Annual Closing) 관련 비즈니스 로직을 처리하는 서비스입니다.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AnnualClosingService implements AnnualClosingUseCase {

    private final JournalQueryPort journalQueryPort;
    private final JournalPostingPort journalPostingPort;

    /**
     * 손익 대체 분개를 생성합니다.
     */
    @Override
    public void performIncomeStatementClosing(int year, String retainedEarningsAccountCode) {
        if (year < 1900 || year > 9999) {
            throw new IllegalArgumentException("year must be between 1900 and 9999");
        }
        if (retainedEarningsAccountCode == null || retainedEarningsAccountCode.isBlank()) {
            throw new IllegalArgumentException("retainedEarningsAccountCode must not be blank");
        }
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        String annualSlipNo = ClosingSlipNoFactory.annualClosing(
                endDate,
                year,
                retainedEarningsAccountCode.trim());

        // 1. 해당 연도의 모든 전표 내역 조회
        List<JournalSummary> summaries = journalQueryPort.getJournalSummaries(startDate, endDate);
        JournalSummary existingClosing = summaries.stream()
                .filter(summary -> annualSlipNo.equals(summary.getSlipNo()))
                .findFirst()
                .orElse(null);
        if (existingClosing != null) {
            requireReusableAnnualClosing(existingClosing, endDate, year);
            return;
        }
        // @todo Replace the per-entry N+1 query with a posted base-currency aggregate port.
        // Completion requires provider-side GROUP BY account/category, exclusion of prior annual
        // closing lineage, stable pagination/streaming, and a 100M-row PostgreSQL plan/load test.
        List<JournalDetailSummary> details = summaries.stream()
                .filter(summary -> "POSTED".equals(summary.getStatus()))
                .flatMap(s -> journalQueryPort.getJournalDetails(s.getId()).stream())
                .collect(Collectors.toList());

        // 2. 계정별 잔액 집계 (수익/비용 계정)
        Map<String, BigDecimal> balanceMap = details.stream()
                .filter(d -> "REVENUE".equals(d.getAccountCategory()) || 
                             "EXPENSES".equals(d.getAccountCategory()))
                .collect(Collectors.groupingBy(
                        JournalDetailSummary::getAccountCode,
                        Collectors.reducing(BigDecimal.ZERO, this::calculateSignedAmountForIS, BigDecimal::add)
                ));

        // 3. 손익 대체 분개 라인 생성
        List<JournalLineCommand> lines = new ArrayList<>();
        BigDecimal netIncome = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : balanceMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList()) {
            String accountCode = entry.getKey();
            BigDecimal balance = entry.getValue();

            if (balance.compareTo(BigDecimal.ZERO) == 0) continue;

            String drcrType;
            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                drcrType = JournalSide.CREDIT.name();
            } else {
                drcrType = JournalSide.DEBIT.name();
            }

            lines.add(new JournalLineCommand(
                    drcrType,
                    accountCode,
                    balance.abs(),
                    balance.abs(),
                    null,
                    null,
                    year + "년 손익 대체"
            ));

            netIncome = netIncome.subtract(balance);
        }

        if (netIncome.compareTo(BigDecimal.ZERO) != 0) {
            String drcrType;
            if (netIncome.compareTo(BigDecimal.ZERO) > 0) {
                drcrType = JournalSide.CREDIT.name();
            } else {
                drcrType = JournalSide.DEBIT.name();
            }

            lines.add(new JournalLineCommand(
                    drcrType,
                    retainedEarningsAccountCode,
                    netIncome.abs(),
                    netIncome.abs(),
                    null,
                    null,
                    year + "년 이익잉여금 대체"
            ));
        }

        if (!lines.isEmpty()) {
            JournalEntryCommand command = new JournalEntryCommand(
                    endDate,
                    endDate,
                    year + "년 손익 대체 분개",
                    "TRANSFER",
                    "KRW",
                    BigDecimal.ONE,
                    "SYSTEM",
                    "SYSTEM",
                    "ANNUAL_CLOSING",
                    String.valueOf(year),
                    annualSlipNo,
                    lines
            );
            journalPostingPort.createDraftEntry(command);
        }
    }

    private void requireReusableAnnualClosing(
            JournalSummary existing,
            LocalDate endDate,
            int year) {
        boolean sameHeader = Objects.equals(existing.getAccountingDate(), endDate)
                && Objects.equals(existing.getDescription(), year + "년 손익 대체 분개")
                && Objects.equals(existing.getEntryType(), "TRANSFER");
        if (!sameHeader) {
            throw new IllegalStateException(
                    "Annual closing slip already exists with different business content: "
                            + existing.getSlipNo());
        }
        if ("REJECTED".equals(existing.getStatus()) || "REVERSED".equals(existing.getStatus())) {
            throw new IllegalStateException(
                    "Annual closing slip exists in a non-reusable status " + existing.getStatus());
        }
    }

    private BigDecimal calculateSignedAmountForIS(JournalDetailSummary detail) {
        String category = detail.getAccountCategory();
        BigDecimal baseAmount = Objects.requireNonNull(
                detail.getBaseAmount(),
                "Annual closing requires baseAmount for account " + detail.getAccountCode());
        if ("EXPENSES".equals(category)) {
            return detail.getSide() == JournalSide.DEBIT ? baseAmount : baseAmount.negate();
        } else if ("REVENUE".equals(category)) {
            return detail.getSide() == JournalSide.CREDIT ? baseAmount.negate() : baseAmount;
        }
        return BigDecimal.ZERO;
    }
}
