package com.ho.account.reporting.infrastructure.persistence;

import com.ho.account.reporting.application.port.out.StoreRegulatoryReportSubmissionPort;
import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.RegulatoryReportSubmission;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "account.reporting.persistence", name = "mode", havingValue = "jpa", matchIfMissing = true)
public class JpaRegulatoryReportSubmissionAdapter implements StoreRegulatoryReportSubmissionPort {

    private final RegulatoryReportSubmissionJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public int nextVersion(FinancialStatement.StatementType type, LocalDateTime baseDate) {
        Integer maxVersion = repository.findMaxVersion(type.name(), baseDate);
        return maxVersion == null ? 1 : maxVersion + 1;
    }

    @Override
    @Transactional
    public void save(RegulatoryReportSubmission submission) {
        repository.save(RegulatoryReportSubmissionJpaEntity.from(submission));
    }
}
