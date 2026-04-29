package com.ho.account.income.application.port.out;

import com.ho.account.income.domain.SalesInvoice;
import java.util.Optional;

public interface SalesInvoicePersistencePort {
    SalesInvoice save(SalesInvoice invoice);
    Optional<SalesInvoice> findById(Long id);
}
