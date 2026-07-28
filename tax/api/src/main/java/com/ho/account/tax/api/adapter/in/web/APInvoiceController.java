package com.ho.account.tax.api.adapter.in.web;

import com.ho.account.tax.api.dto.TaxInvoiceDto;
import com.ho.account.tax.api.dto.TaxInvoiceRequestDto;
import com.ho.account.tax.application.port.in.TaxInvoiceUseCase;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * [헥사고날 아키텍처 - 인바운드 어댑터 (Inbound Web Adapter)]
 * 매입 세금계산서(AP Invoice) 관련 REST API를 제공하는 웹 컨트롤러입니다.
 *
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 시스템의 '세금계산서 접수 데스크'입니다.
 * 외부 시스템이나 화면에서 "매입 세금계산서가 들어왔어요"라고 신고하면,
 * HTTP 요청을 검증한 뒤 core가 이해하는 `TaxInvoiceCommand`로 바꿔 내부 세무 서비스에 전달합니다.
 */
@RestController
@RequestMapping("/api/ap/invoices")
public class APInvoiceController {

    private final TaxInvoiceUseCase taxInvoiceUseCase;

    public APInvoiceController(TaxInvoiceUseCase taxInvoiceUseCase) {
        this.taxInvoiceUseCase = taxInvoiceUseCase;
    }

    @PostMapping
    public ResponseEntity<TaxInvoiceDto> createAPInvoice(@Valid @RequestBody TaxInvoiceRequestDto requestDto) {
        return ResponseEntity.ok(TaxInvoiceDto.fromEntity(taxInvoiceUseCase.createAPInvoice(requestDto.toCommand())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaxInvoiceDto> getAPInvoiceById(@PathVariable("id") Long id) {
        return taxInvoiceUseCase.getAPInvoiceById(id)
                .map(TaxInvoiceDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/issue-id/{issueId}")
    public ResponseEntity<TaxInvoiceDto> getAPInvoiceByIssueId(@PathVariable("issueId") String issueId) {
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
            @PathVariable("id") Long id, @Valid @RequestBody TaxInvoiceRequestDto requestDto) {
        return ResponseEntity.ok(TaxInvoiceDto.fromEntity(taxInvoiceUseCase.updateAPInvoice(id, requestDto.toCommand())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAPInvoice(
            @PathVariable("id") Long id,
            @RequestHeader("X-User-ID") String actor,
            @RequestParam String reason) {
        taxInvoiceUseCase.cancelAPInvoice(id, actor, reason);
        return ResponseEntity.noContent().build();
    }
}