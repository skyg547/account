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
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort; // For mapping request DTO to domain object

    public CollectionController(CollectionService collectionService, BusinessPartnerPersistencePort businessPartnerPersistencePort) {
        this.collectionService = collectionService;
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
    }

    /**
     * ?덈줈???섍툑???섏떊?섍퀬 珥덇린 ?꾪몴瑜??앹꽦?⑸땲?? ?먮룞 留ㅼ묶???쒕룄?⑸땲??
     * @param request ?섍툑 ?깅줉 ?붿껌 DTO
     * @return ?앹꽦???섍툑 ?뺣낫
     */
    @PostMapping
    public ResponseEntity<Collection> receiveCollection(@Valid @RequestBody CollectionRequest request) {
        Collection collection = new Collection();
        collection.setCollectionDate(request.getCollectionDate());
        collection.setAmount(request.getAmount());
        collection.setBankAccount(request.getBankAccount());
        collection.setVirtualAccount(request.getVirtualAccount());
        collection.setReferenceNo(request.getReferenceNo());

        BusinessPartner customer = businessPartnerPersistencePort.findByBusinessPartnerCode(request.getCustomerCode())
                .orElseThrow(() -> new IllegalArgumentException("怨좉컼 ?뺣낫瑜?李얠쓣 ???놁뒿?덈떎: " + request.getCustomerCode()));
        collection.setCustomer(customer);

        Collection createdCollection = collectionService.receivePayment(collection);
        return new ResponseEntity<>(createdCollection, HttpStatus.CREATED);
    }

    /**
     * ?뱀젙 ?섍툑??????먮룞 留ㅼ묶???섎룞?쇰줈 ?몃━嫄고빀?덈떎.
     * (二쇰줈 誘몃ℓ移??먯뿉???섍툑???몄쭛?????ъ떆?꾪븷 ???ъ슜?????덉뒿?덈떎.)
     * @param collectionId 留ㅼ묶???쒕룄???섍툑 ID
     * @return 泥섎━ 寃곌낵 硫붿떆吏
     */
    @PostMapping("/{collectionId}/auto-match")
    public ResponseEntity<String> triggerAutoMatch(@PathVariable Long collectionId) {
        Collection collection = collectionService.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("?섍툑??李얠쓣 ???놁뒿?덈떎: " + collectionId));
        collectionService.attemptAutoMatching(collection);
        return ResponseEntity.ok("Auto matching attempted for collection ID: " + collectionId);
    }

    /**
     * 誘몃ℓ移?맂 ?섍툑 紐⑸줉 (誘몃ℓ移?????議고쉶?⑸땲??
     * @return 誘몃ℓ移??곹깭???섍툑 紐⑸줉
     */
    @GetMapping("/unmatched")
    public ResponseEntity<List<UnmatchedCollection>> getUnmatchedCollections() {
        List<UnmatchedCollection> unmatchedCollections = collectionService.getUnmatchedCollections();
        return ResponseEntity.ok(unmatchedCollections);
    }

    /**
     * ?섍툑??留ㅼ텧梨꾧텒???섎룞?쇰줈 留ㅼ묶?⑸땲??
     * @param request ?섎룞 留ㅼ묶 ?붿껌 DTO
     * @return 留ㅼ묶???섍툑 ?뺣낫
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
     * 留ㅼ묶 洹쒖튃???앹꽦 ?먮뒗 ?낅뜲?댄듃?⑸땲??
     * @param request 留ㅼ묶 洹쒖튃 ?붿껌 DTO
     * @return ??λ맂 留ㅼ묶 洹쒖튃 ?뺣낫
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
     * 紐⑤뱺 ?쒖꽦 留ㅼ묶 洹쒖튃??議고쉶?⑸땲??
     * @return ?쒖꽦 留ㅼ묶 洹쒖튃 紐⑸줉
     */
    @GetMapping("/matching-rules")
    public ResponseEntity<List<MatchingRule>> getAllMatchingRules() {
        return ResponseEntity.ok(collectionService.getAllActiveMatchingRules());
    }
}
