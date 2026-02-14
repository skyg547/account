package com.ho.account.expenditure.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class OffsetPayableRequest {

    @NotNull(message = "상계할 매입채무 ID는 필수입니다.")
    private Long payableId;

    @NotNull(message = "상계에 사용할 선급금 ID는 필수입니다.")
    private Long advancePaymentId;

    @NotNull(message = "상계할 금액은 필수입니다.")
    @DecimalMin(value = "0.01", message = "상계할 금액은 0보다 커야 합니다.")
    private BigDecimal offsetAmount;

    // Getters and Setters
    public Long getPayableId() {
        return payableId;
    }

    public void setPayableId(Long payableId) {
        this.payableId = payableId;
    }

    public Long getAdvancePaymentId() {
        return advancePaymentId;
    }

    public void setAdvancePaymentId(Long advancePaymentId) {
        this.advancePaymentId = advancePaymentId;
    }

    public BigDecimal getOffsetAmount() {
        return offsetAmount;
    }

    public void setOffsetAmount(BigDecimal offsetAmount) {
        this.offsetAmount = offsetAmount;
    }
}
