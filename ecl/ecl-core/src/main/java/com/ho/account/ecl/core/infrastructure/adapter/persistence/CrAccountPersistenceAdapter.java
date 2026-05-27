package com.ho.account.ecl.core.infrastructure.adapter.persistence;

import com.ho.account.ecl.core.application.port.out.CrAccountRepository;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa.JpaCrAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * [Adapter] CrAccountRepository 포트의 JPA 구현체.
 */
@Repository
@RequiredArgsConstructor
public class CrAccountPersistenceAdapter implements CrAccountRepository {

    private final JpaCrAccountRepository jpaRepository;

    @Override
    public Optional<CrAccount> findByAccountNo(String accountNo) {
        return jpaRepository.findByAccountNo(accountNo);
    }

    @Override
    public List<CrAccount> findByIsActiveTrue() {
        return jpaRepository.findByIsActiveTrue();
    }

    @Override
    public List<CrAccount> findByCustomer_IdAndIsActiveTrue(Long customerId) {
        return jpaRepository.findByCustomer_IdAndIsActiveTrue(customerId);
    }

    @Override
    public CrAccount save(CrAccount account) {
        return jpaRepository.save(account);
    }

    @Override
    public void saveAll(Iterable<CrAccount> accounts) {
        jpaRepository.saveAll(accounts);
    }

    @Override
    public void delete(CrAccount account) {
        jpaRepository.delete(account);
    }

    @Override
    public Optional<CrAccount> findById(Long id) {
        return jpaRepository.findById(id);
    }
}
