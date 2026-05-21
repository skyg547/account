package com.ho.account.reporting.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface RegulatoryFilingJpaRepository extends JpaRepository<RegulatoryFilingJpaEntity, Long> {

    Optional<RegulatoryFilingJpaEntity> findTopByStatementTypeAndBaseDateOrderBySubmittedAtDesc(
            String statementType,
            LocalDateTime baseDate);

    List<RegulatoryFilingJpaEntity> findByFilingIdOrderByDisplayOrderAsc(String filingId);
}
