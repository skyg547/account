package com.ho.account.reconciliation.api.dto;

import com.ho.account.reconciliation.application.port.in.RunReconciliationCommand;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

/** 대사 실행(ReconciliationRun) HTTP 요청 DTO입니다. */
@Data
public class ReconciliationRunRequestDto {
    @NotNull
    private Long reconciliationUnitId;

    @NotNull
    private LocalDate reconciliationDate;

    public RunReconciliationCommand toCommand(String auditUser) {
        return new RunReconciliationCommand(reconciliationUnitId, reconciliationDate, auditUser);
    }
}