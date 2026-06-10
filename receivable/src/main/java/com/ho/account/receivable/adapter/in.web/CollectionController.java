package com.ho.account.receivable.adapter.in.web;

import com.ho.account.receivable.application.port.in.CollectionUseCase;
import com.ho.account.receivable.dto.CollectionRequest;
import com.ho.account.receivable.dto.CollectionResponse;
import com.ho.account.receivable.dto.ManualMatchingRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * [헥사고날 아키텍처 - 인바운드 웹 어댑터]
 * 고객 입금 수신, 자동 매칭, 수동 매칭 요청을 CollectionUseCase로 전달합니다.
 *
 * <p>초보자용 설명: 이 컨트롤러는 은행 입금 자료나 화면 입력을 받아
 * "입금 기록", "청구서와 짝 맞추기", "미매칭 조회" 같은 업무 요청으로 바꿔주는 입구입니다.
 * 금액 차감과 상태 변경은 도메인 객체와 서비스에서 처리하므로 컨트롤러에는 계산 로직을 두지 않습니다.</p>
 */
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
