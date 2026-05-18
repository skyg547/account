package com.ho.account.receivable.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class ManualMatchingRequest {

    @NotNull(message = "수금 ID는 필수입니다.")
    private Long collectionId;

    @NotNull(message = "매칭할 매출채권 ID는 필수입니다.")
    private Long receivableId;

    @NotNull(message = "매칭 금액은 필수입니다.")
    @DecimalMin(value = "0.01", message = "매칭 금액은 0보다 커야 합니다.")
    @JsonAlias("amount")
    private BigDecimal matchingAmount;

    // Getter 및 Setter
    public Long getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(Long collectionId) {
        this.collectionId = collectionId;
    }

    public Long getReceivableId() {
        return receivableId;
    }

    public void setReceivableId(Long receivableId) {
        this.receivableId = receivableId;
    }

    public BigDecimal getMatchingAmount() {
        return matchingAmount;
    }

    public void setMatchingAmount(BigDecimal matchingAmount) {
        this.matchingAmount = matchingAmount;
    }
}
