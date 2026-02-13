package com.ho.account.loan.dto;

import com.ho.account.loan.domain.DeferredItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 이연 항목 유형 (DeferredItemType) 요청 DTO
 */
@Data
public class DeferredItemTypeRequestDto {
    @NotBlank
    @Size(max = 100)
    private String code;

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 500)
    private String description;

    @NotNull
    private DeferredItemType.DeferralMethod deferralMethod;

    @NotBlank
    private String deferredAssetAccountCode;

    @NotBlank
    private String recognizedIncomeAccountCode;

    private boolean isActive = true;

    public DeferredItemType toEntity() {
        DeferredItemType type = new DeferredItemType();
        type.setCode(this.code);
        type.setName(this.name);
        type.setDescription(this.description);
        type.setDeferralMethod(this.deferralMethod);
        type.setActive(this.isActive);
        // AccountSubjects will be set in service
        return type;
    }
}
