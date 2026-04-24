package com.ho.account.journalledger.adapter.in.web.ledger;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Currency;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journal.repository.JournalEntryRepository;
import com.ho.account.journalledger.core.domain.ledger.domain.GlBalance;
import com.ho.account.journalledger.core.domain.ledger.domain.SlBalance;
import com.ho.account.journalledger.core.domain.ledger.domain.GlEntry;
import com.ho.account.journalledger.core.domain.ledger.repository.GlEntryRepository;
import com.ho.account.journalledger.application.service.ledger.LedgerService;
import com.ho.account.journalledger.application.service.ledger.PostingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ledger")
public class GlSlController {

    private final PostingService postingService;
    private final LedgerService ledgerService;
    private final accountSubjectPersistencePort accountSubjectPersistencePort;
    private final businessPartnerPersistencePort businessPartnerPersistencePort;
    private final GlEntryRepository glEntryRepository;
    private final JournalEntryRepository journalEntryRepository;

    public GlSlController(PostingService postingService,
                          LedgerService ledgerService,
                          accountSubjectPersistencePort accountSubjectPersistencePort,
                          businessPartnerPersistencePort businessPartnerPersistencePort,
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
     * ?�표 ?�장 ?�기 (Posting)
     */
    @PostMapping("/post/{journalEntryId}")
    public ResponseEntity<String> postJournalEntry(@PathVariable Long journalEntryId) {
        postingService.postJournalEntry(journalEntryId);
        return ResponseEntity.ok("Successfully posted journal entry: " + journalEntryId);
    }

    /**
     * GL ?�장 ?�액 조회 (계정 x 기간)
     */
    @GetMapping("/gl/balances")
    public ResponseEntity<?> getGlBalances(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false) String accountCode) {
        
        AccountSubject accountSubject = null;
        if (accountCode != null) {
            accountSubject = accountSubjectPersistencePort.findById(accountCode).orElse(null);
        }
        
        List<GlBalance> balances = ledgerService.getGlBalances(startDate, endDate, accountSubject, null);
        return ResponseEntity.ok(balances);
    }

    /**
     * SL 보조?�장 ?�액 조회 (거래�?x 기간)
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
     * Drill-down: ?�천 추적 (보고 -> ?�장 -> ?�표)
     */
    @GetMapping("/drill-down")
    public ResponseEntity<?> getDrillDown(
            @RequestParam String sourceType,
            @RequestParam String sourceId) {
        
        List<GlEntry> glEntries = glEntryRepository.findByLineageSourceTypeAndLineageSourceId(sourceType, sourceId);
        return ResponseEntity.ok(glEntries);
    }
}
