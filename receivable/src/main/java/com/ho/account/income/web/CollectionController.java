package com.ho.account.income.web;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.income.domain.Collection;
import com.ho.account.income.domain.MatchingRule;
import com.ho.account.income.domain.UnmatchedCollection;
import com.ho.account.income.dto.CollectionRequest;
import com.ho.account.income.dto.ManualMatchingRequest;
import com.ho.account.income.dto.MatchingRuleRequest;
import com.ho.account.income.service.CollectionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collections")
public class CollectionController {

    private final CollectionService collectionService;
    private final BusinessPartnerRepository businessPartnerRepository; // For mapping request DTO to domain object

    public CollectionController(CollectionService collectionService, BusinessPartnerRepository businessPartnerRepository) {
        this.collectionService = collectionService;
        this.businessPartnerRepository = businessPartnerRepository;
    }

    /**
     * ?ˆë¡œ???˜ê¸ˆ???˜ì‹ ?˜ê³  ì´ˆê¸° ?„í‘œë¥??ì„±?©ë‹ˆ?? ?ë™ ë§¤ì¹­???œë„?©ë‹ˆ??
     * @param request ?˜ê¸ˆ ?±ë¡ ?”ì²­ DTO
     * @return ?ì„±???˜ê¸ˆ ?•ë³´
     */
    @PostMapping
    public ResponseEntity<Collection> receiveCollection(@Valid @RequestBody CollectionRequest request) {
        Collection collection = new Collection();
        collection.setCollectionDate(request.getCollectionDate());
        collection.setAmount(request.getAmount());
        collection.setBankAccount(request.getBankAccount());
        collection.setVirtualAccount(request.getVirtualAccount());
        collection.setReferenceNo(request.getReferenceNo());

        BusinessPartner customer = businessPartnerRepository.findByBusinessPartnerCode(request.getCustomerCode())
                .orElseThrow(() -> new IllegalArgumentException("ê³ ê° ?•ë³´ë¥?ì°¾ì„ ???†ìŠµ?ˆë‹¤: " + request.getCustomerCode()));
        collection.setCustomer(customer);

        Collection createdCollection = collectionService.receivePayment(collection);
        return new ResponseEntity<>(createdCollection, HttpStatus.CREATED);
    }

    /**
     * ?¹ì • ?˜ê¸ˆ???€???ë™ ë§¤ì¹­???˜ë™?¼ë¡œ ?¸ë¦¬ê±°í•©?ˆë‹¤.
     * (ì£¼ë¡œ ë¯¸ë§¤ì¹??ì—???˜ê¸ˆ???¸ì§‘?????¬ì‹œ?„í•  ???¬ìš©?????ˆìŠµ?ˆë‹¤.)
     * @param collectionId ë§¤ì¹­???œë„???˜ê¸ˆ ID
     * @return ì²˜ë¦¬ ê²°ê³¼ ë©”ì‹œì§€
     */
    @PostMapping("/{collectionId}/auto-match")
    public ResponseEntity<String> triggerAutoMatch(@PathVariable Long collectionId) {
        Collection collection = collectionService.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("?˜ê¸ˆ??ì°¾ì„ ???†ìŠµ?ˆë‹¤: " + collectionId));
        collectionService.attemptAutoMatching(collection);
        return ResponseEntity.ok("Auto matching attempted for collection ID: " + collectionId);
    }

    /**
     * ë¯¸ë§¤ì¹?œ ?˜ê¸ˆ ëª©ë¡ (ë¯¸ë§¤ì¹?????ì¡°íšŒ?©ë‹ˆ??
     * @return ë¯¸ë§¤ì¹??íƒœ???˜ê¸ˆ ëª©ë¡
     */
    @GetMapping("/unmatched")
    public ResponseEntity<List<UnmatchedCollection>> getUnmatchedCollections() {
        List<UnmatchedCollection> unmatchedCollections = collectionService.getUnmatchedCollections();
        return ResponseEntity.ok(unmatchedCollections);
    }

    /**
     * ?˜ê¸ˆ??ë§¤ì¶œì±„ê¶Œ???˜ë™?¼ë¡œ ë§¤ì¹­?©ë‹ˆ??
     * @param request ?˜ë™ ë§¤ì¹­ ?”ì²­ DTO
     * @return ë§¤ì¹­???˜ê¸ˆ ?•ë³´
     */
    @PostMapping("/manual-match")
    public ResponseEntity<Collection> manualMatchCollection(@Valid @RequestBody ManualMatchingRequest request) {
        Collection matchedCollection = collectionService.manualMatchCollection(
                request.getCollectionId(),
                request.getReceivableId(),
                request.getMatchingAmount()
        );
        return ResponseEntity.ok(matchedCollection);
    }

    /**
     * ë§¤ì¹­ ê·œì¹™???ì„± ?ëŠ” ?…ë°?´íŠ¸?©ë‹ˆ??
     * @param request ë§¤ì¹­ ê·œì¹™ ?”ì²­ DTO
     * @return ?€?¥ëœ ë§¤ì¹­ ê·œì¹™ ?•ë³´
     */
    @PostMapping("/matching-rules")
    public ResponseEntity<MatchingRule> saveMatchingRule(@Valid @RequestBody MatchingRuleRequest request) {
        MatchingRule matchingRule = new MatchingRule();
        if (request.getId() != null) {
            matchingRule.setId(request.getId());
        }
        matchingRule.setRuleName(request.getRuleName());
        matchingRule.setPriority(request.getPriority());
        matchingRule.setMatchCriteria(request.getMatchCriteria());
        matchingRule.setToleranceAmount(request.getToleranceAmount());
        matchingRule.setActive(request.isActive());

        MatchingRule savedRule = collectionService.saveMatchingRule(matchingRule);
        return new ResponseEntity<>(savedRule, HttpStatus.CREATED);
    }

    /**
     * ëª¨ë“  ?œì„± ë§¤ì¹­ ê·œì¹™??ì¡°íšŒ?©ë‹ˆ??
     * @return ?œì„± ë§¤ì¹­ ê·œì¹™ ëª©ë¡
     */
    @GetMapping("/matching-rules")
    public ResponseEntity<List<MatchingRule>> getAllMatchingRules() {
        return ResponseEntity.ok(collectionService.getAllActiveMatchingRules());
    }
}
