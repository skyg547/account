package com.ho.account.closing.dto;

import com.ho.account.closing.domain.ClosingGate; // Added import
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 결산 게이트 (ClosingGate) 요청 DTO
 */
@Data
public class ClosingGateRequestDto {
    @NotNull
    private Long calendarId;

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    private String checkConditionJson; // JSON string for condition definition

    public ClosingGate toEntity() {
        ClosingGate gate = new ClosingGate();
        // ClosingCalendar will be set in service layer
        gate.setName(this.name);
        gate.setDescription(this.description);
        gate.setCheckConditionJson(this.checkConditionJson);
        return gate;
    }
}
