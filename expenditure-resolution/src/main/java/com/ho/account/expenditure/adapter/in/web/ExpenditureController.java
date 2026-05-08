package com.ho.account.expenditure.adapter.in.web;

import com.ho.account.expenditure.application.port.in.ExpenditureResolutionUseCase;
import com.ho.account.expenditure.dto.ExpenditureResolutionDto;
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
    private final ExpenditureResolutionDtoAssembler dtoAssembler;

    public ExpenditureController(
            ExpenditureResolutionUseCase expenditureResolutionUseCase,
            ExpenditureResolutionDtoAssembler dtoAssembler) {
        this.expenditureResolutionUseCase = expenditureResolutionUseCase;
        this.dtoAssembler = dtoAssembler;
    }

    @PostMapping
    public ResponseEntity<ExpenditureResolutionDto> createResolution(
            @Valid @RequestBody ExpenditureResolutionRequestDto requestDto) {
        return ResponseEntity.ok(dtoAssembler.toDto(
                expenditureResolutionUseCase.createResolution(requestDto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenditureResolutionDto> updateResolution(
            @PathVariable("id") Long id, @Valid @RequestBody ExpenditureResolutionRequestDto requestDto) {
        return ResponseEntity.ok(dtoAssembler.toDto(
                expenditureResolutionUseCase.updateResolution(id, requestDto)));
    }

    @PostMapping("/{id}/request")
    public ResponseEntity<Void> requestApproval(@PathVariable("id") Long id) {
        expenditureResolutionUseCase.requestApproval(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approveResolution(@PathVariable("id") Long id) {
        expenditureResolutionUseCase.approveResolution(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectResolution(@PathVariable("id") Long id, @RequestBody Map<String, String> body) {
        expenditureResolutionUseCase.rejectResolution(id, body.get("reason"));
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public List<ExpenditureResolutionDto> getResolutions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return dtoAssembler.toDtoList(
                expenditureResolutionUseCase.getResolutionsByDate(startDate, endDate));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenditureResolutionDto> getResolution(@PathVariable("id") Long id) {
        return ResponseEntity.ok(dtoAssembler.toDto(
                expenditureResolutionUseCase.getResolution(id)));
    }
}
