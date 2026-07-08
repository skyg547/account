package com.ho.account.expenditure.payable.api.adapter.in.web;

import com.ho.account.expenditure.application.port.in.PurchaseUseCase;
import com.ho.account.expenditure.payable.api.dto.PurchaseInvoiceRequest;
import com.ho.account.expenditure.payable.api.dto.PurchaseInvoiceResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * [헥사고날 아키텍처 - 인바운드 웹 어댑터]
 * 매입 인보이스 등록과 매입채무 상태 갱신 요청을 애플리케이션 포트로 전달합니다.
 *
 * <p>초보자용 설명: 화면이나 API 클라이언트에서 들어온 HTTP 요청을 내부 유즈케이스가
 * 이해할 수 있는 command로 바꿔주는 입구입니다. 실제 업무 규칙은 컨트롤러가 아니라
 * {@link com.ho.account.expenditure.application.service.PurchaseService}에서 처리합니다.</p>
 */
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
    public ResponseEntity<PurchaseInvoiceResponse> createPurchaseInvoice(@Valid @RequestBody PurchaseInvoiceRequest request) {
        // HTTP JSON 입력은 API DTO에서 검증하고, 업무 규칙은 core PurchaseService가 command를 기준으로 처리합니다.
        return ResponseEntity.ok(PurchaseInvoiceResponse.from(purchaseUseCase.createPurchaseInvoice(request.toCommand())));
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