package com.ho.account.reporting.application.port.out;

import com.ho.account.reporting.domain.model.DisclosureNoteMart;
import com.ho.account.reporting.domain.model.FinancialStatement;
import java.time.LocalDateTime;
import java.util.Optional;

public interface LoadDisclosureNoteMartPort {

    Optional<DisclosureNoteMart> find(FinancialStatement.StatementType type, LocalDateTime baseDate);
}
