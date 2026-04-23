package com.ho.account.asset.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class FixedAssetDisposalRequest {

    @NotNull(message = "처분할 고정자산 ID는 필수입니다.")
    private Long assetId;

    @NotNull(message = "처분일은 필수입니다.")
    private LocalDate disposalDate;

    @NotNull(message = "처분가액은 필수입니다.")
    @DecimalMin(value = "0.00", message = "처분가액은 0 이상이어야 합니다.")
    private BigDecimal salePrice;

    // Getter 및 Setter
    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public LocalDate getDisposalDate() {
        return disposalDate;
    }

    public void setDisposalDate(LocalDate disposalDate) {
        this.disposalDate = disposalDate;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }
}
