package com.ho.account.loan.service;

import com.ho.account.contracts.source.SourceDocumentProvider;
import com.ho.account.shared.BoundedContext;
import com.ho.account.loan.infrastructure.persistence.LoanRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(110)
public class LoanSourceDocumentProvider implements SourceDocumentProvider {

    private static final Set<String> SUPPORTED_TYPES = Set.of("LOAN");

    private final LoanRepository loanRepository;

    public LoanSourceDocumentProvider(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    @Override
    public Set<String> supportedLineageSourceTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public Optional<Map<String, Object>> getSourceDocument(String lineageSourceType, String lineageSourceId) {
        try {
            Long id = Long.valueOf(lineageSourceId);
            Map<String, Object> documentDetails = new HashMap<>();
            return loanRepository.findById(id).map(contract -> {
                documentDetails.put("type", "Loan");
                documentDetails.put("data", contract);
                return documentDetails;
            });
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    @Override
    public String serviceName() {
        return "loan-source-document-provider";
    }

    @Override
    public BoundedContext boundedContext() {
        return BoundedContext.LOAN;
    }

    @Override
    public String description() {
        return "Provides loan lineage documents from loans.";
    }
}
