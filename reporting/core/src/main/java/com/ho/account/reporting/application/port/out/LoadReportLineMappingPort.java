package com.ho.account.reporting.application.port.out;

import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.ReportLineMapping;
import java.time.LocalDateTime;
import java.util.List;

public interface LoadReportLineMappingPort {

    List<ReportLineMapping> loadMappings(FinancialStatement.StatementType type, LocalDateTime baseDate);
}
