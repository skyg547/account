package com.ho.account.reporting.infrastructure.persistence;

import com.ho.account.reporting.application.port.out.LoadReportLineMappingPort;
import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.ReportLineMapping;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
@ConditionalOnProperty(prefix = "account.reporting.persistence", name = "mode", havingValue = "jpa", matchIfMissing = true)
public class JpaReportLineMappingAdapter implements LoadReportLineMappingPort {

    private final ReportLineMappingJpaRepository repository;

    @Override
    public List<ReportLineMapping> loadMappings(FinancialStatement.StatementType type, LocalDateTime baseDate) {
        LocalDate date = baseDate.toLocalDate();
        Map<MappingKey, List<ReportLineMappingJpaEntity>> grouped = repository
                .findEffectiveRows(type.name(), date)
                .stream()
                .collect(Collectors.groupingBy(
                        MappingKey::from,
                        LinkedHashMap::new,
                        Collectors.toList()));

        return grouped.entrySet().stream()
                .map(entry -> entry.getKey().toDomain(entry.getValue()))
                .toList();
    }

    private record MappingKey(
            String lineCode,
            String label,
            String noteNumber,
            int lineLevel,
            int displayOrder,
            LocalDate validFrom,
            LocalDate validTo) {

        private static MappingKey from(ReportLineMappingJpaEntity entity) {
            return new MappingKey(
                    entity.getLineCode(),
                    entity.getLabel(),
                    entity.getNoteNumber(),
                    entity.getLineLevel(),
                    entity.getDisplayOrder(),
                    entity.getValidFrom(),
                    entity.getValidTo());
        }

        private ReportLineMapping toDomain(List<ReportLineMappingJpaEntity> rows) {
            List<String> accountCodes = rows.stream()
                    .map(ReportLineMappingJpaEntity::getAccountCode)
                    .toList();
            return rows.get(0).toDomain(accountCodes);
        }
    }
}
