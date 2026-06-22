package com.ho.account.reconciliation.dto;

import com.ho.account.reconciliation.domain.DifferenceReasonCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 차이 사유 코드 (DifferenceReasonCode) 요청 DTO
 */
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

    public DifferenceReasonCode toEntity() {
        DifferenceReasonCode reasonCode = new DifferenceReasonCode();
        reasonCode.setCode(this.code);
        reasonCode.setName(this.name);
        reasonCode.setDescription(this.description);
        reasonCode.setAdjustable(this.isAdjustable);
        reasonCode.setActive(this.isActive);
        return reasonCode;
    }
}
