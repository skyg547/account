package com.ho.account.reporting.application.service;

import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.reporting.application.port.in.DisclosureNoteMartUseCase;
import com.ho.account.reporting.application.port.out.LoadDisclosureNoteMartPort;
import com.ho.account.reporting.application.port.out.LoadReportHistoryPort;
import com.ho.account.reporting.application.port.out.LoadReportLineMappingPort;
import com.ho.account.reporting.application.port.out.StoreDisclosureNoteMartPort;
import com.ho.account.reporting.domain.model.DisclosureNoteMart;
import com.ho.account.reporting.domain.model.DisclosureNoteMartEntry;
import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.ReportLineMapping;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DisclosureNoteMartService implements DisclosureNoteMartUseCase {

    private final LoadReportHistoryPort loadReportHistoryPort;
    private final StoreDisclosureNoteMartPort storeDisclosureNoteMartPort;
    private final LoadDisclosureNoteMartPort loadDisclosureNoteMartPort;
    private final LoadReportLineMappingPort loadReportLineMappingPort;
    private final JournalQueryPort journalQueryPort;

    @Override
    public DisclosureNoteMart generate(GenerateCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        FinancialStatement statement = loadReportHistoryPort.findFinalizedStatement(command.type(), command.baseDate())
                .orElseThrow(() -> new IllegalStateException(
                        "Finalized statement snapshot is required before disclosure note mart generation."));
        DisclosureNoteMart mart = DisclosureNoteMart.fromStatement(
                statement,
                command.requesterId(),
                LocalDateTime.now());
        storeDisclosureNoteMartPort.replace(mart);
        return mart;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DisclosureNoteMart> find(FindQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        return loadDisclosureNoteMartPort.find(query.type(), query.baseDate());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JournalDetailSummary> drillDown(DrillDownQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        DisclosureNoteMart mart = loadDisclosureNoteMartPort.find(query.type(), query.baseDate())
                .orElseThrow(() -> new IllegalArgumentException("Disclosure Note Mart not found for the given criteria."));

        DisclosureNoteMartEntry targetEntry = mart.getEntries().stream()
                .filter(entry -> entry.getEntryId().equals(query.entryId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Entry ID not found in the Disclosure Note Mart."));

        List<ReportLineMapping> mappings = loadReportLineMappingPort.loadMappings(query.type(), query.baseDate());
        
        List<String> accountCodes = mappings.stream()
                .filter(mapping -> mapping.lineCode().equals(targetEntry.getSourceLineCode()))
                .flatMap(mapping -> mapping.accountCodes().stream())
                .distinct()
                .collect(Collectors.toList());

        if (accountCodes.isEmpty()) {
            return List.of();
        }

        // 해당 주석이 속한 회계 기간(해당 월의 1일부터 기준일까지)의 전표를 조회한다고 가정
        // Reporting 모듈에서는 BaseDate가 월말 결산 기준이므로, 시작일을 해당 월의 1일로 설정
        java.time.LocalDate endDate = query.baseDate().toLocalDate();
        java.time.LocalDate startDate = endDate.withDayOfMonth(1);

        return journalQueryPort.getJournalDetailsByAccountCodes(startDate, endDate, accountCodes);
    }
}
