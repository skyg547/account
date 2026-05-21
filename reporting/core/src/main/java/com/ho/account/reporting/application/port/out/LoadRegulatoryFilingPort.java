package com.ho.account.reporting.application.port.out;

import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.RegulatoryFiling;
import java.time.LocalDateTime;
import java.util.Optional;

public interface LoadRegulatoryFilingPort {

    Optional<RegulatoryFiling> findLatest(FinancialStatement.StatementType type, LocalDateTime baseDate);
}
