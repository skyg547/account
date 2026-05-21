package com.ho.account.reporting.application.port.out;

import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.RegulatoryReportSubmission;
import java.time.LocalDateTime;
import java.util.Optional;

public interface LoadRegulatoryReportSubmissionPort {

    Optional<RegulatoryReportSubmission> findLatestReady(
            FinancialStatement.StatementType type,
            LocalDateTime baseDate);
}
