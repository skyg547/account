package com.risk.credit.core.application.port.out;

import com.risk.credit.core.domain.exposure.CrCustomer;
import java.util.Optional;

/**
 * [Port] 신용 리스크용 고객(Customer) 데이터소스 인터페이스.
 */
public interface CrCustomerRepository {
    Optional<CrCustomer> findByCustomerCode(String customerCode);
    CrCustomer save(CrCustomer customer);
    Optional<CrCustomer> findById(Long id);
}
