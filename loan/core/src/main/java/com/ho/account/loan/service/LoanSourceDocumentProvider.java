package com.ho.account.loan.service;

import com.ho.account.contracts.source.SourceDocumentProvider;
import com.ho.account.loan.application.port.out.LoanPersistencePort;
import com.ho.account.loan.domain.Loan;
import com.ho.account.shared.BoundedContext;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(110)
public class LoanSourceDocumentProvider implements SourceDocumentProvider {

    private static final Set<String> SUPPORTED_TYPES = Set.of("LOAN");

    private final LoanPersistencePort persistencePort;

    public LoanSourceDocumentProvider(LoanPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    @Override
    public Set<String> supportedLineageSourceTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public Optional<Map<String, Object>> getSourceDocument(String lineageSourceType, String lineageSourceId) {
        if (!SUPPORTED_TYPES.contains(lineageSourceType)) {
            return Optional.empty();
        }
        try {
            Long id = Long.valueOf(lineageSourceId);
            if (id < 1) {
                return Optional.empty();
            }
            return persistencePort.findLoan(id).map(this::toVersionedDocument);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private Map<String, Object> toVersionedDocument(Loan loan) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", loan.getId());
        data.put("loanNumber", loan.getLoanNumber());
        data.put("businessPartnerId", loan.getBusinessPartnerId());
        data.put("currencyCode", loan.getCurrencyCode());
        data.put("loanType", loan.getLoanType().name());
        data.put("principalAmount", loan.getPrincipalAmount());
        data.put("outstandingPrincipal", loan.getOutstandingPrincipal());
        data.put("interestRate", loan.getInterestRate());
        data.put("disbursalDate", loan.getDisbursalDate());
        data.put("maturityDate", loan.getMaturityDate());
        data.put("status", loan.getStatus().name());
        if (loan.getCurrentEIR() != null) {
            data.put("currentEIR", loan.getCurrentEIR());
        }
        return Map.of(
                "type", "Loan",
                "schemaVersion", 1,
                "data", Map.copyOf(data));
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
