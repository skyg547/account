package com.ho.account.journalledger.application.service.ledger;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journal.repository.JournalEntryRepository;
import com.ho.account.journalledger.domain.ledger.GlEntry;
import com.ho.account.journalledger.domain.ledger.SlEntry;
import com.ho.account.journalledger.adapter.out.persistence.ledger.GlEntryRepository;
import com.ho.account.journalledger.adapter.out.persistence.ledger.SlEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class PostingService {

    private final JournalEntryRepository journalEntryRepository;
    private final GlEntryRepository glEntryRepository;
    private final SlEntryRepository slEntryRepository;
    private final LedgerService ledgerService;

    public PostingService(JournalEntryRepository journalEntryRepository,
                          GlEntryRepository glEntryRepository,
                          SlEntryRepository slEntryRepository,
                          LedgerService ledgerService) {
        this.journalEntryRepository = journalEntryRepository;
        this.glEntryRepository = glEntryRepository;
        this.slEntryRepository = slEntryRepository;
        this.ledgerService = ledgerService;
    }

    /**
     * DRAFT ?ë¨?’— APPROVED ?ê³¹ê¹­???ê¾ªëª´??POSTED ?ê³¹ê¹­æ¿?è¹‚Â€å¯ƒì?ë¸?€? ?ë¨?˜£ è«?è¹‚ëŒ??ë¨?˜£ ?ê³¸ê½­??æ¹²ê³•ì¤??Å??ë¶¿ë¸¸????¼ë–†åª›ê¾©?æ¿¡???…ëœ²??„ë“ƒ??¸ë•²??(ï§ì•¸????…ëœ²??„ë“ƒ).
     */
    @Transactional
    public void postJournalEntry(Long journalEntryId) {
        JournalEntry journalEntry = journalEntryRepository.findById(journalEntryId)
                .orElseThrow(() -> new IllegalArgumentException("JournalEntry not found: " + journalEntryId));

        if (journalEntry.getStatus() == JournalEntryStatus.POSTED || journalEntry.getStatus() == JournalEntryStatus.REVERSED) {
            throw new IllegalStateException("JournalEntry is already posted or reversed.");
        }

        // ?ê³¹ê¹­??POSTEDæ¿?è¹‚Â€å¯?
        journalEntry.setStatus(JournalEntryStatus.POSTED);
        journalEntryRepository.save(journalEntry);

        LocalDate accountingDate = journalEntry.getAccountingDate();
        String fiscalYear = String.valueOf(accountingDate.getYear());
        String fiscalPeriod = String.format("%02d", accountingDate.getMonthValue());

        // åª??ê³¸ê½­ ??±ì”¤??????GlEntry, SlEntry ??¹ê½¦ è«??ë¶¿ë¸¸ ??…ëœ²??„ë“ƒ
        for (JournalDetail detail : journalEntry.getDetails()) {
            boolean isDebit = "DEBIT".equals(detail.getDrcrType());

            // 1. GlEntry ??¹ê½¦
            GlEntry glEntry = new GlEntry();
            glEntry.setJournalDetail(detail);
            glEntry.setAccount(detail.getAccountSubject());
            glEntry.setFiscalYear(fiscalYear);
            glEntry.setFiscalPeriod(fiscalPeriod);
            glEntry.setPostingDate(accountingDate);
            glEntry.setCurrency(journalEntry.getCurrency());
            glEntry.setExchangeRate(journalEntry.getExchangeRate());
            
            if (isDebit) {
                glEntry.setDrAmount(detail.getAmount());
                glEntry.setCrAmount(java.math.BigDecimal.ZERO);
                glEntry.setBaseDrAmount(detail.getBaseAmount());
                glEntry.setBaseCrAmount(java.math.BigDecimal.ZERO);
            } else {
                glEntry.setDrAmount(java.math.BigDecimal.ZERO);
                glEntry.setCrAmount(detail.getAmount());
                glEntry.setBaseDrAmount(java.math.BigDecimal.ZERO);
                glEntry.setBaseCrAmount(detail.getBaseAmount());
            }

            glEntry.setLineageSourceType(journalEntry.getLineageSourceType());
            glEntry.setLineageSourceId(journalEntry.getLineageSourceId());
            glEntryRepository.save(glEntry);

            // 2. SlEntry ??¹ê½¦ (å«„ê³•?’ï§£??ë¨?’— ?ºÂ€??? ??ˆë’— å¯ƒìŒ??ë¨?£„ æ¹²ê³•????¹ê½¦, Drill-down ??¸ë£„)
            SlEntry slEntry = new SlEntry();
            slEntry.setJournalDetail(detail);
            slEntry.setAccount(detail.getAccountSubject());
            slEntry.setBusinessPartner(detail.getBusinessPartner());
            slEntry.setDepartment(detail.getDepartment());
            slEntry.setFiscalYear(fiscalYear);
            slEntry.setFiscalPeriod(fiscalPeriod);
            slEntry.setPostingDate(accountingDate);
            slEntry.setCurrency(journalEntry.getCurrency());
            slEntry.setExchangeRate(journalEntry.getExchangeRate());
            
            if (isDebit) {
                slEntry.setDrAmount(detail.getAmount());
                slEntry.setCrAmount(java.math.BigDecimal.ZERO);
                slEntry.setBaseDrAmount(detail.getBaseAmount());
                slEntry.setBaseCrAmount(java.math.BigDecimal.ZERO);
            } else {
                slEntry.setDrAmount(java.math.BigDecimal.ZERO);
                slEntry.setCrAmount(detail.getAmount());
                slEntry.setBaseDrAmount(java.math.BigDecimal.ZERO);
                slEntry.setBaseCrAmount(detail.getBaseAmount());
            }

            slEntry.setLineageSourceType(journalEntry.getLineageSourceType());
            slEntry.setLineageSourceId(journalEntry.getLineageSourceId());
            slEntryRepository.save(slEntry);

            // 3. ï§ì•¸???ë¶¿ë¸¸ ??…ëœ²??„ë“ƒ
            ledgerService.updateLedgerBalances(detail, accountingDate);
        }
    }
}
