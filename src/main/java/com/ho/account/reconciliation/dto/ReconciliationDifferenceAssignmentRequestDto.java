package com.ho.account.reconciliation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReconciliationDifferenceAssignmentRequestDto {
    @NotNull
    private Long differenceId;
    @NotBlank
    private String assignedToUser;
    @NotNull
    private LocalDateTime slaDueDate;
}
