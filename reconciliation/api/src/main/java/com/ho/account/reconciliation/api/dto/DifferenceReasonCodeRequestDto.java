package com.ho.account.reconciliation.api.dto;

import com.ho.account.reconciliation.application.port.in.DifferenceReasonCodeCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 차이 사유 코드(DifferenceReasonCode) HTTP 요청 DTO입니다. */
@Data
public class DifferenceReasonCodeRequestDto {
    @NotBlank
    @Size(max = 50)
    private String code;

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    private boolean isAdjustable;

    private boolean isActive = true;

    public DifferenceReasonCodeCommand toCommand() {
        return new DifferenceReasonCodeCommand(code, name, description, isAdjustable, isActive);
    }
}