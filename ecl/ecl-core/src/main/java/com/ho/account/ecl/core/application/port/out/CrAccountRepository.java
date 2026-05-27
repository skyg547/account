package com.ho.account.ecl.core.application.port.out;

import com.ho.account.ecl.core.domain.exposure.CrAccount;
import java.util.List;
import java.util.Optional;

/**
 * [Port] 대손충당금(IFRS9)용 계좌(Account) 데이터소스 인터페이스.
 * 
 * 💡 [헥사고날 아키텍처] 
 * 이 인터페이스는 특정 기술(JPA, JDBC 등)에 의존하지 않는 순수한 비즈니스 규약입니다.
 * 실제 구현은 Infrastructure 레이어의 Adapter에서 담당합니다.
 */
public interface CrAccountRepository {
    Optional<CrAccount> findByAccountNo(String accountNo);

    List<CrAccount> findByIsActiveTrue();

    List<CrAccount> findByCustomer_IdAndIsActiveTrue(Long customerId);

    CrAccount save(CrAccount account);

    void saveAll(Iterable<CrAccount> accounts);

    void delete(CrAccount account);

    Optional<CrAccount> findById(Long id);
}
