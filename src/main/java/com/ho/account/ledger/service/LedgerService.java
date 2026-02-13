package com.ho.account.ledger.service;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.FiscalPeriod;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.FiscalPeriodRepository;
import com.ho.account.ledger.domain.GlEntry;
import com.ho.account.ledger.domain.GlBalance;
import com.ho.account.ledger.dto.LedgerDTO;
import com.ho.account.ledger.dto.TrialBalanceDTO;
import com.ho.account.ledger.repository.GlBalanceRepository;
import com.ho.account.ledger.repository.GlEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 원장(Ledger) 조회 서비스
 * 총계정원장(General Ledger) 및 합계잔액시산표(Trial Balance) 조회를 담당함.
 */
@Service
@Transactional(readOnly = true)
public class LedgerService {

        @Autowired
        private GlBalanceRepository glBalanceRepository;
        @Autowired
        private GlEntryRepository glEntryRepository;
        @Autowired
        private FiscalPeriodRepository fiscalPeriodRepository;
        @Autowired
        private AccountSubjectRepository accountSubjectRepository;

        // 총계정원장 조회
        public List<LedgerDTO> getGeneralLedger(String accountCode, LocalDate startDate, LocalDate endDate) {
                List<LedgerDTO> ledgerList = new ArrayList<>();
                AccountSubject account = accountSubjectRepository.findByCode(accountCode)
                                .orElseThrow(() -> new IllegalArgumentException("계정과목을 찾을 수 없습니다: " + accountCode));

                // 1. 전기 이월금 계산
                BigDecimal previousBalance = calculatePreviousBalance(account, startDate);

                ledgerList.add(new LedgerDTO(
                                startDate.minusDays(1),
                                "PREV-BAL",
                                "전기 이월",
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                previousBalance));

                // 2. 기간 내 거래 내역 조회 (GlEntry 기반)
                List<GlEntry> entries = glEntryRepository.findByAccountAndPostingDateBetweenOrderByPostingDateAscIdAsc(
                                account, startDate, endDate);

                BigDecimal currentBalance = previousBalance;

                for (GlEntry entry : entries) {
                        BigDecimal debit = entry.getDrAmount();
                        BigDecimal credit = entry.getCrAmount();
                        currentBalance = currentBalance.add(debit).subtract(credit);

                        ledgerList.add(new LedgerDTO(
                                        entry.getPostingDate(),
                                        entry.getJournalDetail().getJournalEntry().getSlipNo(),
                                        entry.getJournalDetail().getDetailDescription() != null
                                                        ? entry.getJournalDetail().getDetailDescription()
                                                        : entry.getJournalDetail().getJournalEntry().getDescription(),
                                        debit,
                                        credit,
                                        currentBalance));
                }

                return ledgerList;
        }

        private BigDecimal calculatePreviousBalance(AccountSubject account, LocalDate startDate) {
                FiscalPeriod period = fiscalPeriodRepository.findByDate(startDate)
                                .orElseThrow(() -> new RuntimeException("회계 기간을 찾을 수 없습니다."));

                // 해당 기간의 기초 잔액 (Begin Balance) 집계
                BigDecimal beginBalance = glBalanceRepository.sumNetBeginBalance(
                                account, period.getFiscalYear(), period.getFiscalPeriod());

                // 기간 시작일부터 startDate 이전까지의 거래 합계 집계
                BigDecimal periodActivity = glEntryRepository.sumNetAmount(
                                account, period.getStartDate(), startDate.minusDays(1));

                return beginBalance.add(periodActivity);
        }

        // 합계잔액시산표 (Trial Balance) 조회
        public List<TrialBalanceDTO> getTrialBalance(String year, String period) {
                List<GlBalance> balances = glBalanceRepository.findForTrialBalance(year, period);

                // 계정과목별 집계 (부서/통화 무관)
                return balances.stream()
                                .collect(java.util.stream.Collectors.groupingBy(b -> b.getAccount().getCode()))
                                .entrySet().stream()
                                .map(entry -> {
                                        AccountSubject acc = entry.getValue().get(0).getAccount();
                                        BigDecimal beginDr = entry.getValue().stream().map(GlBalance::getBeginBalanceDr)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        BigDecimal beginCr = entry.getValue().stream().map(GlBalance::getBeginBalanceCr)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        BigDecimal currDr = entry.getValue().stream().map(GlBalance::getCurrentPeriodDr)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        BigDecimal currCr = entry.getValue().stream().map(GlBalance::getCurrentPeriodCr)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        BigDecimal endDr = entry.getValue().stream().map(GlBalance::getEndBalanceDr)
                                                        .reduce(BigDecimal.ZERO,
                                                                        BigDecimal::add);
                                        BigDecimal endCr = entry.getValue().stream().map(GlBalance::getEndBalanceCr)
                                                        .reduce(BigDecimal.ZERO,
                                                                        BigDecimal::add);

                                        return new TrialBalanceDTO(acc.getCode(), acc.getName(), beginDr, beginCr,
                                                        currDr, currCr, endDr,
                                                        endCr);
                                })
                                .sorted(java.util.Comparator.comparing(TrialBalanceDTO::getAccountCode))
                                .collect(java.util.stream.Collectors.toList());
        }
}
