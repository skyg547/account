package com.ho.account.journalledger.adapter.in.web.ledger;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.repository.JournalEntryRepository;
import com.ho.account.journalledger.domain.ledger.domain.GlBalance;
import com.ho.account.journalledger.domain.ledger.domain.SlBalance;
import com.ho.account.journalledger.domain.ledger.domain.GlEntry;
import com.ho.account.journalledger.domain.ledger.repository.GlEntryRepository;
import com.ho.account.journalledger.application.service.ledger.LedgerService;
import com.ho.account.journalledger.application.service.ledger.PostingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * GL 및 SL 잔액과 엔트리를 조회하는 컨트롤러
 */
@RestController
@RequestMapping("/api/ledger")
public class GlSlController {

    private final PostingService postingService;
    private final LedgerService ledgerService;
    private final AccountSubjectPersistencePort accountSubjectPersistencePort;
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;
    private final GlEntryRepository glEntryRepository;
    private final JournalEntryRepository journalEntryRepository;

    public GlSlController(PostingService postingService,
                          LedgerService ledgerService,
                          AccountSubjectPersistencePort accountSubjectPersistencePort,
                          BusinessPartnerPersistencePort businessPartnerPersistencePort,
                          GlEntryRepository glEntryRepository,
                          JournalEntryRepository journalEntryRepository) {
        this.postingService = postingService;
        this.ledgerService = ledgerService;
        this.accountSubjectPersistencePort = accountSubjectPersistencePort;
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
        this.glEntryRepository = glEntryRepository;
        this.journalEntryRepository = journalEntryRepository;
    }

    /**
     * 전표 원장 전기 (Posting)
     */
    @PostMapping("/post/{journalEntryId}")
    public ResponseEntity<String> postJournalEntry(@PathVariable Long journalEntryId) {
        postingService.postJournalEntry(journalEntryId);
        return ResponseEntity.ok("Successfully posted journal entry: " + journalEntryId);
    }

    /**
     * GL 원장 잔액 조회 (계정 x 기간)
     */
    @GetMapping("/gl/balances")
    public ResponseEntity<?> getGlBalances(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false) String accountCode) {
        
        AccountSubject accountSubject = null;
        if (accountCode != null) {
            accountSubject = accountSubjectPersistencePort.findByCode(accountCode).orElse(null);
        }
        
        List<GlBalance> balances = ledgerService.getGlBalances(startDate, endDate, accountSubject, null);
        return ResponseEntity.ok(balances);
    }

    /**
     * SL 보조원장 잔액 조회 (거래처 x 기간)
     */
    @GetMapping("/sl/balances")
    public ResponseEntity<?> getSlBalances(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false) String businessPartnerCode) {
        
        BusinessPartner bp = null;
        if (businessPartnerCode != null) {
            bp = businessPartnerPersistencePort.findByBusinessPartnerCode(businessPartnerCode).orElse(null);
        }
        
        List<SlBalance> balances = ledgerService.getSlBalances(startDate, endDate, null, bp, null, null);
        return ResponseEntity.ok(balances);
    }

    /**
     * Drill-down: 원천 추적 (보고 -> 원장 -> 전표)
     */
    @GetMapping("/drill-down")
    public ResponseEntity<?> getDrillDown(
            @RequestParam String sourceType,
            @RequestParam String sourceId) {
        
        List<GlEntry> glEntries = glEntryRepository.findByLineageSourceTypeAndLineageSourceId(sourceType, sourceId);
        return ResponseEntity.ok(glEntries);
    }
}
