package com.ho.account.ecl.core.infrastructure.adapter.persistence;

import com.ho.account.ecl.core.application.port.out.CrCustomerRepository;
import com.ho.account.ecl.core.domain.exposure.CrCustomer;
import com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa.JpaCrCustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * [Adapter] CrCustomerRepository 포트의 JPA 구현체.
 */
@Repository
@RequiredArgsConstructor
public class CrCustomerPersistenceAdapter implements CrCustomerRepository {

    private final JpaCrCustomerRepository jpaRepository;

    @Override
    public Optional<CrCustomer> findByCustomerCode(String customerCode) {
        return jpaRepository.findByCustomerCode(customerCode);
    }

    @Override
    public CrCustomer save(CrCustomer customer) {
        return jpaRepository.save(customer);
    }

    @Override
    public Optional<CrCustomer> findById(Long id) {
        return jpaRepository.findById(id);
    }
}
