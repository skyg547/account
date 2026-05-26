package com.risk.mart.core.application.port.out;

import com.risk.mart.core.domain.ods.loan.OdsEarlyWarning;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

/**
 * [Outbound Port] 조기경보 데이터 접근 인터페이스
 */
public interface OdsEarlyWarningRepository {
    Optional<OdsEarlyWarning> findTopByCustomerCodeAndBaseDateOrderByBaseDateDesc(String customerCode, LocalDate baseDate);
    List<OdsEarlyWarning> findAll();
    OdsEarlyWarning save(OdsEarlyWarning warning);
    void saveAll(Iterable<OdsEarlyWarning> warnings);
}
