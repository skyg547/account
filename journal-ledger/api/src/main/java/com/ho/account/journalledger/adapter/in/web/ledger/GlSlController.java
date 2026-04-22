package com.ho.account.journalledger.adapter.in.web.ledger;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Currency;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.journalledger.domain.journal.JournalEntry;
import com.ho.account.journal.repository.JournalEntryRepository;
import com.ho.account.journalledger.domain.ledger.GlBalance;
import com.ho.account.journalledger.domain.ledger.SlBalance;
import com.ho.account.journalledger.domain.ledger.GlEntry;
import com.ho.account.journalledger.adapter.out.persistence.ledger.GlEntryRepository;
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
    private final AccountSubjectRepository accountSubjectRepository;
    private final BusinessPartnerRepository businessPartnerRepository;
    private final GlEntryRepository glEntryRepository;
    private final JournalEntryRepository journalEntryRepository;

    public GlSlController(PostingService postingService,
                          LedgerService ledgerService,
                          AccountSubjectRepository accountSubjectRepository,
                          BusinessPartnerRepository businessPartnerRepository,
                          GlEntryRepository glEntryRepository,
                          JournalEntryRepository journalEntryRepository) {
        this.postingService = postingService;
        this.ledgerService = ledgerService;
        this.accountSubjectRepository = accountSubjectRepository;
        this.businessPartnerRepository = businessPartnerRepository;
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
            accountSubject = accountSubjectRepository.findById(accountCode).orElse(null);
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
            bp = businessPartnerRepository.findByBusinessPartnerCode(businessPartnerCode).orElse(null);
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
