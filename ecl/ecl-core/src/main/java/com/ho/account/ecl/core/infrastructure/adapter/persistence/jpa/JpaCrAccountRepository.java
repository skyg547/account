package com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa;

import com.ho.account.ecl.core.domain.exposure.CrAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * [Infrastructure] Spring Data JPA를 사용한 실제 DB 접근 인터페이스.
 */
public interface JpaCrAccountRepository extends JpaRepository<CrAccount, Long> {
    Optional<CrAccount> findByAccountNo(String accountNo);

    List<CrAccount> findByIsActiveTrue();

    List<CrAccount> findByCustomer_IdAndIsActiveTrue(Long customerId);
}
