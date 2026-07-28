package com.ho.account.reconciliation.api.dto;

import com.ho.account.reconciliation.application.port.in.ReconciliationUnitCommand;
import com.ho.account.reconciliation.domain.ReconciliationUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 대사 단위(ReconciliationUnit) HTTP 요청 DTO입니다.
 *
 * <p>초보자용 설명: 이 DTO는 화면/HTTP JSON 입력값을 검증합니다. 검증 후에는 core 업무 입력인
 * {@link ReconciliationUnitCommand}로 바꿔 서비스에 전달합니다.</p>
 */
@Data
public class ReconciliationUnitRequestDto {
    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    @NotNull
    private ReconciliationUnit.ReconciliationFrequency frequency;

    @NotNull
    private ReconciliationUnit.ReconciliationType reconciliationType;

    private String criteriaJson; // JSON string for flexible criteria

    private boolean isActive = true;

    public ReconciliationUnitCommand toCommand() {
        return new ReconciliationUnitCommand(name, description, frequency, reconciliationType, criteriaJson, isActive);
    }
}