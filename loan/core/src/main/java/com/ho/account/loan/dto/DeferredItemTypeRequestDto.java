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

    @NotNull
    private DeferredItemType.EirCashFlowTreatment eirCashFlowTreatment =
            DeferredItemType.EirCashFlowTreatment.CUSTOMER_FEE_INFLOW;

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
        type.setEirCashFlowTreatment(this.eirCashFlowTreatment);
        type.setActive(this.isActive);
        // AccountSubject는 서비스에서 설정
        return type;
    }
}
