package com.ho.account.asset.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class LeaseRemeasurementRequest {

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
