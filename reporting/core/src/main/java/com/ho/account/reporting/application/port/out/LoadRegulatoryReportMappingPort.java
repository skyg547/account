package com.ho.account.reporting.application.port.out;

import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.RegulatoryReportMapping;
import java.time.LocalDateTime;
import java.util.List;

public interface LoadRegulatoryReportMappingPort {

    List<RegulatoryReportMapping> loadMappings(FinancialStatement.StatementType type, LocalDateTime baseDate);
}
