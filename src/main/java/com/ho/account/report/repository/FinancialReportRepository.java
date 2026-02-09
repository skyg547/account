package com.accounting.system.report.repository;

import com.accounting.system.report.domain.FinancialReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinancialReportRepository extends JpaRepository<FinancialReport, Long> {
}
