package com.ho.account.mart.core.application.port.out;

import com.ho.account.mart.core.domain.ods.loan.OdsAccountRate;
import java.util.List;
import java.util.Optional;

/**
 * [Outbound Port] 계좌별 적용 금리 데이터 접근 인터페이스
 */
public interface OdsAccountRateRepository {
    Optional<OdsAccountRate> findById(String accountNo);
    List<OdsAccountRate> findAll();
    OdsAccountRate save(OdsAccountRate rate);
    void saveAll(Iterable<OdsAccountRate> rates);
}
