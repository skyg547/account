package com.ho.account.income.web;

import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.repository.BusinessPartnerRepository;
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
     * 새로운 수금을 수신하고 초기 전표를 생성합니다. 자동 매칭을 시도합니다.
     * @param request 수금 등록 요청 DTO
     * @return 생성된 수금 정보
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
                .orElseThrow(() -> new IllegalArgumentException("고객 정보를 찾을 수 없습니다: " + request.getCustomerCode()));
        collection.setCustomer(customer);

        Collection createdCollection = collectionService.receivePayment(collection);
        return new ResponseEntity<>(createdCollection, HttpStatus.CREATED);
    }

    /**
     * 특정 수금에 대해 자동 매칭을 수동으로 트리거합니다.
     * (주로 미매칭 큐에서 수금을 편집한 후 재시도할 때 사용될 수 있습니다.)
     * @param collectionId 매칭을 시도할 수금 ID
     * @return 처리 결과 메시지
     */
    @PostMapping("/{collectionId}/auto-match")
    public ResponseEntity<String> triggerAutoMatch(@PathVariable Long collectionId) {
        Collection collection = collectionService.collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("수금을 찾을 수 없습니다: " + collectionId));
        collectionService.attemptAutoMatching(collection);
        return ResponseEntity.ok("Auto matching attempted for collection ID: " + collectionId);
    }

    /**
     * 미매칭된 수금 목록 (미매칭 큐)을 조회합니다.
     * @return 미매칭 상태의 수금 목록
     */
    @GetMapping("/unmatched")
    public ResponseEntity<List<UnmatchedCollection>> getUnmatchedCollections() {
        List<UnmatchedCollection> unmatchedCollections = collectionService.getUnmatchedCollections();
        return ResponseEntity.ok(unmatchedCollections);
    }

    /**
     * 수금을 매출채권에 수동으로 매칭합니다.
     * @param request 수동 매칭 요청 DTO
     * @return 매칭된 수금 정보
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
     * 매칭 규칙을 생성 또는 업데이트합니다.
     * @param request 매칭 규칙 요청 DTO
     * @return 저장된 매칭 규칙 정보
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
     * 모든 활성 매칭 규칙을 조회합니다.
     * @return 활성 매칭 규칙 목록
     */
    @GetMapping("/matching-rules")
    public ResponseEntity<List<MatchingRule>> getAllMatchingRules() {
        return ResponseEntity.ok(collectionService.getAllActiveMatchingRules());
    }
}
