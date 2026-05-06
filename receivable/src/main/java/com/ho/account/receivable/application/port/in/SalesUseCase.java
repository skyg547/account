package com.ho.account.receivable.application.port.in;

import com.ho.account.receivable.domain.SalesInvoice;
import java.time.LocalDate;

public interface SalesUseCase {
    SalesInvoice createSalesInvoice(SalesInvoice invoice);
    void updateReceivableStatus(LocalDate asOfDate);
}
