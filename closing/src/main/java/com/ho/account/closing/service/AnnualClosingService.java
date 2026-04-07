package com.ho.account.closing.service;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.repository.JournalDetailRepository;
import com.ho.account.journal.service.JournalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 연차 결산(Annual Closing) 관련 비즈니스 로직을 처리하는 서비스입니다.
 * 손익 대체 분개 생성, 잔액 이월 등의 핵심 기능을 담당합니다.
 */
@Service
@Transactional
public class AnnualClosingService {

    private final JournalDetailRepository journalDetailRepository;
    private final AccountSubjectRepository accountSubjectRepository;
    private final JournalService journalService;

    @Autowired
    public AnnualClosingService(JournalDetailRepository journalDetailRepository,
                                AccountSubjectRepository accountSubjectRepository,
                                JournalService journalService) {
        this.journalDetailRepository = journalDetailRepository;
        this.accountSubjectRepository = accountSubjectRepository;
        this.journalService = journalService;
    }

    /**
     * 손익 대체 분개를 생성합니다.
     * 해당 연도의 모든 수익/비용 계정 잔액을 '0'으로 만들고, 그 차액(당기순이익)을 이익잉여금 계정으로 대체합니다.
     *
     * @param year 회계 연도
     * @param retainedEarningsAccountCode 이익잉여금 계정 코드
     */
    public void performIncomeStatementClosing(int year, String retainedEarningsAccountCode) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        // 1. 해당 연도의 모든 승인된 손익계산서(IS) 계정 거래 내역 조회
        List<JournalDetail> details = journalDetailRepository.findByAccountAndDateRangeForIS(startDate, endDate);

        // 2. 계정별 잔액 집계
        Map<AccountSubject, BigDecimal> balanceMap = details.stream()
                .filter(d -> AccountSubject.AccountCategory.REVENUE.name().equals(d.getAccountSubject().getCategory().name()) || AccountSubject.AccountCategory.EXPENSES.name().equals(d.getAccountSubject().getCategory().name()))
                .collect(Collectors.groupingBy(
                        JournalDetail::getAccountSubject,
                        Collectors.reducing(BigDecimal.ZERO, this::calculateSignedAmountForIS, BigDecimal::add)
                ));

        // 3. 손익 대체 분개 생성
        JournalEntry transferEntry = new JournalEntry();
        transferEntry.setSlipDate(endDate);
        transferEntry.setAccountingDate(endDate);
        transferEntry.setDescription(year + "년 손익 대체 분개");
        transferEntry.setEntryType("TRANSFER"); // 전표 유형: 손익대체

        BigDecimal netIncome = BigDecimal.ZERO;

        // 3.1 수익/비용 계정 잔액 '0' 만들기
        for (Map.Entry<AccountSubject, BigDecimal> entry : balanceMap.entrySet()) {
            AccountSubject account = entry.getKey();
            BigDecimal balance = entry.getValue();

            if (balance.compareTo(BigDecimal.ZERO) == 0) continue;

            JournalDetail detail = new JournalDetail();
            detail.setAccountSubject(account);
            detail.setAmount(balance.abs()); // 금액은 절대값

            // 잔액의 반대편으로 분개 생성
            // 수익(대변 잔액) -> 차변으로 분개
            // 비용(차변 잔액) -> 대변으로 분개
            if (balance.compareTo(BigDecimal.ZERO) > 0) { // 양수 잔액 (비용 또는 수익 반대 거래)
                detail.setDrcrType("CREDIT");
            } else { // 음수 잔액 (수익 또는 비용 반대 거래)
                detail.setDrcrType("DEBIT");
            }
            transferEntry.addDetail(detail);

            netIncome = netIncome.subtract(balance); // 당기순이익 계산
        }

        // 3.2 당기순이익을 이익잉여금 계정으로 대체
        if (netIncome.compareTo(BigDecimal.ZERO) != 0) {
            AccountSubject retainedEarningsAccount = accountSubjectRepository.findById(retainedEarningsAccountCode)
                    .orElseThrow(() -> new IllegalArgumentException("이익잉여금 계정을 찾을 수 없습니다."));

            JournalDetail retainedEarningsDetail = new JournalDetail();
            retainedEarningsDetail.setAccountSubject(retainedEarningsAccount);
            retainedEarningsDetail.setAmount(netIncome.abs());

            // 당기순이익(양수) -> 대변(자본 증가)
            // 당기순손실(음수) -> 차변(자본 감소)
            if (netIncome.compareTo(BigDecimal.ZERO) > 0) {
                retainedEarningsDetail.setDrcrType("CREDIT");
            } else {
                retainedEarningsDetail.setDrcrType("DEBIT");
            }
            transferEntry.addDetail(retainedEarningsDetail);
        }

        // 4. 생성된 손익 대체 전표 저장 및 승인
        if (!transferEntry.getDetails().isEmpty()) {
            JournalEntry savedEntry = journalService.createJournalEntry(transferEntry);
            journalService.requestApproval(savedEntry.getId());
            journalService.approveJournalEntry(savedEntry.getId());
        }
    }

    // 손익계산서용 부호 계산 (비용: 차변+, 수익: 대변-)
    private BigDecimal calculateSignedAmountForIS(JournalDetail detail) {
        String type = detail.getAccountSubject().getCategory().name();
        if (AccountSubject.AccountCategory.EXPENSES.name().equals(type)) {
            return "DEBIT".equals(detail.getDrcrType()) ? detail.getAmount() : detail.getAmount().negate();
        } else if (AccountSubject.AccountCategory.REVENUE.name().equals(type)) {
            return "CREDIT".equals(detail.getDrcrType()) ? detail.getAmount().negate() : detail.getAmount();
        }
        return BigDecimal.ZERO;
    }
}
