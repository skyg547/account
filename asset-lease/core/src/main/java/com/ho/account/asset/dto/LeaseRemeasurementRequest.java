package com.ho.account.asset.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;

public class LeaseRemeasurementRequest {

    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{0,79}")
    private String operationId;

    public String getOperationId() { return operationId; }
    public void setOperationId(String operationId) { this.operationId = operationId; }


    @NotNull
    private Long contractId;

    @NotNull
    private LocalDate remeasurementDate;

    @DecimalMin(value = "0.01")
    private BigDecimal newMonthlyPayment; // 변경된 월 리스료 (nullable)

    private LocalDate newEndDate; // 변경된 리스 종료일 (nullable)

    @DecimalMin(value = "0.00")
    private BigDecimal newDiscountRate; // 변경된 할인율 (nullable)

    // Getter 및 Setter
    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public LocalDate getRemeasurementDate() {
        return remeasurementDate;
    }

    public void setRemeasurementDate(LocalDate remeasurementDate) {
        this.remeasurementDate = remeasurementDate;
    }

    public BigDecimal getNewMonthlyPayment() {
        return newMonthlyPayment;
    }

    public void setNewMonthlyPayment(BigDecimal newMonthlyPayment) {
        this.newMonthlyPayment = newMonthlyPayment;
    }

    public LocalDate getNewEndDate() {
        return newEndDate;
    }

    public void setNewEndDate(LocalDate newEndDate) {
        this.newEndDate = newEndDate;
    }

    public BigDecimal getNewDiscountRate() {
        return newDiscountRate;
    }

    public void setNewDiscountRate(BigDecimal newDiscountRate) {
        this.newDiscountRate = newDiscountRate;
    }
}
