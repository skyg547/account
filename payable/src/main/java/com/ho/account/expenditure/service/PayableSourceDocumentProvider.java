package com.ho.account.expenditure.service;

import com.ho.account.contracts.source.SourceDocumentProvider;
import com.ho.account.expenditure.repository.PurchaseInvoiceRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class PayableSourceDocumentProvider implements SourceDocumentProvider {

    private final PurchaseInvoiceRepository purchaseInvoiceRepository;

    public PayableSourceDocumentProvider(PurchaseInvoiceRepository purchaseInvoiceRepository) {
        this.purchaseInvoiceRepository = purchaseInvoiceRepository;
    }

    @Override
    public boolean supports(String lineageSourceType) {
        return "P2P_AP".equals(lineageSourceType);
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
}
