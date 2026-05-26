package com.risk.mart.core.application.port.out;

import com.risk.mart.core.domain.ods.loan.OdsBehavioralHistory;
import java.util.List;

/**
 * [Outbound Port] 행동 모델 과거 이력 데이터 접근 인터페이스
 */
public interface OdsBehavioralHistoryRepository {
    List<OdsBehavioralHistory> findAll();
    OdsBehavioralHistory save(OdsBehavioralHistory history);
    void saveAll(Iterable<OdsBehavioralHistory> histories);
}
