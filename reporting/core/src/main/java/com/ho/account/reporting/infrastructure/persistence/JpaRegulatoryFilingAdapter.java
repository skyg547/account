package com.ho.account.reporting.infrastructure.persistence;

import com.ho.account.reporting.application.port.out.LoadRegulatoryFilingPort;
import com.ho.account.reporting.application.port.out.StoreRegulatoryFilingPort;
import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.RegulatoryFiling;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "account.reporting.persistence", name = "mode", havingValue = "jpa", matchIfMissing = true)
public class JpaRegulatoryFilingAdapter implements StoreRegulatoryFilingPort, LoadRegulatoryFilingPort {

    private final RegulatoryFilingJpaRepository repository;

    @Override
    @Transactional
    public void save(RegulatoryFiling filing) {
        repository.saveAll(RegulatoryFilingJpaEntity.from(filing));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RegulatoryFiling> findLatest(FinancialStatement.StatementType type, LocalDateTime baseDate) {
        return repository.findTopByStatementTypeAndBaseDateOrderBySubmittedAtDesc(type.name(), baseDate)
                .map(RegulatoryFilingJpaEntity::filingId)
                .map(repository::findByFilingIdOrderByDisplayOrderAsc)
                .map(RegulatoryFilingJpaEntity::main);
    }
}
