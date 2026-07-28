package com.ho.account.loan.dto;

// HTTP 요청 계약은 loan:api 인바운드 어댑터가 소유합니다.

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
    @Size(max = 20)
    private String deferredAssetAccountCode;

    @NotBlank
    @Size(max = 20)
    private String recognizedIncomeAccountCode;

    private boolean isActive = true;

    public DeferredItemType toEntity() {
        return DeferredItemType.create(
                code,
                name,
                description,
                deferralMethod,
                eirCashFlowTreatment,
                deferredAssetAccountCode,
                recognizedIncomeAccountCode,
                isActive,
                "SYSTEM");
    }
}
