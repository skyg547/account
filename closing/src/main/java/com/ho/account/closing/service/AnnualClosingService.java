package com.ho.account.closing.service;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 연차 결산(Annual Closing) 관련 비즈니스 로직을 처리하는 서비스입니다.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AnnualClosingService {

    private final JournalPersistencePort journalPersistencePort;
    private final AccountSubjectPersistencePort accountSubjectPersistencePort;
    private final JournalUseCase journalUseCase;

    /**
     * 손익 대체 분개를 생성합니다.
     */
    public void performIncomeStatementClosing(int year, String retainedEarningsAccountCode) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        // 1. 해당 연도의 모든 전표 내역 조회
        List<JournalEntry> entries = journalUseCase.getJournalEntriesByDate(startDate, endDate);
        List<JournalDetail> details = entries.stream()
                .flatMap(e -> e.getDetails().stream())
                .collect(Collectors.toList());

        // 2. 계정별 잔액 집계 (수익/비용 계정)
        Map<AccountSubject, BigDecimal> balanceMap = details.stream()
                .filter(d -> d.getAccountSubject().getCategory() == AccountSubject.AccountCategory.REVENUE || 
                             d.getAccountSubject().getCategory() == AccountSubject.AccountCategory.EXPENSES)
                .collect(Collectors.groupingBy(
                        JournalDetail::getAccountSubject,
                        Collectors.reducing(BigDecimal.ZERO, this::calculateSignedAmountForIS, BigDecimal::add)
                ));

        // 3. 손익 대체 분개 생성
        JournalEntry transferEntry = new JournalEntry();
        transferEntry.setSlipDate(endDate);
        transferEntry.setAccountingDate(endDate);
        transferEntry.setDescription(year + "년 손익 대체 분개");
        transferEntry.setEntryType("TRANSFER");

        BigDecimal netIncome = BigDecimal.ZERO;

        for (Map.Entry<AccountSubject, BigDecimal> entry : balanceMap.entrySet()) {
            AccountSubject account = entry.getKey();
            BigDecimal balance = entry.getValue();

            if (balance.compareTo(BigDecimal.ZERO) == 0) continue;

            JournalDetail detail = new JournalDetail();
            detail.setAccountSubject(account);
            detail.setAmount(balance.abs());
            detail.setBaseAmount(balance.abs());

            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                detail.setSide(JournalSide.CREDIT);
            } else {
                detail.setSide(JournalSide.DEBIT);
            }
            transferEntry.addDetail(detail);

            netIncome = netIncome.subtract(balance);
        }

        if (netIncome.compareTo(BigDecimal.ZERO) != 0) {
            AccountSubject retainedEarningsAccount = accountSubjectPersistencePort.findByCode(retainedEarningsAccountCode)
                    .orElseThrow(() -> new IllegalArgumentException("이익잉여금 계정을 찾을 수 없습니다."));

            JournalDetail retainedEarningsDetail = new JournalDetail();
            retainedEarningsDetail.setAccountSubject(retainedEarningsAccount);
            retainedEarningsDetail.setAmount(netIncome.abs());
            retainedEarningsDetail.setBaseAmount(netIncome.abs());

            if (netIncome.compareTo(BigDecimal.ZERO) > 0) {
                retainedEarningsDetail.setSide(JournalSide.CREDIT);
            } else {
                retainedEarningsDetail.setSide(JournalSide.DEBIT);
            }
            transferEntry.addDetail(retainedEarningsDetail);
        }

        if (!transferEntry.getDetails().isEmpty()) {
            // 전표 번호 생성 및 저장
            transferEntry.setSlipNo(endDate.toString() + "-CLS-" + System.currentTimeMillis());
            journalUseCase.createJournalEntry(transferEntry);
        }
    }

    private BigDecimal calculateSignedAmountForIS(JournalDetail detail) {
        AccountSubject.AccountCategory category = detail.getAccountSubject().getCategory();
        if (category == AccountSubject.AccountCategory.EXPENSES) {
            return detail.getSide() == JournalSide.DEBIT ? detail.getAmount() : detail.getAmount().negate();
        } else if (category == AccountSubject.AccountCategory.REVENUE) {
            return detail.getSide() == JournalSide.CREDIT ? detail.getAmount().negate() : detail.getAmount();
        }
        return BigDecimal.ZERO;
    }
}
