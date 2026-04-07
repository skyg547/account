package com.ho.account.expenditure.web;

import com.ho.account.expenditure.dto.APPaymentDto;
import com.ho.account.expenditure.dto.APPaymentRequestDto;
import com.ho.account.expenditure.service.APPaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ap/payments") // Dedicated path for AP Payments
public class APPaymentController {

    private final APPaymentService apPaymentService;

    @Autowired
    public APPaymentController(APPaymentService apPaymentService) {
        this.apPaymentService = apPaymentService;
    }

    @PostMapping
    public ResponseEntity<APPaymentDto> createAPPayment(@Valid @RequestBody APPaymentRequestDto requestDto) {
        try {
            APPaymentDto createdPayment = APPaymentDto.fromEntity(apPaymentService.createAPPayment(requestDto));
            return ResponseEntity.ok(createdPayment);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null); // Or a custom error response DTO
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<APPaymentDto> getAPPaymentById(@PathVariable Long id) {
        return apPaymentService.getAPPaymentById(id)
                .map(APPaymentDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/by-expenditure/{expenditureResolutionId}")
    public List<APPaymentDto> getAPPaymentsByExpenditureResolution(@PathVariable Long expenditureResolutionId) {
        return apPaymentService.getAPPaymentsByExpenditureResolution(expenditureResolutionId).stream()
                .map(APPaymentDto::fromEntity)
                .collect(Collectors.toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<APPaymentDto> updateAPPayment(@PathVariable Long id,
            @Valid @RequestBody APPaymentRequestDto requestDto) {
        try {
            APPaymentDto updatedPayment = APPaymentDto.fromEntity(apPaymentService.updateAPPayment(id, requestDto));
            return ResponseEntity.ok(updatedPayment);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null); // Or a custom error response DTO
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<APPaymentDto> updateAPPaymentStatus(@PathVariable Long id, @RequestBody String status) {
        try {
            APPaymentDto updatedPayment = APPaymentDto.fromEntity(apPaymentService.updateAPPaymentStatus(id, status));
            return ResponseEntity.ok(updatedPayment);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAPPayment(@PathVariable Long id) {
        try {
            apPaymentService.deleteAPPayment(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
