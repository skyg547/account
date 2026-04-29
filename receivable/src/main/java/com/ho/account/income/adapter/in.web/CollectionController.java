package com.ho.account.income.adapter.in.web;

import com.ho.account.income.application.port.in.CollectionUseCase;
import com.ho.account.income.domain.Collection;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/collections")
public class CollectionController {

    private final CollectionUseCase collectionUseCase;

    public CollectionController(CollectionUseCase collectionUseCase) {
        this.collectionUseCase = collectionUseCase;
    }

    @PostMapping
    public ResponseEntity<Collection> receivePayment(@RequestBody Collection collection) {
        return ResponseEntity.ok(collectionUseCase.receivePayment(collection));
    }

    @PostMapping("/{collectionId}/auto-match")
    public ResponseEntity<Void> attemptAutoMatching(@PathVariable Long collectionId) {
        collectionUseCase.attemptAutoMatching(collectionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unmatched")
    public ResponseEntity<List<Collection>> getUnmatchedCollections() {
        return ResponseEntity.ok(collectionUseCase.getUnmatchedCollections());
    }

    @PostMapping("/manual-match")
    public ResponseEntity<Void> manualMatchCollection(@RequestBody Map<String, Object> request) {
        Long collectionId = Long.valueOf(request.get("collectionId").toString());
        Long receivableId = Long.valueOf(request.get("receivableId").toString());
        BigDecimal amount = new BigDecimal(request.get("amount").toString());
        collectionUseCase.manualMatchCollection(collectionId, receivableId, amount);
        return ResponseEntity.ok().build();
    }
}
