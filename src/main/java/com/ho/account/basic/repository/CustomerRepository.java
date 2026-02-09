package com.accounting.system.basic.repository;

import com.accounting.system.basic.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCustomerCode(String customerCode);
    List<Customer> findByUseYnTrue();
    boolean existsByCustomerCode(String customerCode);
    List<Customer> findByCustomerNameContaining(String name);
}
