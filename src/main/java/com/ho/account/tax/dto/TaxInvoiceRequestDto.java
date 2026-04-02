package com.ho.account.tax.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public class TaxInvoiceRequestDto {

    @NotBlank(message = "승인번호는 필수입니다.")
    private String issueId;

    @NotBlank(message = "세금계산서 타입은 필수입니다.")
    @Pattern(regexp = "SALES|PURCHASE", message = "타입은 'SALES' 또는 'PURCHASE'여야 합니다.")
    private String type; // SALES(매출), PURCHASE(매입)

    @NotNull(message = "작성일자는 필수입니다.")
    private LocalDate issueDate;

    @NotBlank(message = "거래처 코드는 필수입니다.")
    private String businessPartnerCode;

    @NotNull(message = "공급가액은 필수입니다.")
    @PositiveOrZero(message = "공급가액은 0 이상이어야 합니다.")
    private BigDecimal supplyAmount;

    @NotNull(message = "세액은 필수입니다.")
    @PositiveOrZero(message = "세액은 0 이상이어야 합니다.")
    private BigDecimal taxAmount;

    @NotNull(message = "합계금액은 필수입니다.")
    @PositiveOrZero(message = "합계금액은 0 이상이어야 합니다.")
    private BigDecimal totalAmount;

    // Getter 및 Setter
    public String getIssueId() {
        return issueId;
    }

    public void setIssueId(String issueId) {
        this.issueId = issueId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public String getBusinessPartnerCode() {
        return businessPartnerCode;
    }

    public void setBusinessPartnerCode(String businessPartnerCode) {
        this.businessPartnerCode = businessPartnerCode;
    }

    public BigDecimal getSupplyAmount() {
        return supplyAmount;
    }

    public void setSupplyAmount(BigDecimal supplyAmount) {
        this.supplyAmount = supplyAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
