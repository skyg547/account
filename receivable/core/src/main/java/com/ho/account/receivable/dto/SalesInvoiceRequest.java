package com.ho.account.receivable.dto;

import com.ho.account.receivable.domain.SalesInvoice;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class SalesInvoiceRequest {

    @NotBlank(message = "인보이스 번호는 필수입니다.")
    private String invoiceNo;

    @NotBlank(message = "고객 코드는 필수입니다.")
    private String customerCode;

    @NotNull(message = "발행일은 필수입니다.")
    private LocalDate issueDate;

    @NotNull(message = "만기일은 필수입니다.")
    private LocalDate dueDate;

    @NotNull(message = "총액은 필수입니다.")
    @DecimalMin(value = "0.01", message = "총액은 0보다 커야 합니다.")
    private BigDecimal totalAmount;

    @NotNull(message = "세액은 필수입니다.")
    @DecimalMin(value = "0.00", message = "세액은 0 이상이어야 합니다.")
    private BigDecimal taxAmount;

    @NotNull(message = "공급가액은 필수입니다.")
    @DecimalMin(value = "0.01", message = "공급가액은 0보다 커야 합니다.")
    private BigDecimal netAmount;

    private String description;
    private String createdBy;

    public SalesInvoice toEntity() {
        String creator = hasText(createdBy) ? createdBy : "SYSTEM";
        return SalesInvoice.create(
                invoiceNo,
                customerCode,
                issueDate,
                dueDate,
                netAmount,
                taxAmount,
                creator,
                description);
    }

    // Getter 및 Setter
    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public void setCustomerCode(String customerCode) {
        this.customerCode = customerCode;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) { // Corrected method name from voidSetTaxAmount to setTaxAmount
        this.taxAmount = taxAmount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
