package com.ho.account.deposit.infrastructure.adapter.out.persistence;

import com.ho.account.deposit.domain.DepositAccount;
import com.ho.account.deposit.domain.DepositStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * [헥사고날 아키텍처 - Spring Data JPA Repository]
 * 
 * 실제 DB 테이블(deposit_accounts)과 매핑되는 Spring Data JPA 인터페이스입니다.
 * 이 인터페이스는 도메인이나 애플리케이션 서비스에 직접 노출되지 않고, 오직 Adapter 내부에서만 사용됩니다.
 */
public interface SpringDataDepositAccountRepository extends JpaRepository<DepositAccount, Long> {
    Optional<DepositAccount> findByAccountNumber(String accountNumber);
    List<DepositAccount> findByStatusAndValidFromLessThanEqualAndValidToGreaterThanEqual(
            DepositStatus status,
            LocalDate validFrom,
            LocalDate validTo);
}
