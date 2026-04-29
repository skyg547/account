package com.ho.account.income.adapter.in.web;

import com.ho.account.income.application.port.in.SalesUseCase;
import com.ho.account.income.domain.SalesInvoice;
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
    public ResponseEntity<SalesInvoice> createSalesInvoice(@RequestBody SalesInvoice invoice) {
        return ResponseEntity.ok(salesUseCase.createSalesInvoice(invoice));
    }

    @PostMapping("/receivables/update-status/{asOfDate}")
    public ResponseEntity<Void> updateReceivableStatus(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        salesUseCase.updateReceivableStatus(asOfDate);
        return ResponseEntity.ok().build();
    }
}
