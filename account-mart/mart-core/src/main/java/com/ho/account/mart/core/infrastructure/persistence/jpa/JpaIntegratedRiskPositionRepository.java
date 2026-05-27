package com.ho.account.mart.core.infrastructure.persistence.jpa;

import com.ho.account.shared.finance.entity.IntegratedRiskPosition;
import com.ho.account.shared.finance.entity.IntegratedRiskPositionId;
import com.ho.account.mart.core.application.port.out.IntegratedRiskPositionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * [Infrastructure] JPA를 사용한 통합 리스크 포지션 데이터 접근 인터페이스.
 */
public interface JpaIntegratedRiskPositionRepository extends JpaRepository<IntegratedRiskPosition, IntegratedRiskPositionId> {

    void deleteByBaseDt(LocalDate baseDt);

    Page<IntegratedRiskPosition> findByBaseDt(LocalDate baseDt, Pageable pageable);

    List<IntegratedRiskPosition> findByBaseDt(LocalDate baseDt);

    @Query("SELECT p.productCode as productCode, " +
            "       p.currency as currencyCode, " +
            "       SUM(p.currentBalance) as balanceAmount " +
            "FROM IntegratedRiskPosition p " +
            "WHERE p.baseDt = :baseDt " +
            "GROUP BY p.productCode, p.currency")
    List<IntegratedRiskPositionRepository.ProductCurrencyBalanceSummary> getBalanceSummaryByBaseDate(@Param("baseDt") LocalDate baseDt);

    @Query("SELECT p.staging as staging, SUM(p.outstandingAmount) as value " +
            "FROM IntegratedRiskPosition p " +
            "WHERE p.baseDt = :baseDt " +
            "GROUP BY p.staging")
    List<IntegratedRiskPositionRepository.StagingDistribution> getStagingDistribution(@Param("baseDt") LocalDate baseDt);

    @Query("SELECT p.industryCode as sector, SUM(p.expectedLoss) as value " +
            "FROM IntegratedRiskPosition p " +
            "WHERE p.baseDt = :baseDt " +
            "GROUP BY p.industryCode")
    List<IntegratedRiskPositionRepository.SectorDistribution> getSectorDistribution(@Param("baseDt") LocalDate baseDt);

    @Query("SELECT COALESCE(SUM(p.expectedLoss), 0) as totalEcl, " +
            "       COALESCE(SUM(p.outstandingAmount), 0) as totalExposure, " +
            "       COALESCE(SUM(p.rwaIrb), 0) as totalRwa, " +
            "       COUNT(p) as totalCount " +
            "FROM IntegratedRiskPosition p " +
            "WHERE p.baseDt = :baseDt")
    Map<String, Object> getSummaryMetrics(@Param("baseDt") LocalDate baseDt);

    @Query("SELECT p.internalRating as rating, " +
            "       SUM(p.rwaIrb) as rwa, " +
            "       SUM(p.outstandingAmount) as exposure " +
            "FROM IntegratedRiskPosition p " +
            "WHERE p.baseDt = :baseDt " +
            "GROUP BY p.internalRating " +
            "ORDER BY p.internalRating")
    List<IntegratedRiskPositionRepository.RatingDistribution> getRatingDistribution(@Param("baseDt") LocalDate baseDt);
}
