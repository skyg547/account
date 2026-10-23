package com.ho.account.masterdata.core.infrastructure.persistence.repository;

import com.ho.account.masterdata.core.infrastructure.persistence.entity.FiscalPeriodTransitionEntity;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FiscalPeriodTransitionRepository extends JpaRepository<FiscalPeriodTransitionEntity, String> {
    /** One SQL statement observes the period and admission fences in the same database snapshot. */
    interface AdmissionView {
        Long getId();
        String getFiscalYear();
        String getFiscalPeriod();
        LocalDate getStartDate();
        LocalDate getEndDate();
        String getClosingStatus();
    }

    String ADMISSION_SELECT = """
            SELECT p.id AS id, p.fiscal_year AS fiscalYear, p.fiscal_period AS fiscalPeriod,
                   p.start_date AS startDate, p.end_date AS endDate,
                   CASE WHEN EXISTS (
                       SELECT 1 FROM fiscal_period_transitions t
                       WHERE t.fiscal_period_id = p.id
                         AND ((t.phase = 'PREPARED' AND t.target_status = 'CLOSED')
                              OR (t.phase = 'APPLIED' AND t.target_status = 'OPEN'))
                   ) THEN 'CLOSED' ELSE p.closing_status END AS closingStatus
            FROM fiscal_periods p
            """;

    @Query(value = ADMISSION_SELECT + "WHERE p.id = :id", nativeQuery = true)
    Optional<AdmissionView> findAdmissionById(@Param("id") Long id);

    @Query(value = ADMISSION_SELECT + "WHERE p.fiscal_year = :year AND p.fiscal_period = :period",
            nativeQuery = true)
    Optional<AdmissionView> findAdmissionByPeriodKey(@Param("year") String year,
            @Param("period") String period);
    boolean existsByFiscalPeriodId(Long fiscalPeriodId);
    Optional<FiscalPeriodTransitionEntity> findFirstByFiscalPeriodIdAndPhase(
            Long fiscalPeriodId, FiscalPeriodTransitionEntity.Phase phase);
    Optional<FiscalPeriodTransitionEntity> findFirstByFiscalPeriodIdAndPhaseAndTargetStatus(
            Long fiscalPeriodId, FiscalPeriodTransitionEntity.Phase phase, String targetStatus);

    @Query("select t.fiscalPeriodId from FiscalPeriodTransitionEntity t where t.operationId = :operationId")
    Optional<Long> findPeriodIdByOperationId(@Param("operationId") String operationId);

}
