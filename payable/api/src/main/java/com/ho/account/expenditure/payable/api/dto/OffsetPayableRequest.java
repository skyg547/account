package com.ho.account.expenditure.payable.api.dto;

import com.ho.account.expenditure.application.port.in.OffsetPayableCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 매입채무와 선급금 상계 요청 DTO입니다.
 *
 * <p>초보자용 설명: 상계는 "미리 준 돈으로 갚아야 할 돈을 줄이는 처리"입니다.
 * 어떤 채무와 선급금을 연결할지, 얼마를 상계할지, 재시도 작업 ID와 고정 회계일을
 * API 경계에서 먼저 검증합니다. 금액의 소수 정밀도는 core command가 다시 확인합니다.</p>
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

    @NotBlank(message = "상계 작업 ID는 필수입니다.")
    @Size(max = 60, message = "상계 작업 ID는 60자를 넘을 수 없습니다.")
    private String operationId;

    @NotNull(message = "상계 회계일은 필수입니다.")
    private LocalDate accountingDate;

    public OffsetPayableCommand toCommand() {
        return new OffsetPayableCommand(payableId, advancePaymentId, offsetAmount, operationId, accountingDate);
    }

    public Long getPayableId() { return payableId; }
    public void setPayableId(Long payableId) { this.payableId = payableId; }

    public Long getAdvancePaymentId() { return advancePaymentId; }
    public void setAdvancePaymentId(Long advancePaymentId) { this.advancePaymentId = advancePaymentId; }

    public BigDecimal getOffsetAmount() { return offsetAmount; }
    public void setOffsetAmount(BigDecimal offsetAmount) { this.offsetAmount = offsetAmount; }

    public String getOperationId() { return operationId; }
    public void setOperationId(String operationId) { this.operationId = operationId; }

    public LocalDate getAccountingDate() { return accountingDate; }
    public void setAccountingDate(LocalDate accountingDate) { this.accountingDate = accountingDate; }
}
