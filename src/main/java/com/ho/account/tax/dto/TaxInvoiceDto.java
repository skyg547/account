package com.ho.account.tax.dto;

import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.tax.domain.TaxInvoice;
import java.math.BigDecimal;
import java.time.LocalDate;

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
        String bpCode = null;
        String bpName = null;
        if (taxInvoice.getBusinessPartner() != null) {
            bpCode = taxInvoice.getBusinessPartner().getBusinessPartnerCode();
            bpName = taxInvoice.getBusinessPartner().getBusinessPartnerName();
        }
        return new TaxInvoiceDto(
                taxInvoice.getId(),
                taxInvoice.getIssueId(),
                taxInvoice.getType(),
                taxInvoice.getIssueDate(),
                bpCode,
                bpName,
                taxInvoice.getSupplyAmount(),
                taxInvoice.getTaxAmount(),
                taxInvoice.getTotalAmount()
        );
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getIssueId() {
        return issueId;
    }

    public String getType() {
        return type;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public String getBusinessPartnerCode() {
        return businessPartnerCode;
    }

    public String getBusinessPartnerName() {
        return businessPartnerName;
    }

    public BigDecimal getSupplyAmount() {
        return supplyAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
