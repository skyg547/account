package com.ho.account.mart.core.infrastructure.persistence.jpa;

import com.ho.account.shared.finance.entity.AllowanceInputPosition;
import com.ho.account.shared.finance.entity.AllowanceInputPositionId;
import com.ho.account.mart.core.application.port.out.AllowanceInputPositionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * [Infrastructure] JPA를 사용한 대손충당금 입력 포지션 데이터 접근 인터페이스.
 */
public interface JpaAllowanceInputPositionRepository extends JpaRepository<AllowanceInputPosition, AllowanceInputPositionId> {

    void deleteByBaseDt(LocalDate baseDt);

    Page<AllowanceInputPosition> findByBaseDt(LocalDate baseDt, Pageable pageable);

    List<AllowanceInputPosition> findByBaseDt(LocalDate baseDt);

    @Query("SELECT p.productCode as productCode, " +
            "       p.currency as currencyCode, " +
            "       SUM(p.currentBalance) as balanceAmount " +
            "FROM AllowanceInputPosition p " +
            "WHERE p.baseDt = :baseDt " +
            "GROUP BY p.productCode, p.currency")
    List<AllowanceInputPositionRepository.ProductCurrencyBalanceSummary> getBalanceSummaryByBaseDate(@Param("baseDt") LocalDate baseDt);

    @Query("SELECT p.staging as staging, SUM(p.outstandingAmount) as value " +
            "FROM AllowanceInputPosition p " +
            "WHERE p.baseDt = :baseDt " +
            "GROUP BY p.staging")
    List<AllowanceInputPositionRepository.StagingDistribution> getStagingDistribution(@Param("baseDt") LocalDate baseDt);

    @Query("SELECT p.industryCode as sector, SUM(p.expectedLoss) as value " +
            "FROM AllowanceInputPosition p " +
            "WHERE p.baseDt = :baseDt " +
            "GROUP BY p.industryCode")
    List<AllowanceInputPositionRepository.SectorDistribution> getSectorDistribution(@Param("baseDt") LocalDate baseDt);

    @Query("SELECT COALESCE(SUM(p.expectedLoss), 0) as totalEcl, " +
            "       COALESCE(SUM(p.outstandingAmount), 0) as totalExposure, " +
            "       COUNT(p) as totalCount " +
            "FROM AllowanceInputPosition p " +
            "WHERE p.baseDt = :baseDt")
    Map<String, Object> getSummaryMetrics(@Param("baseDt") LocalDate baseDt);
}

