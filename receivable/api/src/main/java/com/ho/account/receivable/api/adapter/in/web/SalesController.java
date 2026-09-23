package com.ho.account.receivable.api.adapter.in.web;

import com.ho.account.receivable.api.dto.SalesInvoiceRequest;
import com.ho.account.receivable.api.dto.SalesInvoiceResponse;
import com.ho.account.receivable.application.port.in.SalesUseCase;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * [헥사고날 아키텍처 - 인바운드 어댑터 (Inbound Web Adapter)]
 * 매출(Sales) 및 매출채권 관련 API를 제공하는 웹 컨트롤러입니다.
 *
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 시스템의 '매출 접수 창구'입니다.
 * 외부에서 "이만큼 팔았으니 청구서 만들어줘"라는 요청이 들어오면,
 * 그 데이터를 받아서 내부 비즈니스 로직(`SalesService`)이 처리할 수 있게 건네주는 역할을 합니다.
 * 웹 브라우저의 언어(HTTP/JSON)를 우리 시스템의 언어로 바꿔주는 번역기라고 볼 수 있습니다.
 *
 * <p>현재 컨트롤러는 HTTP 요청 검증과 command 변환만 담당합니다. 매출채권 생성, 거래처 검증,
 * 전표 생성 같은 업무 흐름은 core의 {@link SalesUseCase} 구현체가 처리합니다.</p>
 */
@RestController
@RequestMapping({"/api/sales", "/api/receivable"})
public class SalesController {

    private final SalesUseCase salesUseCase;

    public SalesController(SalesUseCase salesUseCase) {
        this.salesUseCase = salesUseCase;
    }

    @PostMapping("/invoices")
    public ResponseEntity<SalesInvoiceResponse> createSalesInvoice(@Valid @RequestBody SalesInvoiceRequest request) {
        return ResponseEntity.ok(SalesInvoiceResponse.fromEntity(
                salesUseCase.createSalesInvoice(request.toCommand())));
    }

    @GetMapping("/invoices")
    public ResponseEntity<List<SalesInvoiceResponse>> findInvoices(
            @RequestParam(value = "status", required = false) String status) {
        List<SalesInvoiceResponse> response = salesUseCase.findInvoices(status).stream()
                .map(SalesInvoiceResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/invoices/{id}")
    public ResponseEntity<SalesInvoiceResponse> findInvoiceById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(SalesInvoiceResponse.fromEntity(salesUseCase.findInvoiceById(id)));
    }

    @PostMapping("/receivables/update-status/{asOfDate}")
    public ResponseEntity<Void> updateReceivableStatus(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        salesUseCase.updateReceivableStatus(asOfDate);
        return ResponseEntity.ok().build();
    }
}
