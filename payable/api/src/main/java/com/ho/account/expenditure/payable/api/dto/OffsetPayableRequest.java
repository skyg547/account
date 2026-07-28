package com.ho.account.expenditure.payable.api.dto;

import com.ho.account.expenditure.application.port.in.OffsetPayableCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * 매입채무와 선급금 상계 요청 DTO입니다.
 *
 * <p>초보자용 설명: 상계는 "미리 준 돈으로 갚아야 할 돈을 줄이는 처리"입니다.
 * 어떤 채무와 어떤 선급금을 연결할지, 얼마를 상계할지를 API 경계에서 먼저 검증합니다.</p>
 */
public class OffsetPayableRequest {

    @NotNull(message = "상계할 매입채무 ID는 필수입니다.")
    @Positive(message = "상계할 매입채무 ID는 0보다 커야 합니다.")
    private Long payableId;

    @NotNull(message = "상계에 사용할 선급금 ID는 필수입니다.")
    @Positive(message = "상계에 사용할 선급금 ID는 0보다 커야 합니다.")
    private Long advancePaymentId;

    @NotNull(message = "상계할 금액은 필수입니다.")
    @DecimalMin(value = "0.01", message = "상계할 금액은 0보다 커야 합니다.")
    private BigDecimal offsetAmount;

    public OffsetPayableCommand toCommand() {
        return new OffsetPayableCommand(payableId, advancePaymentId, offsetAmount);
    }

    public Long getPayableId() { return payableId; }
    public void setPayableId(Long payableId) { this.payableId = payableId; }

    public Long getAdvancePaymentId() { return advancePaymentId; }
    public void setAdvancePaymentId(Long advancePaymentId) { this.advancePaymentId = advancePaymentId; }

    public BigDecimal getOffsetAmount() { return offsetAmount; }
    public void setOffsetAmount(BigDecimal offsetAmount) { this.offsetAmount = offsetAmount; }
}