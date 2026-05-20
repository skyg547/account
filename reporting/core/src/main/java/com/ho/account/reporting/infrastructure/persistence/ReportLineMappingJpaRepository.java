package com.ho.account.reporting.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ReportLineMappingJpaRepository extends JpaRepository<ReportLineMappingJpaEntity, Long> {

    @Query("""
            select mapping
            from ReportLineMappingJpaEntity mapping
            where mapping.statementType = :statementType
              and mapping.validFrom <= :baseDate
              and (mapping.validTo is null or mapping.validTo >= :baseDate)
            order by mapping.displayOrder asc, mapping.lineCode asc, mapping.accountCode asc
            """)
    List<ReportLineMappingJpaEntity> findEffectiveRows(
            @Param("statementType") String statementType,
            @Param("baseDate") LocalDate baseDate);
}
