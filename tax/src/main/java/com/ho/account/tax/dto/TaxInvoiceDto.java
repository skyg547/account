package com.ho.account.tax.dto;

import com.ho.account.tax.domain.TaxInvoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [TaxInvoiceDto]
 * 세금계산서 정보를 전달하기 위한 데이터 전송 객체입니다.
 */
public class TaxInvoiceDto {

    private Long id;
    private String issueId;
    private String type; // SALES, PURCHASE
    private LocalDate issueDate;
    private String businessPartnerCode;
    private String businessPartnerName;
    private BigDecimal supplyAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private TaxInvoice.TaxInvoiceStatus status;
    private String cancelledBy;
    private String cancellationReason;
    private LocalDateTime cancelledAt;

    public TaxInvoiceDto() {
    }

    public TaxInvoiceDto(Long id, String issueId, String type, LocalDate issueDate, String businessPartnerCode, String businessPartnerName, BigDecimal supplyAmount, BigDecimal taxAmount, BigDecimal totalAmount) {
        this.id = id;
        this.issueId = issueId;
        this.type = type;
        this.issueDate = issueDate;
        this.businessPartnerCode = businessPartnerCode;
        this.businessPartnerName = businessPartnerName;
        this.supplyAmount = supplyAmount;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
    }

    public static TaxInvoiceDto fromEntity(TaxInvoice taxInvoice) {
        TaxInvoiceDto dto = new TaxInvoiceDto(
                taxInvoice.getId(),
                taxInvoice.getIssueId(),
                taxInvoice.getType(),
                taxInvoice.getIssueDate(),
                taxInvoice.getBusinessPartnerCode(), // ID 기반 필드 사용
                null, // 이름 정보는 필요 시 Service 레이어에서 매핑 권장
                taxInvoice.getSupplyAmount(),
                taxInvoice.getTaxAmount(),
                taxInvoice.getTotalAmount());
        dto.status = taxInvoice.getStatus();
        dto.cancelledBy = taxInvoice.getCancelledBy();
        dto.cancellationReason = taxInvoice.getCancellationReason();
        dto.cancelledAt = taxInvoice.getCancelledAt();
        return dto;
    }

    // Getter
    public Long getId() { return id; }
    public String getIssueId() { return issueId; }
    public String getType() { return type; }
    public LocalDate getIssueDate() { return issueDate; }
    public String getBusinessPartnerCode() { return businessPartnerCode; }
    public String getBusinessPartnerName() { return businessPartnerName; }
    public BigDecimal getSupplyAmount() { return supplyAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public TaxInvoice.TaxInvoiceStatus getStatus() { return status; }
    public String getCancelledBy() { return cancelledBy; }
    public String getCancellationReason() { return cancellationReason; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
}
