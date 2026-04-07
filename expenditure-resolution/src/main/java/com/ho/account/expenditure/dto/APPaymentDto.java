package com.ho.account.expenditure.dto;

import com.ho.account.expenditure.domain.APPayment;
import com.ho.account.tax.dto.TaxInvoiceDto;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class APPaymentDto {

    private Long id;
    private Long expenditureResolutionId;
    private Long taxInvoiceId;
    private String taxInvoiceIssueId;
    private LocalDateTime paymentDate;
    private BigDecimal amount;
    private BigDecimal unappliedAmount;
    private String paymentMethod;
    private String status;

    public APPaymentDto() {
    }

    public APPaymentDto(Long id, Long expenditureResolutionId, Long taxInvoiceId, String taxInvoiceIssueId, LocalDateTime paymentDate, BigDecimal amount, BigDecimal unappliedAmount, String paymentMethod, String status) {
        this.id = id;
        this.expenditureResolutionId = expenditureResolutionId;
        this.taxInvoiceId = taxInvoiceId;
        this.taxInvoiceIssueId = taxInvoiceIssueId;
        this.paymentDate = paymentDate;
        this.amount = amount;
        this.unappliedAmount = unappliedAmount;
        this.paymentMethod = paymentMethod;
        this.status = status;
    }

    public static APPaymentDto fromEntity(APPayment apPayment) {
        Long taxInvoiceId = null;
        String taxInvoiceIssueId = null;
        if (apPayment.getTaxInvoice() != null) {
            taxInvoiceId = apPayment.getTaxInvoice().getId();
            taxInvoiceIssueId = apPayment.getTaxInvoice().getIssueId();
        }
        return new APPaymentDto(
                apPayment.getId(),
                apPayment.getExpenditureResolution().getId(),
                taxInvoiceId,
                taxInvoiceIssueId,
                apPayment.getPaymentDate(),
                apPayment.getAmount(),
                apPayment.getUnappliedAmount(),
                apPayment.getPaymentMethod(),
                apPayment.getStatus()
        );
    }

    // Getter
    public Long getId() {
        return id;
    }

    public Long getExpenditureResolutionId() {
        return expenditureResolutionId;
    }

    public Long getTaxInvoiceId() {
        return taxInvoiceId;
    }

    public String getTaxInvoiceIssueId() {
        return taxInvoiceIssueId;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getUnappliedAmount() {
        return unappliedAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getStatus() {
        return status;
    }
}
