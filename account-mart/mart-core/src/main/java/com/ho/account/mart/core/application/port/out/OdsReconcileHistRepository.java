package com.ho.account.mart.core.application.port.out;

import com.ho.account.mart.core.domain.ods.audit.OdsReconcileHist;
import java.time.LocalDate;
import java.util.List;

/**
 * [Outbound Port] 원장 대사 이력 데이터 접근 인터페이스
 */
public interface OdsReconcileHistRepository {
    List<OdsReconcileHist> findByBaseDate(LocalDate baseDate);
    List<OdsReconcileHist> findAll();
    OdsReconcileHist save(OdsReconcileHist reconcileHist);
    void saveAll(List<OdsReconcileHist> reconcileHists);
}
