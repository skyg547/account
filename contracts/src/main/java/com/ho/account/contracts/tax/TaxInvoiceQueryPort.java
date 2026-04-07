package com.ho.account.contracts.tax;

import java.util.Optional;

public interface TaxInvoiceQueryPort {

    Optional<TaxInvoiceRef> findById(Long taxInvoiceId);
}
