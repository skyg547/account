package com.ho.account.receivable.adapter.out.source;

import com.ho.account.contracts.source.SourceDocumentProvider;
import com.ho.account.receivable.application.port.out.SalesInvoicePersistencePort;
import com.ho.account.receivable.domain.SalesInvoice;
import com.ho.account.shared.BoundedContext;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class ReceivableSourceDocumentProvider implements SourceDocumentProvider {

    private static final String SALES_INVOICE = "SALES_INVOICE";
    private static final String O2C_AR = "O2C_AR";
    private static final String SALES = "SALES";

    private final SalesInvoicePersistencePort salesInvoicePersistencePort;

    public ReceivableSourceDocumentProvider(SalesInvoicePersistencePort salesInvoicePersistencePort) {
        this.salesInvoicePersistencePort = salesInvoicePersistencePort;
    }

    @Override
    public Set<String> supportedLineageSourceTypes() {
        return Set.of(SALES_INVOICE, O2C_AR, SALES);
    }

    @Override
    public Optional<Map<String, Object>> getSourceDocument(String lineageSourceType, String lineageSourceId) {
        if (!supports(lineageSourceType)) {
            return Optional.empty();
        }

        return parseInvoiceId(lineageSourceId)
                .flatMap(salesInvoicePersistencePort::findById)
                .map(this::toDocument);
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
        return "Provides receivable sales invoice source documents for journal drill-down.";
    }

    private Optional<Long> parseInvoiceId(String lineageSourceId) {
        if (lineageSourceId == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(lineageSourceId));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private Map<String, Object> toDocument(SalesInvoice invoice) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("type", SALES_INVOICE);
        document.put("id", invoice.getId());
        document.put("invoiceNo", invoice.getInvoiceNo());
        document.put("customerCode", invoice.getCustomerCode());
        document.put("issueDate", invoice.getIssueDate());
        document.put("dueDate", invoice.getDueDate());
        document.put("netAmount", invoice.getNetAmount());
        document.put("taxAmount", invoice.getTaxAmount());
        document.put("totalAmount", invoice.getTotalAmount());
        document.put("status", invoice.getStatus() != null ? invoice.getStatus().name() : null);
        document.put("description", invoice.getDescription());
        return document;
    }
}
