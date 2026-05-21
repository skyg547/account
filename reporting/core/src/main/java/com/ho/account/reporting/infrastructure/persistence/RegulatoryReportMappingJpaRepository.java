package com.ho.account.reporting.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RegulatoryReportMappingJpaRepository extends JpaRepository<RegulatoryReportMappingJpaEntity, Long> {

    @Query("""
            select mapping
            from RegulatoryReportMappingJpaEntity mapping
            where mapping.statementType = :statementType
              and mapping.validFrom <= :baseDate
              and (mapping.validTo is null or mapping.validTo >= :baseDate)
            order by mapping.displayOrder asc, mapping.reportCode asc, mapping.fieldCode asc
            """)
    List<RegulatoryReportMappingJpaEntity> findEffectiveMappings(
            @Param("statementType") String statementType,
            @Param("baseDate") LocalDate baseDate);
}
