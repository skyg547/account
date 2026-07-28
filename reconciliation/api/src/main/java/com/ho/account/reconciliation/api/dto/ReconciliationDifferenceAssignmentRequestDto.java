package com.ho.account.reconciliation.api.dto;

import com.ho.account.reconciliation.application.port.in.AssignDifferenceCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

/** 대사 차이 담당자 배정 HTTP 요청 DTO입니다. */
@Data
public class ReconciliationDifferenceAssignmentRequestDto {
    @NotNull
    private Long differenceId;

    @NotBlank
    private String assignedToUser;

    @NotNull
    private LocalDateTime slaDueDate;

    public AssignDifferenceCommand toCommand() {
        return new AssignDifferenceCommand(differenceId, assignedToUser, slaDueDate);
    }
}