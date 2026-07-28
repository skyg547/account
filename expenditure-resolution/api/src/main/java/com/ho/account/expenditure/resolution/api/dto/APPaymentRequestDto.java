package com.ho.account.expenditure.resolution.api.dto;

import com.ho.account.expenditure.application.port.in.APPaymentCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AP 지급 HTTP 요청 DTO입니다. 요청 검증 후 core `APPaymentCommand`로 변환됩니다.
 */
public class APPaymentRequestDto {

    @NotNull(message = "지출결의 ID는 필수입니다.")
    private Long expenditureResolutionId;

    private Long taxInvoiceId;

    @NotNull(message = "지급일시는 필수입니다.")
    private LocalDateTime paymentDate;

    @NotNull(message = "지급 금액은 필수입니다.")
    @DecimalMin(value = "0.0", inclusive = false, message = "지급 금액은 0보다 커야 합니다.")
    private BigDecimal amount;

    @NotBlank(message = "지급 방법은 필수입니다.")
    private String paymentMethod;

    public APPaymentCommand toCommand() {
        return new APPaymentCommand(
                expenditureResolutionId,
                taxInvoiceId,
                paymentDate,
                amount,
                paymentMethod);
    }

    public Long getExpenditureResolutionId() { return expenditureResolutionId; }
    public void setExpenditureResolutionId(Long expenditureResolutionId) { this.expenditureResolutionId = expenditureResolutionId; }
    public Long getTaxInvoiceId() { return taxInvoiceId; }
    public void setTaxInvoiceId(Long taxInvoiceId) { this.taxInvoiceId = taxInvoiceId; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}