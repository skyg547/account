package com.ho.account.expenditure.adapter.out.source;

import com.ho.account.contracts.source.SourceDocumentProvider;
import com.ho.account.expenditure.application.port.out.PurchaseInvoicePersistencePort;
import com.ho.account.expenditure.domain.PurchaseInvoice;
import com.ho.account.shared.BoundedContext;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class PayableSourceDocumentProvider implements SourceDocumentProvider {

    private static final String PURCHASE_INVOICE = "PURCHASE_INVOICE";
    private static final String P2P_AP = "P2P_AP";

    private final PurchaseInvoicePersistencePort purchaseInvoicePersistencePort;

    public PayableSourceDocumentProvider(PurchaseInvoicePersistencePort purchaseInvoicePersistencePort) {
        this.purchaseInvoicePersistencePort = purchaseInvoicePersistencePort;
    }

    @Override
    public Set<String> supportedLineageSourceTypes() {
        return Set.of(PURCHASE_INVOICE, P2P_AP);
    }

    @Override
    public Optional<Map<String, Object>> getSourceDocument(String lineageSourceType, String lineageSourceId) {
        if (!supports(lineageSourceType)) {
            return Optional.empty();
        }

        return parseInvoiceKey(lineageSourceId)
                .flatMap(key -> purchaseInvoicePersistencePort.findByInvoiceNoAndVendorCode(
                        key.invoiceNo(), key.vendorCode()))
                .map(this::cument);
    }

    @Override
    public String serviceName() {
        return "payable-source-document-provider";
    }

    @Override
    public BoundedContext boundedContext() {
        return BoundedContext.PAYABLE;
    }

    @Override
    public String description() {
        return "Provides payable purchase invoice source documents for journal drill-down.";
    }

    private Optional<InvoiceKey> parseInvoiceKey(String lineageSourceId) {
        if (lineageSourceId == null) {
            return Optional.empty();
        }

        int separator = lineageSourceId.lastIndexOf('_');
        if (separator <= 0 || separator == lineageSourceId.length() - 1) {
            return Optional.empty();
        }

        return Optional.of(new InvoiceKey(
                lineageSourceId.substring(0, separator),
                lineageSourceId.substring(separator + 1)));
    }

    private Map<String, Object> cument(PurchaseInvoice invoice) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("type", PURCHASE_INVOICE);
        document.put("id", invoice.getId());
        document.put("invoiceNo", invoice.getInvoiceNo());
        document.put("vendorCode", invoice.getVendorCode());
        document.put("issueDate", invoice.getIssueDate());
        document.put("dueDate", invoice.getDueDate());
        document.put("netAmount", invoice.getNetAmount());
        document.put("taxAmount", invoice.getTaxAmount());
        document.put("totalAmount", invoice.getTotalAmount());
        document.put("status", invoice.getStatus() != null ? invoice.getStatus().name() : null);
        document.put("journalEntryId", invoice.getJournalEntryId());
        document.put("description", invoice.getDescription());
        return document;
    }

    private record InvoiceKey(String invoiceNo, String vendorCode) {
    }
}
