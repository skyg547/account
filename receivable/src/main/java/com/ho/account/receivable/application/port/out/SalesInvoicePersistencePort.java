package com.ho.account.receivable.application.port.out;

import com.ho.account.receivable.domain.SalesInvoice;
import java.util.Optional;

public interface SalesInvoicePersistencePort {
    SalesInvoice save(SalesInvoice invoice);
    Optional<SalesInvoice> findById(Long id);
}
