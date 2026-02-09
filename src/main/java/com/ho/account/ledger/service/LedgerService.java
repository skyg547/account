package com.accounting.system.ledger.service;

import com.accounting.system.journal.domain.JournalDetail;
import com.accounting.system.journal.repository.JournalDetailRepository;
import com.accounting.system.ledger.dto.LedgerDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class LedgerService {

    private final JournalDetailRepository journalDetailRepository;

    @Autowired
    public LedgerService(JournalDetailRepository journalDetailRepository) {
        this.journalDetailRepository = journalDetailRepository;
    }

    // 총계정원장 조회
    public List<LedgerDTO> getGeneralLedger(String accountCode, LocalDate startDate, LocalDate endDate) {
        List<LedgerDTO> ledgerList = new ArrayList<>();

        // 1. 전기 이월금 계산 (시작일 이전의 모든 거래 합산)
        BigDecimal previousBalance = calculatePreviousBalance(accountCode, startDate);
        
        // 전기 이월금 항목 추가
        ledgerList.add(new LedgerDTO(
                startDate.minusDays(1),
                "PREV-BAL",
                "전기 이월",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                previousBalance
        ));

        // 2. 기간 내 거래 내역 조회
        List<JournalDetail> details = journalDetailRepository.findByAccountAndDateRange(accountCode, startDate, endDate);
        
        BigDecimal currentBalance = previousBalance;

        for (JournalDetail detail : details) {
            BigDecimal debit = BigDecimal.ZERO;
            BigDecimal credit = BigDecimal.ZERO;

            if ("DEBIT".equals(detail.getDrcrType())) {
                debit = detail.getAmount();
                currentBalance = currentBalance.add(debit);
            } else {
                credit = detail.getAmount();
                currentBalance = currentBalance.subtract(credit);
            }

            ledgerList.add(new LedgerDTO(
                    detail.getJournalEntry().getAccountingDate(), // 회계일자 사용
                    detail.getJournalEntry().getSlipNo(),
                    detail.getDetailDescription() != null ? detail.getDetailDescription() : detail.getJournalEntry().getDescription(),
                    debit,
                    credit,
                    currentBalance
            ));
        }

        return ledgerList;
    }

    // 전기 이월금 계산 로직
    private BigDecimal calculatePreviousBalance(String accountCode, LocalDate startDate) {
        List<JournalDetail> prevDetails = journalDetailRepository.findPreviousDetails(accountCode, startDate);
        
        BigDecimal balance = BigDecimal.ZERO;
        for (JournalDetail detail : prevDetails) {
            if ("DEBIT".equals(detail.getDrcrType())) {
                balance = balance.add(detail.getAmount());
            } else {
                balance = balance.subtract(detail.getAmount());
            }
        }
        return balance;
    }
}
