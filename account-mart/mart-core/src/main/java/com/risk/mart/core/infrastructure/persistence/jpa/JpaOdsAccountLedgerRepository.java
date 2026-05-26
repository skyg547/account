package com.risk.mart.core.infrastructure.persistence.jpa;

import com.risk.mart.core.infrastructure.persistence.entity.ods.OdsAccountLedgerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * [Infrastructure] JPA를 사용한 원천 원장 데이터 접근 인터페이스.
 */
@Repository
public interface JpaOdsAccountLedgerRepository extends JpaRepository<OdsAccountLedgerEntity, String> {
    
    /** 💡 [기술 팁] 배치에서 사용하는 고정 쿼리를 상수로 관리하여 캡슐화를 강화합니다. */
    String QUERY_FOR_CDM_LOAD = "SELECT a FROM OdsAccountLedgerEntity a WHERE a.isActive = true ORDER BY a.accountNo";
    String QUERY_FOR_DQ_CHECK = "SELECT a FROM OdsAccountLedgerEntity a ORDER BY a.accountNo";
}
