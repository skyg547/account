package com.ho.account.reporting.infrastructure.persistence;

import com.ho.account.reporting.application.port.out.LoadReportLineMappingPort;
import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.ReportLineMapping;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "account.reporting.persistence", name = "mode", havingValue = "memory")
public class DefaultReportLineMappingAdapter implements LoadReportLineMappingPort {

    private static final LocalDate DEFAULT_VALID_FROM = LocalDate.of(2020, 1, 1);

    private final List<ReportLineMapping> mappings = List.of(
            new ReportLineMapping(
                    FinancialStatement.StatementType.BALANCE_SHEET,
                    "ASSET_CASH",
                    "현금 및 현금성자산",
                    List.of("101", "102"),
                    "3",
                    1,
                    10,
                    DEFAULT_VALID_FROM,
                    null),
            new ReportLineMapping(
                    FinancialStatement.StatementType.BALANCE_SHEET,
                    "LIABILITY_DEPOSIT",
                    "예수부채",
                    List.of("201", "202"),
                    "8",
                    1,
                    20,
                    DEFAULT_VALID_FROM,
                    null),
            new ReportLineMapping(
                    FinancialStatement.StatementType.INCOME_STATEMENT,
                    "INTEREST_INCOME",
                    "이자수익",
                    List.of("401"),
                    "12",
                    1,
                    10,
                    DEFAULT_VALID_FROM,
                    null),
            new ReportLineMapping(
                    FinancialStatement.StatementType.INCOME_STATEMENT,
                    "INTEREST_EXPENSE",
                    "이자비용",
                    List.of("501"),
                    "13",
                    1,
                    20,
                    DEFAULT_VALID_FROM,
                    null));

    @Override
    public List<ReportLineMapping> loadMappings(FinancialStatement.StatementType type, LocalDateTime baseDate) {
        LocalDate date = baseDate.toLocalDate();
        return mappings.stream()
                .filter(mapping -> mapping.statementType() == type)
                .filter(mapping -> mapping.isEffectiveAt(date))
                .sorted(Comparator.comparingInt(ReportLineMapping::displayOrder))
                .toList();
    }
}
