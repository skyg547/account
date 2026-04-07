package com.ho.account.report.repository;

import com.ho.account.report.domain.ReportSnapshotHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportSnapshotHeaderRepository extends JpaRepository<ReportSnapshotHeader, Long> {
    List<ReportSnapshotHeader> findByReportTypeAndBaseDate(String reportType, LocalDate baseDate);

    Optional<ReportSnapshotHeader> findByReportTypeAndBaseDateAndVersion(String reportType, LocalDate baseDate,
            Integer version);
}
