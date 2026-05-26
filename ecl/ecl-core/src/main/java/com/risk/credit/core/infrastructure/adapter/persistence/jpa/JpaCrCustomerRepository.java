package com.risk.credit.core.infrastructure.adapter.persistence.jpa;

import com.risk.credit.core.domain.exposure.CrCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * [Infrastructure] Spring Data JPA를 사용한 고객 데이터 접근 인터페이스.
 */
public interface JpaCrCustomerRepository extends JpaRepository<CrCustomer, Long> {
    Optional<CrCustomer> findByCustomerCode(String customerCode);
}
