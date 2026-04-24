package com.ho.account.journalledger.application.service.ledger;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.repository.JournalEntryRepository;
import com.ho.account.journalledger.domain.ledger.domain.GlEntry;
import com.ho.account.journalledger.domain.ledger.domain.SlEntry;
import com.ho.account.journalledger.domain.ledger.repository.GlEntryRepository;
import com.ho.account.journalledger.domain.ledger.repository.SlEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.math.BigDecimal;

/**
 * 전표 데이터를 원장(GL/SL)에 전기(Posting)하는 서비스
 */
@Service
@RequiredArgsConstructor
public class PostingService {

    private final JournalEntryRepository journalEntryRepository;
    private final GlEntryRepository glEntryRepository;
    private final SlEntryRepository slEntryRepository;
    private final LedgerService ledgerService;

    @Transactional
    public void postJournalEntry(Long journalEntryId) {
        JournalEntry journalEntry = journalEntryRepository.findById(journalEntryId)
                .orElseThrow(() -> new IllegalArgumentException("JournalEntry not found: " + journalEntryId));

        if (journalEntry.getStatus() == JournalEntryStatus.POSTED || journalEntry.getStatus() == JournalEntryStatus.REVERSED) {
            throw new IllegalStateException("JournalEntry is already posted or reversed.");
        }

        journalEntry.setStatus(JournalEntryStatus.POSTED);
        journalEntryRepository.save(journalEntry);

        LocalDate accountingDate = journalEntry.getAccountingDate();
        String fiscalYear = String.valueOf(accountingDate.getYear());
        String fiscalPeriod = String.format("%02d", accountingDate.getMonthValue());

        for (JournalDetail detail : journalEntry.getDetails()) {
            boolean isDebit = "DEBIT".equals(detail.getDrcrType());

            // 1. GlEntry 생성
            GlEntry glEntry = new GlEntry();
            glEntry.setJournalDetail(detail);
            glEntry.setAccount(detail.getAccountSubject());
            glEntry.setFiscalYear(fiscalYear);
            glEntry.setFiscalPeriod(fiscalPeriod);
            glEntry.setPostingDate(accountingDate);
            glEntry.setCurrency(journalEntry.getCurrency());
            
            if (isDebit) {
                glEntry.setDrAmount(detail.getAmount());
                glEntry.setCrAmount(BigDecimal.ZERO);
                glEntry.setBaseDrAmount(detail.getBaseAmount());
                glEntry.setBaseCrAmount(BigDecimal.ZERO);
            } else {
                glEntry.setDrAmount(BigDecimal.ZERO);
                glEntry.setCrAmount(detail.getAmount());
                glEntry.setBaseDrAmount(BigDecimal.ZERO);
                glEntry.setBaseCrAmount(detail.getBaseAmount());
            }
            glEntryRepository.save(glEntry);

            // 2. SlEntry 생성
            SlEntry slEntry = new SlEntry();
            slEntry.setJournalDetail(detail);
            slEntry.setAccount(detail.getAccountSubject());
            slEntry.setBusinessPartner(detail.getBusinessPartner());
            slEntry.setDepartment(detail.getDepartment());
            slEntry.setFiscalYear(fiscalYear);
            slEntry.setFiscalPeriod(fiscalPeriod);
            slEntry.setPostingDate(accountingDate);
            slEntry.setCurrency(journalEntry.getCurrency());
            
            if (isDebit) {
                slEntry.setDrAmount(detail.getAmount());
                slEntry.setCrAmount(BigDecimal.ZERO);
                slEntry.setBaseDrAmount(detail.getBaseAmount());
                slEntry.setBaseCrAmount(BigDecimal.ZERO);
            } else {
                slEntry.setDrAmount(BigDecimal.ZERO);
                slEntry.setCrAmount(detail.getAmount());
                slEntry.setBaseDrAmount(BigDecimal.ZERO);
                slEntry.setBaseCrAmount(detail.getBaseAmount());
            }
            slEntryRepository.save(slEntry);

            // 3. 잔액 업데이트 (Carry-forward 포함)
            ledgerService.updateLedgerBalances(detail, accountingDate);
        }
    }
}
