package com.ho.account.receivable.adapter.in.web;

import com.ho.account.receivable.application.port.in.CollectionUseCase;
import com.ho.account.receivable.dto.CollectionRequest;
import com.ho.account.receivable.dto.CollectionResponse;
import com.ho.account.receivable.dto.ManualMatchingRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collections")
public class CollectionController {

    private final CollectionUseCase collectionUseCase;

    public CollectionController(CollectionUseCase collectionUseCase) {
        this.collectionUseCase = collectionUseCase;
    }

    @PostMapping
    public ResponseEntity<CollectionResponse> receivePayment(@Valid @RequestBody CollectionRequest request) {
        return ResponseEntity.ok(CollectionResponse.fromEntity(
                collectionUseCase.receivePayment(request.toEntity())));
    }

    @PostMapping("/{collectionId}/auto-match")
    public ResponseEntity<Void> attemptAutoMatching(@PathVariable Long collectionId) {
        collectionUseCase.attemptAutoMatching(collectionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unmatched")
    public ResponseEntity<List<CollectionResponse>> getUnmatchedCollections() {
        return ResponseEntity.ok(collectionUseCase.getUnmatchedCollections().stream()
                .map(CollectionResponse::fromEntity)
                .toList());
    }

    @PostMapping("/manual-match")
    public ResponseEntity<Void> manualMatchCollection(@Valid @RequestBody ManualMatchingRequest request) {
        collectionUseCase.manualMatchCollection(
                request.getCollectionId(),
                request.getReceivableId(),
                request.getMatchingAmount());
        return ResponseEntity.ok().build();
    }
}
