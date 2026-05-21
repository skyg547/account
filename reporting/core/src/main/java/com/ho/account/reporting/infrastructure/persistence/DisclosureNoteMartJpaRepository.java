package com.ho.account.reporting.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface DisclosureNoteMartJpaRepository extends JpaRepository<DisclosureNoteMartJpaEntity, Long> {

    List<DisclosureNoteMartJpaEntity> findByStatementTypeAndBaseDateOrderByNoteNumberAscSourceLineCodeAsc(
            String statementType,
            LocalDateTime baseDate);

    void deleteByStatementTypeAndBaseDate(String statementType, LocalDateTime baseDate);
}
