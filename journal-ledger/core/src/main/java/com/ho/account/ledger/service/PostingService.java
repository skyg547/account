package com.ho.account.journalledger.application.service.ledger;

import com.ho.account.journalledger.domain.journal.JournalDetail;
import com.ho.account.journalledger.domain.journal.JournalEntry;
import com.ho.account.journalledger.domain.journal.JournalEntryStatus;
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
     * DRAFT ?먮뒗 APPROVED ?곹깭???꾪몴瑜?POSTED ?곹깭濡?蹂寃쏀븯怨? ?먯옣 諛?蹂댁“?먯옣 ?곸꽭瑜?湲곕줉?섎ŉ ?붿븸???ㅼ떆媛꾩쑝濡??낅뜲?댄듃?⑸땲??(利앸텇 ?낅뜲?댄듃).
     */
    @Transactional
    public void postJournalEntry(Long journalEntryId) {
        JournalEntry journalEntry = journalEntryRepository.findById(journalEntryId)
                .orElseThrow(() -> new IllegalArgumentException("JournalEntry not found: " + journalEntryId));

        if (journalEntry.getStatus() == JournalEntryStatus.POSTED || journalEntry.getStatus() == JournalEntryStatus.REVERSED) {
            throw new IllegalStateException("JournalEntry is already posted or reversed.");
        }

        // ?곹깭瑜?POSTED濡?蹂寃?
        journalEntry.setStatus(JournalEntryStatus.POSTED);
        journalEntryRepository.save(journalEntry);

        LocalDate accountingDate = journalEntry.getAccountingDate();
        String fiscalYear = String.valueOf(accountingDate.getYear());
        String fiscalPeriod = String.format("%02d", accountingDate.getMonthValue());

        // 媛??곸꽭 ?쇱씤?????GlEntry, SlEntry ?앹꽦 諛??붿븸 ?낅뜲?댄듃
        for (JournalDetail detail : journalEntry.getDetails()) {
            boolean isDebit = "DEBIT".equals(detail.getDrcrType());

            // 1. GlEntry ?앹꽦
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

            // 2. SlEntry ?앹꽦 (嫄곕옒泥??먮뒗 遺?쒓? ?덈뒗 寃쎌슦?먮룄 湲곕낯 ?앹꽦, Drill-down ?⑸룄)
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

            // 3. 利앸텇 ?붿븸 ?낅뜲?댄듃
            ledgerService.updateLedgerBalances(detail, accountingDate);
        }
    }
}
