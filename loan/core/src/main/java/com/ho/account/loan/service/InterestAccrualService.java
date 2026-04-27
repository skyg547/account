package com.ho.account.loan.service;

import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
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
 * ?€ì¶??´ì ë°œìƒ(Accrual) ì²˜ë¦¬ ?œë¹„??
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
     * ?¹ì • ?¼ì??ëª¨ë“  ?œì„± ?€ì¶œì— ?€???´ì ë°œìƒ ì²˜ë¦¬ë¥??˜í–‰?©ë‹ˆ??
     */
    @Transactional
    public void processDailyAccrual(LocalDate accrualDate) {
        List<LoanContract> activeLoans = loanContractRepository.findByStatus("ACTIVE");

        for (LoanContract loan : activeLoans) {
            processIndividualAccrual(loan, accrualDate);
        }
    }

    private void processIndividualAccrual(LoanContract loan, LocalDate accrualDate) {
        // ?´ë? ?´ë‹¹ ?¼ì??ë°œìƒ ì²˜ë¦¬ê°€ ?˜ì—ˆ?”ì? ?•ì¸
        if (accrualLogRepository.findByLoanContractIdAndAccrualDate(loan.getId(), accrualDate).isPresent()) {
            return;
        }

        // ?´ë‹¹ ?¼ì???´ë‹¹?˜ëŠ” ?ê° ?¤ì?ì¤???ª© ì°¾ê¸° (?”ë§ ?•ì‚° ê¸°ì? ê°€??
        // ?¤ì œë¡œëŠ” ?¼í•  ê³„ì‚°(Daily Accrual)???•ì„?´ë‚˜, ?¬ê¸°?œëŠ” ê°„ë‹¨???¤ì?ì¤?ê¸°ë°˜ ?”ë§ ì²˜ë¦¬ë¥??ˆì‹œë¡???
        amortizationRepository.findByLoanContractIdAndPaymentDate(loan.getId(), accrualDate)
                .ifPresent(scheduleEntry -> {
                    BigDecimal interestAmount = scheduleEntry.getInterestAmount();

                    LoanAccrualLog log = new LoanAccrualLog();
                    log.setLoanContract(loan);
                    log.setAccrualDate(accrualDate);
                    log.setAccruedAmount(interestAmount);
                    log.setAuditUser("SYSTEM");

                    try {
                        // 1. ?„í‘œ ?ì„±
                        JournalEntry journalEntry = createAccrualJournal(loan, interestAmount, accrualDate);
                        JournalEntry savedJournal = journalService.createJournalEntry(journalEntry);

                        // 2. ë¡œê·¸ ê¸°ë¡
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
        entry.setDescription("?€ì¶??´ì ë°œìƒ ?˜ìµ ?¸ì‹: " + loan.getLoanContractNo());
        entry.setCreatedBy("SYSTEM");

        // ì°¨ë?: ë¯¸ìˆ˜?´ì (?ëŠ” ?€ì¶?ì±„ê¶Œ ì¦ì•¡)
        JournalDetail debit = new JournalDetail();
        debit.setDrcrType("DEBIT");
        debit.setAmount(amount);
        debit.setDetailDescription("ë¯¸ìˆ˜?´ì ë°œìƒ");
        // ?´ìë¯¸ìˆ˜ê¸?ê³„ì • (?? 11501)
        accountSubjectRepository.findByCode("11501").ifPresent(debit::setAccountSubject);

        // ?€ë³€: ?´ì?˜ìµ
        JournalDetail credit = new JournalDetail();
        credit.setDrcrType("CREDIT");
        credit.setAmount(amount);
        credit.setDetailDescription("?´ì?˜ìµ ?¸ì‹");
        // ?€ì¶œì´?ìˆ˜??ê³„ì • (?? 41101)
        accountSubjectRepository.findByCode("41101").ifPresent(credit::setAccountSubject);

        entry.addDetail(debit);
        entry.addDetail(credit);

        return entry;
    }
}
