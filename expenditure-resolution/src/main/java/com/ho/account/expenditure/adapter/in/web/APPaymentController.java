package com.ho.account.expenditure.adapter.in.web;

import com.ho.account.expenditure.application.port.in.APPaymentUseCase;
import com.ho.account.expenditure.domain.APPaymentStatus;
import com.ho.account.expenditure.dto.APPaymentDto;
import com.ho.account.expenditure.dto.APPaymentRequestDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ap/payments")
public class APPaymentController {

    private final APPaymentUseCase apPaymentUseCase;

    public APPaymentController(APPaymentUseCase apPaymentUseCase) {
        this.apPaymentUseCase = apPaymentUseCase;
    }

    @PostMapping
    public ResponseEntity<APPaymentDto> createAPPayment(@Valid @RequestBody APPaymentRequestDto requestDto) {
        return ResponseEntity.ok(APPaymentDto.fromEntity(apPaymentUseCase.createAPPayment(requestDto)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<APPaymentDto> getAPPaymentById(@PathVariable("id") Long id) {
        return apPaymentUseCase.getAPPaymentById(id)
                .map(APPaymentDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/by-expenditure/{expenditureResolutionId}")
    public List<APPaymentDto> getAPPaymentsByExpenditureResolution(
            @PathVariable("expenditureResolutionId") Long expenditureResolutionId) {
        return apPaymentUseCase.getAPPaymentsByExpenditureResolution(expenditureResolutionId).stream()
                .map(APPaymentDto::fromEntity)
                .collect(Collectors.toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<APPaymentDto> updateAPPayment(
            @PathVariable("id") Long id, @Valid @RequestBody APPaymentRequestDto requestDto) {
        return ResponseEntity.ok(APPaymentDto.fromEntity(apPaymentUseCase.updateAPPayment(id, requestDto)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<APPaymentDto> updateAPPaymentStatus(
            @PathVariable("id") Long id, @RequestBody String status) {
        APPaymentStatus apPaymentStatus = APPaymentStatus.valueOf(status.trim().toUpperCase());
        return ResponseEntity.ok(APPaymentDto.fromEntity(apPaymentUseCase.updateAPPaymentStatus(id, apPaymentStatus)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAPPayment(@PathVariable("id") Long id) {
        apPaymentUseCase.deleteAPPayment(id);
        return ResponseEntity.noContent().build();
    }
}
