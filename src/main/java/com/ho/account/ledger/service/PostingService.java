package com.ho.account.ledger.service;

import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.domain.JournalEntryStatus;
import com.ho.account.journal.repository.JournalEntryRepository;
import com.ho.account.ledger.domain.GlEntry;
import com.ho.account.ledger.domain.SlEntry;
import com.ho.account.ledger.repository.GlEntryRepository;
import com.ho.account.ledger.repository.SlEntryRepository;
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
     * DRAFT 또는 APPROVED 상태의 전표를 POSTED 상태로 변경하고, 원장 및 보조원장 상세를 기록하며 잔액을 실시간으로 업데이트합니다 (증분 업데이트).
     */
    @Transactional
    public void postJournalEntry(Long journalEntryId) {
        JournalEntry journalEntry = journalEntryRepository.findById(journalEntryId)
                .orElseThrow(() -> new IllegalArgumentException("JournalEntry not found: " + journalEntryId));

        if (journalEntry.getStatus() == JournalEntryStatus.POSTED || journalEntry.getStatus() == JournalEntryStatus.REVERSED) {
            throw new IllegalStateException("JournalEntry is already posted or reversed.");
        }

        // 상태를 POSTED로 변경
        journalEntry.setStatus(JournalEntryStatus.POSTED);
        journalEntryRepository.save(journalEntry);

        LocalDate accountingDate = journalEntry.getAccountingDate();
        String fiscalYear = String.valueOf(accountingDate.getYear());
        String fiscalPeriod = String.format("%02d", accountingDate.getMonthValue());

        // 각 상세 라인에 대해 GlEntry, SlEntry 생성 및 잔액 업데이트
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

            // 2. SlEntry 생성 (거래처 또는 부서가 있는 경우에도 기본 생성, Drill-down 용도)
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

            // 3. 증분 잔액 업데이트
            ledgerService.updateLedgerBalances(detail, accountingDate);
        }
    }
}
