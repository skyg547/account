package com.ho.account.receivable.adapter.in.web;

import com.ho.account.receivable.application.port.in.SalesUseCase;
import com.ho.account.receivable.dto.SalesInvoiceRequest;
import com.ho.account.receivable.dto.SalesInvoiceResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

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
