package com.ho.account.reporting.infrastructure.persistence;

import com.ho.account.reporting.application.port.out.LoadRegulatoryFilingPort;
import com.ho.account.reporting.application.port.out.StoreRegulatoryFilingPort;
import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.RegulatoryFiling;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "account.reporting.persistence", name = "mode", havingValue = "memory")
public class InMemoryRegulatoryFilingAdapter implements StoreRegulatoryFilingPort, LoadRegulatoryFilingPort {

    private final Map<FilingKey, List<RegulatoryFiling>> filings = new ConcurrentHashMap<>();

    @Override
    public void save(RegulatoryFiling filing) {
        FilingKey key = new FilingKey(filing.getStatementType(), filing.getBaseDate());
        filings.compute(key, (ignored, existing) -> {
            List<RegulatoryFiling> next = existing == null
                    ? new java.util.ArrayList<>()
                    : new java.util.ArrayList<>(existing);
            next.add(filing);
            return List.copyOf(next);
        });
    }

    @Override
    public Optional<RegulatoryFiling> findLatest(FinancialStatement.StatementType type, LocalDateTime baseDate) {
        return filings.getOrDefault(new FilingKey(type, baseDate), List.of()).stream()
                .max(Comparator.comparing(RegulatoryFiling::getSubmittedAt));
    }

    private record FilingKey(
            FinancialStatement.StatementType type,
            LocalDateTime baseDate) {
    }
}
