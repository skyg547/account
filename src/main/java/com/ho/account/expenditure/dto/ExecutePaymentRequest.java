package com.ho.account.expenditure.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ExecutePaymentRequest {

    @NotNull(message = "지급 ID는 필수입니다.")
    private Long paymentId;

    @NotBlank(message = "지급 나갈 계좌 정보는 필수입니다.")
    private String bankAccount;

    // Getters and Setters
    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }
}
