package com.ho.account.expenditure.payable.api.dto;

import com.ho.account.expenditure.application.port.in.AdvancePaymentCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 선급금 등록 요청 DTO입니다.
 *
 * <p>초보자용 설명: 선급금은 물건이나 서비스를 받기 전에 먼저 지급한 돈입니다.
 * HTTP 요청은 이 DTO에서 검증하고, 업무 처리는 core command로 변환한 뒤 진행합니다.</p>
 */
public class AdvancePaymentRequest {

    @NotBlank(message = "공급업체 코드는 필수입니다.")
    private String vendorCode;

    @NotNull(message = "선급금 지급일은 필수입니다.")
    private LocalDate paymentDate;

    @NotNull(message = "선급금 금액은 필수입니다.")
    @DecimalMin(value = "0.01", message = "선급금 금액은 0보다 커야 합니다.")
    private BigDecimal amount;

    private String description;

    public AdvancePaymentCommand toCommand() {
        return new AdvancePaymentCommand(vendorCode, paymentDate, amount, description);
    }

    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String vendorCode) { this.vendorCode = vendorCode; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}