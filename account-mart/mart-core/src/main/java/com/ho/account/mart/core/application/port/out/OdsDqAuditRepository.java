package com.ho.account.mart.core.application.port.out;

import com.ho.account.mart.core.domain.ods.audit.OdsDqAudit;
import java.time.LocalDate;
import java.util.List;

/**
 * [Outbound Port] 데이터 품질 검사 결과 데이터 접근 인터페이스
 */
public interface OdsDqAuditRepository {
    List<OdsDqAudit> findByBaseDate(LocalDate baseDate);
    List<OdsDqAudit> findAll();
    OdsDqAudit save(OdsDqAudit audit);
    void saveAll(List<OdsDqAudit> audits);
}
