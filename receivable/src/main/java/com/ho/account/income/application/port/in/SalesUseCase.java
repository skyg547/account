package com.ho.account.income.application.port.in;

import com.ho.account.income.domain.SalesInvoice;
import java.time.LocalDate;

public interface SalesUseCase {
    SalesInvoice createSalesInvoice(SalesInvoice invoice);
    void updateReceivableStatus(LocalDate asOfDate);
}
