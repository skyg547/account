package com.ho.account.contracts.tax;

public record TaxInvoiceRef(
        Long id,
        String issueId,
        String type) {
}
