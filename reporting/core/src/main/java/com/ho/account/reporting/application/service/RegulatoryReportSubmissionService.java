package com.ho.account.reporting.application.service;

import com.ho.account.reporting.application.port.in.SubmitRegulatoryReportUseCase;
import com.ho.account.reporting.application.port.out.LoadReportHistoryPort;
import com.ho.account.reporting.application.port.out.StoreRegulatoryReportSubmissionPort;
import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.RegulatoryReportSubmission;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RegulatoryReportSubmissionService implements SubmitRegulatoryReportUseCase {

    private final LoadReportHistoryPort loadReportHistoryPort;
    private final StoreRegulatoryReportSubmissionPort storeRegulatoryReportSubmissionPort;

    @Override
    public RegulatoryReportSubmission submit(SubmitCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        FinancialStatement statement = loadReportHistoryPort.findFinalizedStatement(command.type(), command.baseDate())
                .orElseThrow(() -> new IllegalStateException(
                        "Finalized statement snapshot is required before regulatory submission."));
        int version = storeRegulatoryReportSubmissionPort.nextVersion(command.type(), command.baseDate());
        RegulatoryReportSubmission submission = RegulatoryReportSubmission.ready(
                UUID.randomUUID().toString(),
                statement,
                version,
                command.requesterId(),
                command.correctionReason(),
                LocalDateTime.now());
        storeRegulatoryReportSubmissionPort.save(submission);
        return submission;
    }
}
