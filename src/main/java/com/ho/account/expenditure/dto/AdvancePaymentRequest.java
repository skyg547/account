package com.ho.account.expenditure.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class AdvancePaymentRequest {

    @NotBlank(message = "공급업체 코드는 필수입니다.")
    private String vendorCode;

    @NotNull(message = "선급금 지급일은 필수입니다.")
    private LocalDate paymentDate;

    @NotNull(message = "선급금 금액은 필수입니다.")
    @DecimalMin(value = "0.01", message = "선급금 금액은 0보다 커야 합니다.")
    private BigDecimal amount;

    private String description;

    // Getters and Setters
    public String getVendorCode() {
        return vendorCode;
    }

    public void setVendorCode(String vendorCode) {
        this.vendorCode = vendorCode;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
