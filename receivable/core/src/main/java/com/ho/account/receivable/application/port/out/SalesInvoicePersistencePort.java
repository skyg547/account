package com.ho.account.receivable.application.port.out;

import com.ho.account.receivable.domain.SalesInvoice;
import com.ho.account.receivable.domain.SalesInvoiceStatus;
import java.util.List;
import java.util.Optional;

public interface SalesInvoicePersistencePort {
    SalesInvoice save(SalesInvoice invoice);
    List<SalesInvoice> findAll();
    List<SalesInvoice> findByStatus(SalesInvoiceStatus status);
    Optional<SalesInvoice> findById(Long id);
}
