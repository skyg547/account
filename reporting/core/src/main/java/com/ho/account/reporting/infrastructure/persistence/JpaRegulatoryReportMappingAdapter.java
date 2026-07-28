package com.ho.account.reporting.infrastructure.persistence;

import com.ho.account.reporting.application.port.out.LoadRegulatoryReportMappingPort;
import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.RegulatoryReportMapping;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "account.reporting.persistence", name = "mode", havingValue = "jpa", matchIfMissing = true)
public class JpaRegulatoryReportMappingAdapter implements LoadRegulatoryReportMappingPort {

    private final RegulatoryReportMappingJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<RegulatoryReportMapping> loadMappings(
            FinancialStatement.StatementType type,
            LocalDateTime baseDate) {
        return repository.findEffectiveMappings(type.name(), baseDate.toLocalDate()).stream()
                .map(RegulatoryReportMappingJpaEntity::toDomain)
                .toList();
    }
}
