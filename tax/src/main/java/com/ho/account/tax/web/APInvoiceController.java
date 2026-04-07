package com.ho.account.tax.web;

import com.ho.account.tax.domain.TaxInvoice;
import com.ho.account.tax.dto.TaxInvoiceDto;
import com.ho.account.tax.dto.TaxInvoiceRequestDto;
import com.ho.account.tax.service.APInvoiceService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ap/invoices")
public class APInvoiceController {

    private final APInvoiceService apInvoiceService;

    public APInvoiceController(APInvoiceService apInvoiceService) {
        this.apInvoiceService = apInvoiceService;
    }

    @PostMapping
    public ResponseEntity<TaxInvoiceDto> createAPInvoice(@Valid @RequestBody TaxInvoiceRequestDto requestDto) {
        try {
            TaxInvoice createdInvoice = apInvoiceService.createAPInvoice(requestDto);
            return ResponseEntity.ok(TaxInvoiceDto.fromEntity(createdInvoice));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
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
    public ResponseEntity<TaxInvoiceDto> updateAPInvoice(@PathVariable Long id,
                                                         @Valid @RequestBody TaxInvoiceRequestDto requestDto) {
        try {
            TaxInvoice updatedInvoice = apInvoiceService.updateAPInvoice(id, requestDto);
            return ResponseEntity.ok(TaxInvoiceDto.fromEntity(updatedInvoice));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
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
