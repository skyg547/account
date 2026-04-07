package com.ho.account.income.repository;

import com.ho.account.income.domain.ArPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArPaymentRepository extends JpaRepository<ArPayment, Long> {
    Optional<ArPayment> findByPaymentNo(String paymentNo);
}
