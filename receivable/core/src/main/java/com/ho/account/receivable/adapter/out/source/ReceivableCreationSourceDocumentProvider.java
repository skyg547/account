package com.ho.account.receivable.adapter.out.source;

import com.ho.account.contracts.source.SourceDocumentProvider;
import com.ho.account.receivable.application.port.out.CollectionPersistencePort;
import com.ho.account.receivable.application.port.out.SalesInvoicePersistencePort;
import com.ho.account.receivable.application.port.out.SourceCreationPersistencePort;
import com.ho.account.shared.BoundedContext;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Business-key lineage resolves through the committed receipt, never through a rolled-back ID. */
@Component
@Transactional(readOnly = true)
public class ReceivableCreationSourceDocumentProvider implements SourceDocumentProvider {
    private final SourceCreationPersistencePort receipts;
    private final SalesInvoicePersistencePort invoices;
    private final CollectionPersistencePort collections;

    public ReceivableCreationSourceDocumentProvider(SourceCreationPersistencePort receipts,
            SalesInvoicePersistencePort invoices, CollectionPersistencePort collections) {
        this.receipts = receipts;
        this.invoices = invoices;
        this.collections = collections;
    }

    @Override public Set<String> supportedLineageSourceTypes() { return Set.of("SALES_INVOICE_REF", "COLLECTION_REF"); }
    @Override public String serviceName() { return "receivable-creation-source-document-provider"; }
    @Override public BoundedContext boundedContext() { return BoundedContext.RECEIVABLE; }
    @Override public String description() { return "Resolves stable receivable creation business keys."; }

    @Override
    public Optional<Map<String, Object>> getSourceDocument(String type, String key) {
        if (!supports(type) || key == null) return Optional.empty();
        return receipts.find(type, key).flatMap(receipt -> {
            if ("SALES_INVOICE_REF".equals(type)) {
                return invoices.findById(receipt.sourceId()).map(invoice -> Map.<String, Object>of(
                        "type", type, "id", invoice.getId(), "invoiceNo", invoice.getInvoiceNo(),
                        "customerCode", invoice.getCustomerCode(), "totalAmount", invoice.getTotalAmount()));
            }
            return collections.findById(receipt.sourceId()).map(collection -> Map.<String, Object>of(
                    "type", type, "id", collection.getId(), "referenceNo", key,
                    "customerCode", collection.getCustomerCode(), "amount", collection.getAmount()));
        });
    }
}
