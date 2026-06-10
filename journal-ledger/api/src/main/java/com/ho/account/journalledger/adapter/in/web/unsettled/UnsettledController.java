package com.ho.account.journalledger.adapter.in.web.unsettled;

import com.ho.account.journalledger.application.port.in.UnsettledItemUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 미결 항목 관리 API 컨트롤러
 */
@RestController
@RequestMapping("/api/unsettled")
public class UnsettledController {

    private final UnsettledItemUseCase unsettledItemUseCase;

    public UnsettledController(UnsettledItemUseCase unsettledItemUseCase) {
        this.unsettledItemUseCase = unsettledItemUseCase;
    }

    /**
     * 미결 현황 조회 (거래처별)
     */
    @GetMapping("/businesspartner/{businessPartnerCode}")
    public List<UnsettledApiDto.View> getUnsettledItems(@PathVariable String businessPartnerCode) {
        return unsettledItemUseCase.getUnsettledItems(businessPartnerCode).stream()
                .map(UnsettledApiDto.View::from)
                .toList();
    }

    /**
     * 수동 반제 처리
     */
    @PostMapping("/{id}/settle")
    public ResponseEntity<Void> settleItem(
            @PathVariable Long id,
            @RequestHeader("X-User-ID") String actor,
            @Valid @RequestBody UnsettledApiDto.SettlementRequest request) {
        try {
            unsettledItemUseCase.settleItem(id, request.amount(), actor, request.settlementReference());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
