package com.ho.account.expenditure.payable.api.dto;

import com.ho.account.expenditure.application.port.in.ExecutePaymentCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 개별 지급 실행 요청 DTO입니다.
 *
 * <p>초보자용 설명: 실제 송금을 시도하려면 어떤 지급 건인지(paymentId)와
 * 어느 은행 계좌에서 나갈지(bankAccount)가 반드시 필요합니다.</p>
 */
public class ExecutePaymentRequest {

    @NotNull(message = "지급 ID는 필수입니다.")
    @Positive(message = "지급 ID는 0보다 커야 합니다.")
    private Long paymentId;

    @NotBlank(message = "지급 나갈 계좌 정보는 필수입니다.")
    private String bankAccount;

    public ExecutePaymentCommand toCommand() {
        return new ExecutePaymentCommand(paymentId, bankAccount);
    }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
}