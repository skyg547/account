package com.ho.account.reporting.application.port.in;

import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.RegulatoryReportSubmission;
import java.time.LocalDateTime;

public interface SubmitRegulatoryReportUseCase {

    RegulatoryReportSubmission submit(SubmitCommand command);

    record SubmitCommand(
            FinancialStatement.StatementType type,
            LocalDateTime baseDate,
            String requesterId,
            String correctionReason) {
    }
}
