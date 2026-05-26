package com.risk.credit.core.application.port.out;

import com.risk.credit.core.domain.model.CrSaRwMaster;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [Port] SA 위험가중치 마스터 데이터 접근 인터페이스.
 */
public interface CrSaRwMasterRepository extends JpaRepository<CrSaRwMaster, Long> {
}
