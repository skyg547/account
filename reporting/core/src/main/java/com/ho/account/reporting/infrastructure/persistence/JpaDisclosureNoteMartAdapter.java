package com.ho.account.reporting.infrastructure.persistence;

import com.ho.account.reporting.application.port.out.LoadDisclosureNoteMartPort;
import com.ho.account.reporting.application.port.out.StoreDisclosureNoteMartPort;
import com.ho.account.reporting.domain.model.DisclosureNoteMart;
import com.ho.account.reporting.domain.model.DisclosureNoteMartEntry;
import com.ho.account.reporting.domain.model.FinancialStatement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "account.reporting.persistence", name = "mode", havingValue = "jpa", matchIfMissing = true)
public class JpaDisclosureNoteMartAdapter implements LoadDisclosureNoteMartPort, StoreDisclosureNoteMartPort {

    private final DisclosureNoteMartJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<DisclosureNoteMart> find(FinancialStatement.StatementType type, LocalDateTime baseDate) {
        List<DisclosureNoteMartEntry> entries = repository
                .findByStatementTypeAndBaseDateOrderByNoteNumberAscSourceLineCodeAsc(type.name(), baseDate)
                .stream()
                .map(DisclosureNoteMartJpaEntity::toDomain)
                .toList();
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        DisclosureNoteMartEntry first = entries.get(0);
        return Optional.of(DisclosureNoteMart.restored(
                first.getMartId(),
                first.getStatementId(),
                first.getStatementType(),
                first.getBaseDate(),
                first.getGeneratedBy(),
                first.getGeneratedAt(),
                entries));
    }

    @Override
    @Transactional
    public void replace(DisclosureNoteMart mart) {
        repository.deleteByStatementTypeAndBaseDate(mart.getStatementType().name(), mart.getBaseDate());
        repository.flush();
        repository.saveAll(mart.getEntries().stream()
                .map(DisclosureNoteMartJpaEntity::from)
                .toList());
    }
}
