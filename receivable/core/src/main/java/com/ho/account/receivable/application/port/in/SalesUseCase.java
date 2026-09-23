package com.ho.account.receivable.application.port.in;

import com.ho.account.receivable.domain.SalesInvoice;
import java.time.LocalDate;
import java.util.List;

public interface SalesUseCase {
    SalesInvoice createSalesInvoice(SalesInvoiceCommand command);
    List<SalesInvoice> findInvoices(String status);
    SalesInvoice findInvoiceById(Long id);
    void updateReceivableStatus(LocalDate asOfDate);

    class InvalidSalesInvoiceStatusException extends IllegalArgumentException {
        public InvalidSalesInvoiceStatusException(String status, Throwable cause) {
            super("Unknown sales invoice status: " + status, cause);
        }
    }

    class SalesInvoiceNotFoundException extends RuntimeException {
        public SalesInvoiceNotFoundException(Long id) {
            super("Sales invoice not found: " + id);
        }
    }
}
