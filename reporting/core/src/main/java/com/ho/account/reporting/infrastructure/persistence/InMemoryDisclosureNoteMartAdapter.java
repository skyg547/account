package com.ho.account.reporting.infrastructure.persistence;

import com.ho.account.reporting.application.port.out.LoadDisclosureNoteMartPort;
import com.ho.account.reporting.application.port.out.StoreDisclosureNoteMartPort;
import com.ho.account.reporting.domain.model.DisclosureNoteMart;
import com.ho.account.reporting.domain.model.FinancialStatement;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "account.reporting.persistence", name = "mode", havingValue = "memory")
public class InMemoryDisclosureNoteMartAdapter implements LoadDisclosureNoteMartPort, StoreDisclosureNoteMartPort {

    private final Map<MartKey, DisclosureNoteMart> marts = new ConcurrentHashMap<>();

    @Override
    public Optional<DisclosureNoteMart> find(FinancialStatement.StatementType type, LocalDateTime baseDate) {
        return Optional.ofNullable(marts.get(new MartKey(type, baseDate)));
    }

    @Override
    public void replace(DisclosureNoteMart mart) {
        marts.put(new MartKey(mart.getStatementType(), mart.getBaseDate()), mart);
    }

    private record MartKey(
            FinancialStatement.StatementType type,
            LocalDateTime baseDate) {
    }
}
