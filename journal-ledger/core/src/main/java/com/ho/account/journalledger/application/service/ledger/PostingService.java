package com.ho.account.journalledger.application.service.ledger;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.domain.journal.repository.JournalEntryRepository;
import com.ho.account.journalledger.domain.ledger.domain.GlEntry;
import com.ho.account.journalledger.domain.ledger.domain.SlEntry;
import com.ho.account.journalledger.domain.ledger.repository.GlEntryRepository;
import com.ho.account.journalledger.domain.ledger.repository.SlEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.math.BigDecimal;

/**
 * [헥사고날 아키텍처 - 애플리케이션 서비스 (Application Service)]
 * 전기 서비스 (Posting Service) — 전표를 원장에 반영합니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 전기(Posting)란 승인된 '전표(영수증)'의 내용을 바탕으로 실제 '총계정원장(GL)'과 '보조원장(SL)'이라는 큰 장부에 기록을 옮겨 적는 행위입니다.
 * 이 클래스는 전표 승인 이후, 해당 전표의 상세 라인(차변/대변)을 하나씩 읽어서 원장 엔티티(GlEntry, SlEntry)로 변환하고
 * 영속성 포트를 통해 DB에 저장하는 중추적인 역할을 합니다.
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
        postJournalEntry(journalEntryId, "SYSTEM");
    }

    @Transactional
    public void postJournalEntry(Long journalEntryId, String poster) {
        JournalEntry journalEntry = journalEntryRepository.findById(journalEntryId)
                .orElseThrow(() -> new IllegalArgumentException("JournalEntry not found: " + journalEntryId));

        journalEntry.post(poster);
        journalEntryRepository.save(journalEntry);

        LocalDate accountingDate = journalEntry.getAccountingDate();
        String fiscalYear = String.valueOf(accountingDate.getYear());
        String fiscalPeriod = String.format("%02d", accountingDate.getMonthValue());

        List<GlEntry> glEntries = new ArrayList<>();
        List<SlEntry> slEntries = new ArrayList<>();
        List<JournalDetail> details = journalEntry.getDetails();

        for (JournalDetail detail : details) {
            boolean isDebit = JournalSide.DEBIT.equals(detail.getSide());

            GlEntry glEntry = new GlEntry();
            glEntry.setJournalDetail(detail);
            glEntry.setAccountCode(detail.getAccountCode());
            glEntry.setFiscalYear(fiscalYear);
            glEntry.setFiscalPeriod(fiscalPeriod);
            glEntry.setPostingDate(accountingDate);
            glEntry.setCurrencyCode(journalEntry.getCurrencyCode());
            glEntry.setLineageSourceType(journalEntry.getLineageSourceType());
            glEntry.setLineageSourceId(journalEntry.getLineageSourceId());

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
            glEntries.add(glEntry);

            SlEntry slEntry = new SlEntry();
            slEntry.setJournalDetail(detail);
            slEntry.setAccountCode(detail.getAccountCode());
            slEntry.setBusinessPartnerCode(detail.getBusinessPartnerCode());
            slEntry.setDepartmentCode(detail.getDepartmentCode());
            slEntry.setFiscalYear(fiscalYear);
            slEntry.setFiscalPeriod(fiscalPeriod);
            slEntry.setPostingDate(accountingDate);
            slEntry.setCurrencyCode(journalEntry.getCurrencyCode());
            slEntry.setLineageSourceType(journalEntry.getLineageSourceType());
            slEntry.setLineageSourceId(journalEntry.getLineageSourceId());

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
            slEntries.add(slEntry);
        }

        glEntryRepository.saveAll(glEntries);
        slEntryRepository.saveAll(slEntries);
        
        ledgerService.updateLedgerBalancesBulk(details);
    }
}