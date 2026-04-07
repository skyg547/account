package com.ho.account.report.repository;

import com.ho.account.report.domain.ReportSnapshotDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportSnapshotDetailRepository extends JpaRepository<ReportSnapshotDetail, Long> {
    List<ReportSnapshotDetail> findByHeader_SnapshotId(Long snapshotId);
}
