package com.ho.account.reporting.application.port.in;

import com.ho.account.reporting.domain.model.DisclosureNoteMart;
import com.ho.account.reporting.domain.model.FinancialStatement;
import java.time.LocalDateTime;
import java.util.Optional;

public interface DisclosureNoteMartUseCase {

    DisclosureNoteMart generate(GenerateCommand command);

    Optional<DisclosureNoteMart> find(FindQuery query);

    record GenerateCommand(
            FinancialStatement.StatementType type,
            LocalDateTime baseDate,
            String requesterId) {
    }

    record FindQuery(
            FinancialStatement.StatementType type,
            LocalDateTime baseDate) {
    }
}
