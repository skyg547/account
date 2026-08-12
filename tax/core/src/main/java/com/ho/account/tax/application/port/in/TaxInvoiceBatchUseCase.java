package com.ho.account.tax.application.port.in;

import java.time.LocalDate;

public interface TaxInvoiceBatchUseCase {

    TaxInvoiceValidationResult validatePurchaseInvoices(LocalDate startDate, LocalDate endDate);

    TaxInvoiceValidationResult validatePurchaseInvoices(LocalDate startDate, LocalDate endDate, int pageSize);

    record TaxInvoiceValidationResult(int scannedCount, int validatedCount) {
    }
}

