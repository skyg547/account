package com.ho.account.expenditure.adapter.in.web;

import com.ho.account.expenditure.application.port.in.PurchaseUseCase;
import com.ho.account.expenditure.domain.PurchaseInvoice;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/purchase")
public class PurchaseController {

    private final PurchaseUseCase purchaseUseCase;

    public PurchaseController(PurchaseUseCase purchaseUseCase) {
        this.purchaseUseCase = purchaseUseCase;
    }

    /**
     * 매입 인보이스를 등록합니다.
     */
    @PostMapping("/invoices")
    public ResponseEntity<PurchaseInvoice> createPurchaseInvoice(@RequestBody PurchaseInvoice invoice) {
        return ResponseEntity.ok(purchaseUseCase.createPurchaseInvoice(invoice));
    }

    /**
     * 매입채무 상태를 업데이트합니다 (연체 체크 등).
     */
    @PostMapping("/payables/update-status/{asOfDate}")
    public ResponseEntity<Void> updatePayableStatus(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        purchaseUseCase.updatePayableStatus(asOfDate);
        return ResponseEntity.ok().build();
    }
}
