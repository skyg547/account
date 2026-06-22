package com.ho.account.receivable.adapter.in.web;

import com.ho.account.receivable.application.port.in.SalesUseCase;
import com.ho.account.receivable.dto.SalesInvoiceRequest;
import com.ho.account.receivable.dto.SalesInvoiceResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * [헥사고날 아키텍처 - 인바운드 어댑터 (Inbound Web Adapter)]
 * 매출(Sales) 및 매출채권 관련 API를 제공하는 웹 컨트롤러입니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 시스템의 '매출 접수 창구'입니다.
 * 외부에서 "이만큼 팔았으니 청구서 만들어줘"라는 요청이 들어오면, 
 * 그 데이터를 받아서 내부 비즈니스 로직(`SalesService`)이 처리할 수 있게 건네주는 역할을 합니다.
 * 웹 브라우저의 언어(HTTP/JSON)를 우리 시스템의 언어로 바꿔주는 번역기라고 볼 수 있습니다.
 */
@RestController
@RequestMapping("/api/sales")
public class SalesController {

    private final SalesUseCase salesUseCase;

    public SalesController(SalesUseCase salesUseCase) {
        this.salesUseCase = salesUseCase;
    }

    @PostMapping("/invoices")
    public ResponseEntity<SalesInvoiceResponse> createSalesInvoice(@Valid @RequestBody SalesInvoiceRequest request) {
        return ResponseEntity.ok(SalesInvoiceResponse.fromEntity(
                salesUseCase.createSalesInvoice(request.toEntity())));
    }

    @PostMapping("/receivables/update-status/{asOfDate}")
    public ResponseEntity<Void> updateReceivableStatus(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        salesUseCase.updateReceivableStatus(asOfDate);
        return ResponseEntity.ok().build();
    }
}
