package com.ho.account.receivable.api.dto;

import com.ho.account.receivable.domain.SalesInvoice;
import com.ho.account.receivable.domain.SalesInvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public class SalesInvoiceResponse {
    private Long id;
    private String invoiceNo;
    private String customerCode;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private BigDecimal netAmount;
    private SalesInvoiceStatus status;
    private String description;

    public static SalesInvoiceResponse fromEntity(SalesInvoice entity) {
        SalesInvoiceResponse response = new SalesInvoiceResponse();
        response.setId(entity.getId());
        response.setInvoiceNo(entity.getInvoiceNo());
        response.setCustomerCode(entity.getCustomerCode());
        response.setIssueDate(entity.getIssueDate());
        response.setDueDate(entity.getDueDate());
        response.setTotalAmount(entity.getTotalAmount());
        response.setTaxAmount(entity.getTaxAmount());
        response.setNetAmount(entity.getNetAmount());
        response.setStatus(entity.getStatus());
        response.setDescription(entity.getDescription());
        return response;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public SalesInvoiceStatus getStatus() { return status; }
    public void setStatus(SalesInvoiceStatus status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
