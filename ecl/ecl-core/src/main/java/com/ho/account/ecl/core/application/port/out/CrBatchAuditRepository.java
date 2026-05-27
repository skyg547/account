package com.ho.account.ecl.core.application.port.out;

import com.ho.account.ecl.core.domain.result.CrBatchAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * [Repository] 대손충당금(IFRS9) 산출 배치 실행 이력(Audit) 저장소.
 */
@Repository
public interface CrBatchAuditRepository extends JpaRepository<CrBatchAudit, Long> {
    List<CrBatchAudit> findByBaseDate(LocalDate baseDate);
}
