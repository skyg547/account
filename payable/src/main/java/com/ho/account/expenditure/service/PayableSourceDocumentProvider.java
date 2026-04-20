package com.ho.account.expenditure.service;

import com.ho.account.contracts.source.SourceDocumentProvider;
import com.ho.account.shared.BoundedContext;
import com.ho.account.expenditure.repository.PurchaseInvoiceRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class PayableSourceDocumentProvider implements SourceDocumentProvider {

    private static final Set<String> SUPPORTED_TYPES = Set.of("P2P_AP");

    private final PurchaseInvoiceRepository purchaseInvoiceRepository;

    public PayableSourceDocumentProvider(PurchaseInvoiceRepository purchaseInvoiceRepository) {
        this.purchaseInvoiceRepository = purchaseInvoiceRepository;
    }

    @Override
    public Set<String> supportedLineageSourceTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public Optional<Map<String, Object>> getSourceDocument(String lineageSourceType, String lineageSourceId) {
        String[] parts = lineageSourceId.split("-");
        if (parts.length != 2) {
            return Optional.empty();
        }

        Map<String, Object> documentDetails = new HashMap<>();
        return purchaseInvoiceRepository.findByInvoiceNoAndVendorBusinessPartnerCode(parts[0], parts[1]).map(invoice -> {
            documentDetails.put("type", "PurchaseInvoice");
            documentDetails.put("data", invoice);
            return documentDetails;
        });
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
        return "Provides payable lineage documents from purchase invoices.";
    }
}
