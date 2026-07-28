package com.ho.account.receivable.api.adapter.in.web;

import com.ho.account.receivable.api.dto.CollectionRequest;
import com.ho.account.receivable.api.dto.CollectionResponse;
import com.ho.account.receivable.api.dto.ManualMatchingRequest;
import com.ho.account.receivable.application.port.in.CollectionUseCase;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [헥사고날 아키텍처 - 인바운드 웹 어댑터]
 * 고객 입금 수신, 자동 매칭, 수동 매칭 요청을 CollectionUseCase로 전달합니다.
 *
 * <p>초보자용 설명: 이 컨트롤러는 은행 입금 자료나 화면 입력을 받아
 * "입금 기록", "청구서와 짝 맞추기", "미매칭 조회" 같은 업무 요청으로 바꿔주는 입구입니다.
 * 금액 차감과 상태 변경은 도메인 객체와 서비스에서 처리하므로 컨트롤러에는 계산 로직을 두지 않습니다.</p>
 *
 * <p>API DTO는 웹 계층의 요청 형식이고, core에는 {@code CollectionCommand}/{@code ManualMatchingCommand}
 * 형태로 전달합니다. 이렇게 하면 Batch나 내부 호출도 같은 업무 규칙을 재사용할 수 있습니다.</p>
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
                collectionUseCase.receivePayment(request.toCommand())));
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
        collectionUseCase.manualMatchCollection(request.toCommand());
        return ResponseEntity.ok().build();
    }
}