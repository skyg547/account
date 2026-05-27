package com.ho.account.ecl.core.application.port.out;

import com.ho.account.ecl.core.domain.exposure.CrCustomer;
import java.util.Optional;

/**
 * [Port] 대손충당금(IFRS9)용 고객(Customer) 데이터소스 인터페이스.
 */
public interface CrCustomerRepository {
    Optional<CrCustomer> findByCustomerCode(String customerCode);
    CrCustomer save(CrCustomer customer);
    Optional<CrCustomer> findById(Long id);
}
