package com.ho.account.expenditure.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class APPaymentRequestDto {

    @NotNull(message = "지출결의 ID는 필수입니다.")
    private Long expenditureResolutionId;

    private Long taxInvoiceId; // Optional link to TaxInvoice

    @NotNull(message = "지급일시는 필수입니다.")
    private LocalDateTime paymentDate;

    @NotNull(message = "지급 금액은 필수입니다.")
    @DecimalMin(value = "0.0", inclusive = false, message = "지급 금액은 0보다 커야 합니다.")
    private BigDecimal amount;

    // unappliedAmount는 내부적으로 계산되므로 요청에서는 받지 않음.

    @NotBlank(message = "지급 방법은 필수입니다.")
    private String paymentMethod; // TRANSFER(이체), CASH(현금), CARD(카드)

    // status는 서비스 내부에서 관리하므로 요청에서는 받지 않음.

    // Getters and Setters
    public Long getExpenditureResolutionId() {
        return expenditureResolutionId;
    }

    public void setExpenditureResolutionId(Long expenditureResolutionId) {
        this.expenditureResolutionId = expenditureResolutionId;
    }

    public Long getTaxInvoiceId() {
        return taxInvoiceId;
    }

    public void setTaxInvoiceId(Long taxInvoiceId) {
        this.taxInvoiceId = taxInvoiceId;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
