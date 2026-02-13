package com.ho.account.reconciliation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 대사 차이 할당 (ReconciliationDifferenceAssignment) 요청 DTO
 */
@Data
public class ReconciliationDifferenceAssignmentRequestDto {
    @NotNull
    private Long differenceId;

    @NotBlank
    private String assignedToUser;

    @NotNull
    private LocalDateTime slaDueDate;
}
