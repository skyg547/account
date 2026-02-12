package com.ho.account.unsettled.web;

import com.ho.account.unsettled.domain.UnsettledItem;
import com.ho.account.unsettled.service.UnsettledService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/unsettled")
public class UnsettledController {

    private final UnsettledService unsettledService;

    @Autowired
    public UnsettledController(UnsettledService unsettledService) {
        this.unsettledService = unsettledService;
    }

    // 미결 현황 조회 (거래처별)
    @GetMapping("/businesspartner/{businessPartnerCode}")
    public List<UnsettledItem> getUnsettledItems(@PathVariable String businessPartnerCode) {
        return unsettledService.getUnsettledItems(customerCode);
    }

    // 수동 반제 처리
    @PostMapping("/{id}/settle")
    public ResponseEntity<Void> settleItem(@PathVariable Long id, @RequestBody Map<String, BigDecimal> body) {
        try {
            BigDecimal amount = body.get("amount");
            unsettledService.settleItem(id, amount);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
