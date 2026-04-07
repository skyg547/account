package com.ho.account.report.repository;

import com.ho.account.report.domain.ReportLineMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReportLineMappingRepository extends JpaRepository<ReportLineMapping, Long> {

    List<ReportLineMapping> findByReportTypeOrderByDisplayOrderAsc(String reportType);

    List<ReportLineMapping> findByLineCode(String lineCode);

    @Query("SELECT r FROM ReportLineMapping r WHERE r.reportType = :reportType " +
            "AND r.validFromDate <= :asOfDate AND (r.validToDate IS NULL OR r.validToDate >= :asOfDate) " +
            "ORDER BY r.displayOrder ASC")
    List<ReportLineMapping> findActiveByReportTypeAsOfDate(
            @Param("reportType") String reportType,
            @Param("asOfDate") LocalDate asOfDate);

    List<ReportLineMapping> findByParentLineCode(String parentLineCode);
}
