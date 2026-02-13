package com.ho.account.report.repository;

import com.ho.account.report.domain.RegulatorySubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegulatorySubmissionRepository extends JpaRepository<RegulatorySubmission, Long> {
    List<RegulatorySubmission> findByReportCode(String reportCode);

    List<RegulatorySubmission> findBySnapshot_SnapshotId(Long snapshotId);
}
