package com.ho.account.expenditure.payable.api.dto;

import com.ho.account.expenditure.application.port.in.PurchaseInvoiceCommand;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 매입 인보이스 생성 HTTP 요청 DTO입니다.
 *
 * <p>초보자용 설명: 컨트롤러는 외부에서 들어온 JSON을 바로 JPA 엔티티로 받지 않고,
 * 이 요청 객체에서 필수값과 금액 형식을 먼저 검사합니다. 검사를 통과한 값은
 * core 유즈케이스가 이해하는 {@link PurchaseInvoiceCommand}로 바꿔 전달합니다.</p>
 */
public class PurchaseInvoiceRequest {

    @NotBlank(message = "인보이스 번호는 필수입니다.")
    private String invoiceNo;

    @NotBlank(message = "공급업체 코드는 필수입니다.")
    private String vendorCode;

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

    public PurchaseInvoiceCommand toCommand() {
        return new PurchaseInvoiceCommand(
                invoiceNo,
                vendorCode,
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

    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String vendorCode) { this.vendorCode = vendorCode; }

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