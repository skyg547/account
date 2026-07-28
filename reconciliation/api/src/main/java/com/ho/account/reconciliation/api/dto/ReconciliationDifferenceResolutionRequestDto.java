package com.ho.account.reconciliation.api.dto;

import com.ho.account.reconciliation.application.port.in.ResolveDifferenceCommand;
import com.ho.account.reconciliation.domain.ReconciliationDifference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 대사 차이 해결 HTTP 요청 DTO입니다.
 * 조정 가능한 사유 코드는 core에서 조정 전표 링크 필수 여부를 다시 검증합니다.
 */
@Data
public class ReconciliationDifferenceResolutionRequestDto {
    @NotNull
    private Long differenceId;

    @NotNull
    private Long reasonCodeId;

    private Long adjustmentJournalEntryId;

    @NotNull
    private ReconciliationDifference.ReconciliationDifferenceStatus status;

    @NotBlank
    private String resolvedBy;

    public ResolveDifferenceCommand toCommand() {
        return new ResolveDifferenceCommand(differenceId, reasonCodeId, adjustmentJournalEntryId, status, resolvedBy);
    }
}