package com.ho.account.receivable.api.dto;

import com.ho.account.receivable.application.port.in.SalesInvoiceCommand;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 매출 인보이스 생성 HTTP 요청 DTO입니다.
 *
 * <p>초보자용 설명: 이 클래스는 외부 JSON을 검증하는 API 계약입니다. 검증이 끝난 값은 core의
 * {@link SalesInvoiceCommand}로 변환되어 매출채권 업무 규칙을 처리합니다.</p>
 */
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

    @NotBlank(message = "생성자 정보는 필수입니다.")
    private String createdBy;

    private String description;

    @AssertTrue(message = "만기일은 발행일보다 빠를 수 없습니다.")
    public boolean isDueDateNotBeforeIssueDate() {
        if (issueDate == null || dueDate == null) {
            return true;
        }
        return !dueDate.isBefore(issueDate);
    }

    @AssertTrue(message = "총액은 공급가액과 세액의 합계와 같아야 합니다.")
    public boolean isTotalAmountMatchesNetAndTax() {
        if (totalAmount == null || netAmount == null || taxAmount == null) {
            return true;
        }
        return totalAmount.compareTo(netAmount.add(taxAmount)) == 0;
    }

    public SalesInvoiceCommand toCommand() {
        return new SalesInvoiceCommand(
                invoiceNo,
                customerCode,
                issueDate,
                dueDate,
                totalAmount,
                taxAmount,
                netAmount,
                createdBy,
                description);
    }

    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }

    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}