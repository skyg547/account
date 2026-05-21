package com.ho.account.reporting.infrastructure.persistence;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RegulatoryReportSubmissionJpaRepository
        extends JpaRepository<RegulatoryReportSubmissionJpaEntity, Long> {

    @Query("""
            select max(submission.submissionVersion)
            from RegulatoryReportSubmissionJpaEntity submission
            where submission.statementType = :statementType
              and submission.baseDate = :baseDate
            """)
    Integer findMaxVersion(
            @Param("statementType") String statementType,
            @Param("baseDate") LocalDateTime baseDate);
}
