package com.ho.account.reconciliation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public interface ExternalReconStageRecordRepository extends JpaRepository<ExternalReconStageRecord, Long> {

    @Query("""
            SELECT COUNT(r) AS itemCount,
                   COALESCE(SUM(r.amount), 0) AS totalAmount
            FROM ExternalReconStageRecord r
            WHERE r.unitId = :unitId
              AND r.stageCode = :stageCode
              AND r.reconciliationDate = :reconciliationDate
              AND (:productCode IS NULL OR r.productCode = :productCode)
              AND (:currencyCode IS NULL OR r.currencyCode = :currencyCode)
              AND (:legalEntityCode IS NULL OR r.legalEntityCode = :legalEntityCode)
            """)
    SnapshotAggregateProjection summarize(
            @Param("unitId") String unitId,
            @Param("stageCode") String stageCode,
            @Param("reconciliationDate") LocalDate reconciliationDate,
            @Param("productCode") String productCode,
            @Param("currencyCode") String currencyCode,
            @Param("legalEntityCode") String legalEntityCode
    );

    @Query("""
            SELECT r FROM ExternalReconStageRecord r
            WHERE r.unitId = :unitId
              AND r.stageCode = :stageCode
              AND r.reconciliationDate = :reconciliationDate
              AND (:productCode IS NULL OR r.productCode = :productCode)
              AND (:currencyCode IS NULL OR r.currencyCode = :currencyCode)
              AND (:legalEntityCode IS NULL OR r.legalEntityCode = :legalEntityCode)
            """)
    java.util.List<ExternalReconStageRecord> findStageRecords(
            @Param("unitId") String unitId,
            @Param("stageCode") String stageCode,
            @Param("reconciliationDate") LocalDate reconciliationDate,
            @Param("productCode") String productCode,
            @Param("currencyCode") String currencyCode,
            @Param("legalEntityCode") String legalEntityCode
    );

    interface SnapshotAggregateProjection {
        Long getItemCount();

        BigDecimal getTotalAmount();
    }
}
