package com.ho.account.tax.application.service;

import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.tax.application.port.in.TaxInvoiceBatchUseCase;
import com.ho.account.tax.application.port.out.TaxInvoicePersistencePort;
import com.ho.account.tax.domain.TaxInvoice;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TaxInvoiceBatchService implements TaxInvoiceBatchUseCase {

    private final TaxInvoicePersistencePort taxInvoicePersistencePort;
    private final MasterDataQueryPort masterDataQueryPort;

    public TaxInvoiceBatchService(
            TaxInvoicePersistencePort taxInvoicePersistencePort,
            MasterDataQueryPort masterDataQueryPort) {
        this.taxInvoicePersistencePort = taxInvoicePersistencePort;
        this.masterDataQueryPort = masterDataQueryPort;
    }

    @Override
    public TaxInvoiceValidationResult validatePurchaseInvoices(LocalDate startDate, LocalDate endDate) {
        List<TaxInvoice> invoices = taxInvoicePersistencePort.findByIssueDateBetween(startDate, endDate);
        int validated = 0;
        for (TaxInvoice invoice : invoices) {
            if (!invoice.isPurchaseType()) {
                continue;
            }
            invoice.validateAmounts();
            masterDataQueryPort.findBusinessPartner(invoice.getBusinessPartnerCode())
                    .orElseThrow(() -> new IllegalStateException(
                            "Business partner missing for tax invoice " + invoice.getIssueId()));
            validated++;
        }
        return new TaxInvoiceValidationResult(invoices.size(), validated);
    }
}
