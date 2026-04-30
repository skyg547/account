package com.ho.account.tax.adapter.in.web;

import com.ho.account.tax.application.port.in.TaxInvoiceUseCase;
import com.ho.account.tax.dto.TaxInvoiceDto;
import com.ho.account.tax.dto.TaxInvoiceRequestDto;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ap/invoices")
public class APInvoiceController {

    private final TaxInvoiceUseCase taxInvoiceUseCase;

    public APInvoiceController(TaxInvoiceUseCase taxInvoiceUseCase) {
        this.taxInvoiceUseCase = taxInvoiceUseCase;
    }

    @PostMapping
    public ResponseEntity<TaxInvoiceDto> createAPInvoice(@Valid @RequestBody TaxInvoiceRequestDto requestDto) {
        return ResponseEntity.ok(TaxInvoiceDto.fromEntity(taxInvoiceUseCase.createAPInvoice(requestDto)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaxInvoiceDto> getAPInvoiceById(@PathVariable Long id) {
        return taxInvoiceUseCase.getAPInvoiceById(id)
                .map(TaxInvoiceDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/issue-id/{issueId}")
    public ResponseEntity<TaxInvoiceDto> getAPInvoiceByIssueId(@PathVariable String issueId) {
        return taxInvoiceUseCase.getAPInvoiceByIssueId(issueId)
                .map(TaxInvoiceDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<TaxInvoiceDto>> getAPInvoicesBetweenDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(taxInvoiceUseCase.getAPInvoicesBetweenDates(startDate, endDate).stream()
                .map(TaxInvoiceDto::fromEntity)
                .collect(Collectors.toList()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaxInvoiceDto> updateAPInvoice(
            @PathVariable Long id, @Valid @RequestBody TaxInvoiceRequestDto requestDto) {
        return ResponseEntity.ok(TaxInvoiceDto.fromEntity(taxInvoiceUseCase.updateAPInvoice(id, requestDto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAPInvoice(@PathVariable Long id) {
        taxInvoiceUseCase.deleteAPInvoice(id);
        return ResponseEntity.noContent().build();
    }
}
