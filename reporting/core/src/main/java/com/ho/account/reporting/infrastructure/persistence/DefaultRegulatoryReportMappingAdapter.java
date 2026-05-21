package com.ho.account.reporting.infrastructure.persistence;

import com.ho.account.reporting.application.port.out.LoadRegulatoryReportMappingPort;
import com.ho.account.reporting.domain.model.DisclosureNoteMartEntry.NoteCategory;
import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.RegulatoryReportMapping;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "account.reporting.persistence", name = "mode", havingValue = "memory")
public class DefaultRegulatoryReportMappingAdapter implements LoadRegulatoryReportMappingPort {

    private static final LocalDate DEFAULT_VALID_FROM = LocalDate.of(2020, 1, 1);

    private final List<RegulatoryReportMapping> mappings = List.of(
            new RegulatoryReportMapping(
                    FinancialStatement.StatementType.BALANCE_SHEET,
                    "FSS",
                    "FSS_BS_DISCLOSURE",
                    "CASH_AND_CASH_EQUIVALENTS",
                    "현금 및 현금성자산",
                    NoteCategory.CURRENCY,
                    "3",
                    "ASSET_CASH",
                    true,
                    10,
                    DEFAULT_VALID_FROM,
                    null),
            new RegulatoryReportMapping(
                    FinancialStatement.StatementType.BALANCE_SHEET,
                    "FSS",
                    "FSS_BS_DISCLOSURE",
                    "DEPOSIT_LIABILITIES",
                    "예수부채",
                    NoteCategory.MATURITY,
                    "8",
                    "LIABILITY_DEPOSIT",
                    true,
                    20,
                    DEFAULT_VALID_FROM,
                    null),
            new RegulatoryReportMapping(
                    FinancialStatement.StatementType.INCOME_STATEMENT,
                    "FSS",
                    "FSS_IS_RATE",
                    "INTEREST_INCOME",
                    "이자수익",
                    NoteCategory.INTEREST_RATE,
                    "12",
                    "INTEREST_INCOME",
                    true,
                    10,
                    DEFAULT_VALID_FROM,
                    null),
            new RegulatoryReportMapping(
                    FinancialStatement.StatementType.INCOME_STATEMENT,
                    "FSS",
                    "FSS_IS_RATE",
                    "INTEREST_EXPENSE",
                    "이자비용",
                    NoteCategory.INTEREST_RATE,
                    "13",
                    "INTEREST_EXPENSE",
                    true,
                    20,
                    DEFAULT_VALID_FROM,
                    null));

    @Override
    public List<RegulatoryReportMapping> loadMappings(
            FinancialStatement.StatementType type,
            LocalDateTime baseDate) {
        LocalDate date = baseDate.toLocalDate();
        return mappings.stream()
                .filter(mapping -> mapping.statementType() == type)
                .filter(mapping -> mapping.isEffectiveAt(date))
                .sorted(Comparator.comparingInt(RegulatoryReportMapping::displayOrder))
                .toList();
    }
}
