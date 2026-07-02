package com.ho.account.mart.core.infrastructure.persistence.jpa;

import com.ho.account.mart.core.application.port.out.OdsGeneralLedgerRepository;
import com.ho.account.mart.core.infrastructure.persistence.entity.ods.OdsGeneralLedgerEntity;
import com.ho.account.mart.core.infrastructure.persistence.entity.ods.OdsGeneralLedgerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JpaOdsGeneralLedgerRepository extends JpaRepository<OdsGeneralLedgerEntity, OdsGeneralLedgerId> {
    List<OdsGeneralLedgerEntity> findByBaseDate(LocalDate baseDate);

    @Query("""
            select e.glCode as subjectCode,
                   e.currency as currencyCode,
                   coalesce(sum(e.balance), 0) as balanceAmount
            from OdsGeneralLedgerEntity e
            where e.baseDate = :baseDate
            group by e.glCode, e.currency
            """)
    List<OdsGeneralLedgerRepository.SubjectCurrencyBalanceSummary> getBalanceSummaryByBaseDate(@Param("baseDate") LocalDate baseDate);
}
