package com.risk.credit.core.application.port.out;

import com.risk.credit.core.domain.result.CrBatchAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * [Repository] 리스크 산출 배치 실행 이력(Audit) 저장소.
 */
@Repository
public interface CrBatchAuditRepository extends JpaRepository<CrBatchAudit, Long> {
    List<CrBatchAudit> findByBaseDate(LocalDate baseDate);
}
