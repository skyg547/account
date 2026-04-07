package com.ho.account.loan.service;

import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.service.JournalService;
import com.ho.account.loan.domain.LoanAccrualLog;
import com.ho.account.loan.domain.LoanContract;
import com.ho.account.loan.repository.LoanAccrualLogRepository;
import com.ho.account.loan.repository.LoanContractRepository;
import com.ho.account.loan.repository.LoanAmortizationScheduleEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 대출 이자 발생(Accrual) 처리 서비스
 */
@Service
public class InterestAccrualService {

    private final LoanContractRepository loanContractRepository;
    private final LoanAmortizationScheduleEntryRepository amortizationRepository;
    private final LoanAccrualLogRepository accrualLogRepository;
    private final JournalService journalService;
    private final AccountSubjectRepository accountSubjectRepository;

    @Autowired
    public InterestAccrualService(LoanContractRepository loanContractRepository,
            LoanAmortizationScheduleEntryRepository amortizationRepository,
            LoanAccrualLogRepository accrualLogRepository,
            JournalService journalService,
            AccountSubjectRepository accountSubjectRepository) {
        this.loanContractRepository = loanContractRepository;
        this.amortizationRepository = amortizationRepository;
        this.accrualLogRepository = accrualLogRepository;
        this.journalService = journalService;
        this.accountSubjectRepository = accountSubjectRepository;
    }

    /**
     * 특정 일자의 모든 활성 대출에 대해 이자 발생 처리를 수행합니다.
     */
    @Transactional
    public void processDailyAccrual(LocalDate accrualDate) {
        List<LoanContract> activeLoans = loanContractRepository.findByStatus("ACTIVE");

        for (LoanContract loan : activeLoans) {
            processIndividualAccrual(loan, accrualDate);
        }
    }

    private void processIndividualAccrual(LoanContract loan, LocalDate accrualDate) {
        // 이미 해당 일자에 발생 처리가 되었는지 확인
        if (accrualLogRepository.findByLoanContractIdAndAccrualDate(loan.getId(), accrualDate).isPresent()) {
            return;
        }

        // 해당 일자에 해당하는 상각 스케줄 항목 찾기 (월말 정산 기준 가정)
        // 실제로는 일할 계산(Daily Accrual)이 정석이나, 여기서는 간단히 스케줄 기반 월말 처리를 예시로 함
        amortizationRepository.findByLoanContractIdAndPaymentDate(loan.getId(), accrualDate)
                .ifPresent(scheduleEntry -> {
                    BigDecimal interestAmount = scheduleEntry.getInterestAmount();

                    LoanAccrualLog log = new LoanAccrualLog();
                    log.setLoanContract(loan);
                    log.setAccrualDate(accrualDate);
                    log.setAccruedAmount(interestAmount);
                    log.setAuditUser("SYSTEM");

                    try {
                        // 1. 전표 생성
                        JournalEntry journalEntry = createAccrualJournal(loan, interestAmount, accrualDate);
                        JournalEntry savedJournal = journalService.createJournalEntry(journalEntry);

                        // 2. 로그 기록
                        log.setJournalNo(savedJournal.getSlipNo());
                        log.setStatus("SUCCESS");
                    } catch (Exception e) {
                        log.setStatus("FAILED");
                        log.setErrorMessage(e.getMessage());
                    }

                    accrualLogRepository.save(log);
                });
    }

    private JournalEntry createAccrualJournal(LoanContract loan, BigDecimal amount, LocalDate date) {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(LocalDate.now());
        entry.setAccountingDate(date);
        entry.setDescription("대출 이자 발생 수익 인식: " + loan.getLoanContractNo());
        entry.setCreatedBy("SYSTEM");

        // 차변: 미수이자 (또는 대출 채권 증액)
        JournalDetail debit = new JournalDetail();
        debit.setDrcrType("DEBIT");
        debit.setAmount(amount);
        debit.setDetailDescription("미수이자 발생");
        // 이자미수금 계정 (예: 11501)
        accountSubjectRepository.findByCode("11501").ifPresent(debit::setAccountSubject);

        // 대변: 이자수익
        JournalDetail credit = new JournalDetail();
        credit.setDrcrType("CREDIT");
        credit.setAmount(amount);
        credit.setDetailDescription("이자수익 인식");
        // 대출이자수익 계정 (예: 41101)
        accountSubjectRepository.findByCode("41101").ifPresent(credit::setAccountSubject);

        entry.addDetail(debit);
        entry.addDetail(credit);

        return entry;
    }
}
