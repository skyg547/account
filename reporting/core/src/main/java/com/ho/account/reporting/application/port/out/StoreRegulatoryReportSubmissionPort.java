package com.ho.account.reporting.application.port.out;

import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.RegulatoryReportSubmission;
import java.time.LocalDateTime;

public interface StoreRegulatoryReportSubmissionPort {

    int nextVersion(FinancialStatement.StatementType type, LocalDateTime baseDate);

    void save(RegulatoryReportSubmission submission);
}
