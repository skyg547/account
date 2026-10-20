package com.ho.account.masterdata.core.infrastructure.persistence.repository;

import com.ho.account.masterdata.core.infrastructure.persistence.BusinessPartnerScreeningAuditJpaEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessPartnerScreeningAuditRepository
        extends JpaRepository<BusinessPartnerScreeningAuditJpaEntity, Long> {

    /** PostgreSQL/H2 apply Pageable as SQL LIMIT; history is not materialized in the application. */
    @Query("""
            SELECT audit FROM BusinessPartnerScreeningAuditJpaEntity audit
            WHERE audit.businessPartnerCode = :code
              AND COALESCE(audit.decidedAt, audit.submittedAt) < :endExclusive
            ORDER BY COALESCE(audit.decidedAt, audit.submittedAt) DESC, audit.id DESC
            """)
    List<BusinessPartnerScreeningAuditJpaEntity> findLatestBefore(
            @Param("code") String code,
            @Param("endExclusive") LocalDateTime endExclusive,
            Pageable page);
}
