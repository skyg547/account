package com.ho.account.expenditure.adapter.in.web;

import com.ho.account.expenditure.application.port.in.ExpenditureResolutionUseCase;
import com.ho.account.expenditure.domain.ExpenditureResolution;
import com.ho.account.expenditure.dto.ExpenditureResolutionRequestDto;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenditures")
public class ExpenditureController {

    private final ExpenditureResolutionUseCase expenditureResolutionUseCase;

    public ExpenditureController(ExpenditureResolutionUseCase expenditureResolutionUseCase) {
        this.expenditureResolutionUseCase = expenditureResolutionUseCase;
    }

    @PostMapping
    public ResponseEntity<ExpenditureResolution> createResolution(
            @Valid @RequestBody ExpenditureResolutionRequestDto requestDto) {
        return ResponseEntity.ok(expenditureResolutionUseCase.createResolution(requestDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenditureResolution> updateResolution(
            @PathVariable Long id, @Valid @RequestBody ExpenditureResolutionRequestDto requestDto) {
        return ResponseEntity.ok(expenditureResolutionUseCase.updateResolution(id, requestDto));
    }

    @PostMapping("/{id}/request")
    public ResponseEntity<Void> requestApproval(@PathVariable Long id) {
        expenditureResolutionUseCase.requestApproval(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approveResolution(@PathVariable Long id) {
        expenditureResolutionUseCase.approveResolution(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectResolution(@PathVariable Long id, @RequestBody Map<String, String> body) {
        expenditureResolutionUseCase.rejectResolution(id, body.get("reason"));
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public List<ExpenditureResolution> getResolutions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return expenditureResolutionUseCase.getResolutionsByDate(startDate, endDate);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenditureResolution> getResolution(@PathVariable Long id) {
        return ResponseEntity.ok(expenditureResolutionUseCase.getResolution(id));
    }
}
