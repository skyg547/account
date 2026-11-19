package com.ho.account.expenditure.adapter.out.source;

import com.ho.account.contracts.source.SourceDocumentProvider;
import com.ho.account.expenditure.application.port.out.AdvancePaymentPersistencePort;
import com.ho.account.expenditure.application.port.out.SourceCreationPersistencePort;
import com.ho.account.shared.BoundedContext;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class AdvanceCreationSourceDocumentProvider implements SourceDocumentProvider {
    private final SourceCreationPersistencePort receipts;
    private final AdvancePaymentPersistencePort advances;

    public AdvanceCreationSourceDocumentProvider(SourceCreationPersistencePort receipts, AdvancePaymentPersistencePort advances) {
        this.receipts = receipts;
        this.advances = advances;
    }

    @Override public Set<String> supportedLineageSourceTypes() { return Set.of("ADVANCE_PAYMENT_REF"); }
    @Override public String serviceName() { return "advance-creation-source-document-provider"; }
    @Override public BoundedContext boundedContext() { return BoundedContext.PAYABLE; }
    @Override public String description() { return "Resolves stable advance creation business keys."; }

    @Override
    public Optional<Map<String, Object>> getSourceDocument(String type, String key) {
        if (!supports(type) || key == null) return Optional.empty();
        return receipts.find(type, key).flatMap(receipt -> advances.findById(receipt.sourceId()))
                .map(advance -> Map.of("type", type, "id", advance.getId(), "operationId", key,
                        "vendorCode", advance.getVendorCode(), "amount", advance.getAmount()));
    }
}
