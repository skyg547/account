package com.ho.account.income.service;

import com.ho.account.contracts.source.SourceDocumentProvider;
import com.ho.account.shared.BoundedContext;
import com.ho.account.income.repository.SalesInvoiceRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class ReceivableSourceDocumentProvider implements SourceDocumentProvider {

    private static final Set<String> SUPPORTED_TYPES = Set.of("O2C_AR", "SALES", "SALES_INVOICE");

    private final SalesInvoiceRepository salesInvoiceRepository;

    public ReceivableSourceDocumentProvider(SalesInvoiceRepository salesInvoiceRepository) {
        this.salesInvoiceRepository = salesInvoiceRepository;
    }

    @Override
    public Set<String> supportedLineageSourceTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public Optional<Map<String, Object>> getSourceDocument(String lineageSourceType, String lineageSourceId) {
        Map<String, Object> documentDetails = new HashMap<>();
        documentDetails.put("type", "SalesInvoice");

        try {
            if ("O2C_AR".equals(lineageSourceType)) {
                Long id = Long.valueOf(lineageSourceId);
                return salesInvoiceRepository.findById(id).map(invoice -> {
                    documentDetails.put("data", invoice);
                    return documentDetails;
                });
            }

            return salesInvoiceRepository.findByInvoiceNo(lineageSourceId)
                    .<Map<String, Object>>map(invoice -> {
                        documentDetails.put("data", invoice);
                        return documentDetails;
                    })
                    .or(() -> Optional.of(documentDetails));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    @Override
    public String serviceName() {
        return "receivable-source-document-provider";
    }

    @Override
    public BoundedContext boundedContext() {
        return BoundedContext.RECEIVABLE;
    }

    @Override
    public String description() {
        return "Provides receivable lineage documents from sales invoices.";
    }
}
