package com.ho.account.reporting.infrastructure.persistence;

import com.ho.account.reporting.application.port.out.LoadRegulatoryReportSubmissionPort;
import com.ho.account.reporting.application.port.out.StoreRegulatoryReportSubmissionPort;
import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.RegulatoryReportSubmission;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "account.reporting.persistence", name = "mode", havingValue = "memory")
public class InMemoryRegulatoryReportSubmissionAdapter
        implements StoreRegulatoryReportSubmissionPort, LoadRegulatoryReportSubmissionPort {

    private final Map<SubmissionKey, List<RegulatoryReportSubmission>> submissions = new ConcurrentHashMap<>();

    @Override
    public int nextVersion(FinancialStatement.StatementType type, LocalDateTime baseDate) {
        return submissions.getOrDefault(new SubmissionKey(type, baseDate), List.of()).stream()
                .map(RegulatoryReportSubmission::getVersion)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;
    }

    @Override
    public void save(RegulatoryReportSubmission submission) {
        SubmissionKey key = new SubmissionKey(submission.getStatementType(), submission.getBaseDate());
        submissions.compute(key, (ignored, existing) -> {
            List<RegulatoryReportSubmission> next = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
            next.add(submission);
            return List.copyOf(next);
        });
    }

    @Override
    public Optional<RegulatoryReportSubmission> findLatestReady(
            FinancialStatement.StatementType type,
            LocalDateTime baseDate) {
        return submissions.getOrDefault(new SubmissionKey(type, baseDate), List.of()).stream()
                .filter(submission -> submission.getStatus() == RegulatoryReportSubmission.SubmissionStatus.READY)
                .max(Comparator.comparingInt(RegulatoryReportSubmission::getVersion));
    }

    private record SubmissionKey(
            FinancialStatement.StatementType type,
            LocalDateTime baseDate) {
    }
}
