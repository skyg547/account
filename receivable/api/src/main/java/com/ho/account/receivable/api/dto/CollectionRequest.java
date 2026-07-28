package com.ho.account.receivable.api.dto;

import com.ho.account.receivable.application.port.in.CollectionCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 수납 등록 HTTP 요청 DTO입니다.
 *
 * <p>초보자용 설명: 은행 입금 자료나 화면 입력은 이 DTO에서 검증하고, core에는 기술 독립적인
 * {@link CollectionCommand}로 전달합니다.</p>
 */
public class CollectionRequest {

    @NotNull(message = "수금일은 필수입니다.")
    private LocalDate collectionDate;

    @NotBlank(message = "고객 코드는 필수입니다.")
    private String customerCode;

    @NotNull(message = "수금액은 필수입니다.")
    @DecimalMin(value = "0.01", message = "수금액은 0보다 커야 합니다.")
    private BigDecimal amount;

    private String bankAccount;
    private String virtualAccount;
    private String referenceNo;

    public CollectionCommand toCommand() {
        return new CollectionCommand(collectionDate, customerCode, amount, bankAccount, virtualAccount, referenceNo);
    }

    public LocalDate getCollectionDate() { return collectionDate; }
    public void setCollectionDate(LocalDate collectionDate) { this.collectionDate = collectionDate; }

    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }

    public String getVirtualAccount() { return virtualAccount; }
    public void setVirtualAccount(String virtualAccount) { this.virtualAccount = virtualAccount; }

    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }
}