package com.ho.account.reporting.application.service;

import com.ho.account.reporting.application.port.in.DisclosureNoteMartUseCase;
import com.ho.account.reporting.application.port.out.LoadDisclosureNoteMartPort;
import com.ho.account.reporting.application.port.out.LoadReportHistoryPort;
import com.ho.account.reporting.application.port.out.StoreDisclosureNoteMartPort;
import com.ho.account.reporting.domain.model.DisclosureNoteMart;
import com.ho.account.reporting.domain.model.FinancialStatement;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
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
}
