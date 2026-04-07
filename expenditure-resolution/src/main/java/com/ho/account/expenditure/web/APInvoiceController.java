package com.ho.account.expenditure.web;

import com.ho.account.expenditure.service.APInvoiceService;
import com.ho.account.tax.domain.TaxInvoice;
import com.ho.account.tax.dto.TaxInvoiceDto;
import com.ho.account.tax.dto.TaxInvoiceRequestDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ap/invoices") // Dedicated path for AP Invoices
public class APInvoiceController {

    private final APInvoiceService apInvoiceService;

    @Autowired
    public APInvoiceController(APInvoiceService apInvoiceService) {
        this.apInvoiceService = apInvoiceService;
    }

    @PostMapping
    public ResponseEntity<TaxInvoiceDto> createAPInvoice(@Valid @RequestBody TaxInvoiceRequestDto requestDto) {
        try {
            TaxInvoice createdInvoice = apInvoiceService.createAPInvoice(requestDto);
            return ResponseEntity.ok(TaxInvoiceDto.fromEntity(createdInvoice));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null); // Or a custom error response DTO
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaxInvoiceDto> getAPInvoiceById(@PathVariable Long id) {
        return apInvoiceService.getAPInvoiceById(id)
                .map(invoice -> ResponseEntity.ok(TaxInvoiceDto.fromEntity(invoice)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/issue-id/{issueId}")
    public ResponseEntity<TaxInvoiceDto> getAPInvoiceByIssueId(@PathVariable String issueId) {
        return apInvoiceService.getAPInvoiceByIssueId(issueId)
                .map(invoice -> ResponseEntity.ok(TaxInvoiceDto.fromEntity(invoice)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<TaxInvoiceDto> getAPInvoicesBetweenDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return apInvoiceService.getAPInvoicesBetweenDates(startDate, endDate).stream()
                .map(TaxInvoiceDto::fromEntity)
                .collect(Collectors.toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaxInvoiceDto> updateAPInvoice(@PathVariable Long id, @Valid @RequestBody TaxInvoiceRequestDto requestDto) {
        try {
            TaxInvoice updatedInvoice = apInvoiceService.updateAPInvoice(id, requestDto);
            return ResponseEntity.ok(TaxInvoiceDto.fromEntity(updatedInvoice));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null); // Or a custom error response DTO
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAPInvoice(@PathVariable Long id) {
        try {
            apInvoiceService.deleteAPInvoice(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
