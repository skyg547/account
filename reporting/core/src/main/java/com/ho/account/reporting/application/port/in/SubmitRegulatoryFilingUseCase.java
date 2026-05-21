package com.ho.account.reporting.application.port.in;

import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.RegulatoryFiling;
import java.time.LocalDateTime;
import java.util.Optional;

public interface SubmitRegulatoryFilingUseCase {

    RegulatoryFiling submit(SubmitCommand command);

    Optional<RegulatoryFiling> findLatest(FindLatestQuery query);

    record SubmitCommand(
            FinancialStatement.StatementType type,
            LocalDateTime baseDate,
            String requesterId,
            String targetAgency) {
    }

    record FindLatestQuery(
            FinancialStatement.StatementType type,
            LocalDateTime baseDate) {
    }
}
