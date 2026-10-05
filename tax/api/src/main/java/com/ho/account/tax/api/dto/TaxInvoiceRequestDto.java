package com.ho.account.tax.api.dto;

import com.ho.account.tax.application.port.in.TaxInvoiceCommand;
import com.ho.account.tax.domain.TaxInvoice;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * HTTP 요청 DTO입니다.
 *
 * 초보자용 설명:
 * 이 클래스는 화면/외부 시스템에서 들어온 JSON을 먼저 검사하는 입구 전용 객체입니다.
 * core 업무 로직은 이 DTO를 직접 모르고, `toCommand()`로 변환된 업무 명령만 받습니다.
 */
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

    @AssertTrue(message = "금액은 NUMERIC(19,2)에 반올림 없이 저장할 수 있어야 합니다.")
    public boolean isAmountsStorable() {
        // null은 각 필드의 @NotNull에 맡기고, 입력 값은 도메인과 동일한 정밀도 규칙으로 확인한다.
        return (supplyAmount == null || TaxInvoice.isStorableAmount(supplyAmount))
                && (taxAmount == null || TaxInvoice.isStorableAmount(taxAmount))
                && (totalAmount == null || TaxInvoice.isStorableAmount(totalAmount));
    }

    public TaxInvoiceCommand toCommand() {
        return new TaxInvoiceCommand(
                issueId,
                type,
                issueDate,
                businessPartnerCode,
                supplyAmount,
                taxAmount,
                totalAmount);
    }

    public String getIssueId() { return issueId; }
    public void setIssueId(String issueId) { this.issueId = issueId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public String getBusinessPartnerCode() { return businessPartnerCode; }
    public void setBusinessPartnerCode(String businessPartnerCode) { this.businessPartnerCode = businessPartnerCode; }
    public BigDecimal getSupplyAmount() { return supplyAmount; }
    public void setSupplyAmount(BigDecimal supplyAmount) { this.supplyAmount = supplyAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
