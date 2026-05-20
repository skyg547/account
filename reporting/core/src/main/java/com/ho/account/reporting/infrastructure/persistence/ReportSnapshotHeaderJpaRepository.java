package com.ho.account.reporting.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface ReportSnapshotHeaderJpaRepository extends JpaRepository<ReportSnapshotHeaderJpaEntity, Long> {

    @EntityGraph(attributePaths = "details")
    Optional<ReportSnapshotHeaderJpaEntity> findByStatementTypeAndBaseDateAndStatus(
            String statementType,
            LocalDateTime baseDate,
            String status);

    void deleteByStatementTypeAndBaseDateAndStatus(String statementType, LocalDateTime baseDate, String status);
}
